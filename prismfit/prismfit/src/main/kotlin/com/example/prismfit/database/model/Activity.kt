package com.example.prismfit.database.model

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("activities")
data class Activity(
    val type: String,
    val startTime: Instant,
    val endTime: Instant,
    val durationSeconds: Long,
    val distanceMeters: Int,
    val averageMetersPerHour: Int,
    val ownerId: ObjectId,
    @Id val id: ObjectId = ObjectId.get()
)