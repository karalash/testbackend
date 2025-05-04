package com.example.prismfit.controllers

import com.example.prismfit.security.AuthService
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService
) {
    data class AuthRequest(
        @field:Email(message = "Invalid email format.")
        val email: String,
        @field:Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{9,}\$",
            message = "Password must be at least 9 characters long and contain at least one digit, uppercase and lowercase letter."
        )
        val password: String,
        @field:Size(min = 3, max = 20, message = "Nickname must be 3-20 characters long.")
        val nickname: String,
        val dateOfBirth: Long
    )

    data class RefreshRequest(
        val refreshToken: String
    )

//    @PostMapping("/register")
//    fun register(
//        @Valid @RequestBody body: AuthRequest
//    ) {
//        authService.register(body.email, body.password, body.nickname, body.dateOfBirth)
//    }

    @PostMapping("/register")
    fun register(
        @Valid @RequestBody body: AuthRequest
    ): AuthService.TokenPair {
        return authService.register(body.email, body.password, body.nickname, body.dateOfBirth)
    }

    @PostMapping("/login")
    fun login(
        @RequestBody body: AuthRequest
    ): AuthService.TokenPair {
        return authService.login(body.email, body.password)
    }

    @PostMapping("/refresh")
    fun refresh(
        @RequestBody body: RefreshRequest
    ): AuthService.TokenPair {
        return authService.refresh(body.refreshToken)
    }
}