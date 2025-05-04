package com.example.prismfit.security

import com.example.prismfit.database.model.RefreshToken
import com.example.prismfit.database.model.User
import com.example.prismfit.database.repository.RefreshTokenRepository
import com.example.prismfit.database.repository.UserRepository
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.util.*

@Service
class AuthService(
    private val jwtService: JwtService,
    private val userRepository: UserRepository,
    private val hashEncoder: HashEncoder,
    private val refreshTokenRepository: RefreshTokenRepository
) {
    data class TokenPair(
        val accessToken: String,
        val refreshToken: String
    )

//    fun register(email: String, password: String, nickname: String, dateOfBirth: Long): User {
//        val user = userRepository.findByEmail(email.trim())
//        if(user != null) {
//            throw ResponseStatusException(HttpStatus.CONFLICT, "A user with that email already exists.")
//        }
//
//        val birthDate = Instant.ofEpochMilli(dateOfBirth)
//            .atZone(ZoneId.systemDefault())
//            .toLocalDate()
//        val age = Period.between(birthDate, LocalDate.now()).years
//        if (age < 12) {
//            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "User must be at least 12 years old.")
//        }
//
//        return userRepository.save(
//            User(
//                email = email,
//                hashedPassword = hashEncoder.encode(password),
//                nickname = nickname,
//                dateOfBirth = dateOfBirth
//            )
//        )
//    }
    fun register(email: String, password: String, nickname: String, dateOfBirth: Long): TokenPair {
        val user = userRepository.findByEmail(email.trim())
        if (user != null) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "A user with that email already exists.")
        }

        val birthDate = Instant.ofEpochMilli(dateOfBirth)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        val age = Period.between(birthDate, LocalDate.now()).years
        if (age < 12) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "User must be at least 12 years old.")
        }

        val newUser = userRepository.save(
            User(
                email = email,
                hashedPassword = hashEncoder.encode(password),
                nickname = nickname,
                dateOfBirth = dateOfBirth
            )
        )

        val newAccessToken = jwtService.generateAccessToken(newUser.id.toHexString())
        val newRefreshToken = jwtService.generateRefreshToken(newUser.id.toHexString())

        storeRefreshToken(newUser.id, newRefreshToken)

        return TokenPair(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken
        )
    }

    fun login(email: String, password: String): TokenPair {
        val user = userRepository.findByEmail(email)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "A user with that email doesn't exists.")

//        if(!hashEncoder.matches(password, user.hashedPassword)) {
//            throw BadCredentialsException("Incorrect password.")
//        }
        if (!hashEncoder.matches(password, user.hashedPassword)) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Incorrect password.")
        }

        val newAccessToken = jwtService.generateAccessToken(user.id.toHexString())
        val newRefreshToken = jwtService.generateRefreshToken(user.id.toHexString())

        storeRefreshToken(user.id, newRefreshToken)

        return TokenPair(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken
        )
    }

//    fun login(email: String, password: String): TokenPair {
//        val user = userRepository.findByEmail(email)
//            ?: throw BadCredentialsException("Invalid credentials.")
//
//        if(!hashEncoder.matches(password, user.hashedPassword)) {
//            throw BadCredentialsException("Invalid credentials.")
//        }
//
//        val newAccessToken = jwtService.generateAccessToken(user.id.toHexString())
//        val newRefreshToken = jwtService.generateRefreshToken(user.id.toHexString())
//
//        storeRefreshToken(user.id, newRefreshToken)
//
//        return TokenPair(
//            accessToken = newAccessToken,
//            refreshToken = newRefreshToken
//        )
//    }

    @Transactional
    fun refresh(refreshToken: String): TokenPair {
        if(!jwtService.validateRefreshToken(refreshToken)) {
            throw ResponseStatusException(HttpStatusCode.valueOf(401), "Invalid refresh token.")
        }

        val userId = jwtService.getUserIdFromToken(refreshToken)
        val user = userRepository.findById(ObjectId(userId)).orElseThrow {
            ResponseStatusException(HttpStatusCode.valueOf(401), "Invalid refresh token.")
        }

        val hashed = hashToken(refreshToken)
        refreshTokenRepository.findByUserIdAndHashedToken(user.id, hashed)
            ?: throw ResponseStatusException(
                HttpStatusCode.valueOf(401),
                "Refresh token not recognized (maybe used or expired?)"
            )

        refreshTokenRepository.deleteByUserIdAndHashedToken(user.id, hashed)

        val newAccessToken = jwtService.generateAccessToken(userId)
        val newRefreshToken = jwtService.generateRefreshToken(userId)

        storeRefreshToken(user.id, newRefreshToken)

        return TokenPair(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken
        )
    }

    private fun storeRefreshToken(userId: ObjectId, rawRefreshToken: String) {
        val hashed = hashToken(rawRefreshToken)
        val expiryMs = jwtService.refreshTokenValidityMs
        val expiresAt = Instant.now().plusMillis(expiryMs)

        refreshTokenRepository.save(
            RefreshToken(
                userId = userId,
                expiresAt = expiresAt,
                hashedToken = hashed
            )
        )
    }

    private fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(token.encodeToByteArray())
        return Base64.getEncoder().encodeToString(hashBytes)
    }
}