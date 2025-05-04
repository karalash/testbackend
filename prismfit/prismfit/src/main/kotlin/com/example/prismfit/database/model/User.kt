package com.example.prismfit.database.model

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document("users")
data class User(
    val email: String,
    val hashedPassword: String,
    val nickname: String,
    val dateOfBirth: Long,
    @Id val id: ObjectId = ObjectId()
)