package com.example.prismfit.controllers

import com.example.prismfit.database.model.Activity
import com.example.prismfit.database.repository.ActivityRepository
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.bson.types.ObjectId
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import kotlin.math.roundToInt

@RestController
@RequestMapping("/activities")
class ActivityController(
    private val activityRepository: ActivityRepository
) {

    data class ActivityRequest(
        @field:NotBlank val type: String,
        val startTime: Instant,
        val endTime: Instant,
        val distanceMeters: Int,
        val durationSeconds: Long
    )

    data class ActivityResponse(
        val id: String,
        val type: String,
        val startTime: Instant,
        val endTime: Instant,
        val durationSeconds: Long,
        val distanceMeters: Int,
        val averageMetersPerHour: Int
    )

    @PostMapping
    fun save(@Valid @RequestBody body: ActivityRequest): ActivityResponse {
        val ownerId = SecurityContextHolder.getContext().authentication.principal as String
        val averageMetersPerHour = if (body.durationSeconds > 0) {
            ((body.distanceMeters.toDouble() / body.durationSeconds) * 3600).roundToInt()
        } else {
            0
        }
        val activity = activityRepository.save(
            Activity(
                type = body.type,
                startTime = body.startTime,
                endTime = body.endTime,
                durationSeconds = body.durationSeconds,
                distanceMeters = body.distanceMeters,
                averageMetersPerHour = averageMetersPerHour,
                ownerId = ObjectId(ownerId)
            )
        )
        return activity.toResponse()
    }

    @GetMapping
    fun getAllActivities(): List<ActivityResponse> {
        val ownerId = SecurityContextHolder.getContext().authentication.principal as String
        return activityRepository.findByOwnerId(ObjectId(ownerId))
            .sortedByDescending { it.endTime }
            .map { it.toResponse() }
    }

    private fun  Activity.toResponse(): ActivityResponse {
        return ActivityResponse(
            id = id.toHexString(),
            type = type,
            startTime = startTime,
            endTime =  endTime,
            durationSeconds = durationSeconds,
            distanceMeters = distanceMeters,
            averageMetersPerHour = averageMetersPerHour
        )
    }
}