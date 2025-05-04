package com.example.prismfit.database.model

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDate

@Document("meals")
data class Meal(
    val date: LocalDate,
    val type: String,
    val dishes: List<Dish>,
    val ownerId: ObjectId,
    @Id val id: ObjectId = ObjectId.get()
)