package com.example.prismfit.database.repository

import com.example.prismfit.database.model.Meal
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import java.time.LocalDate
import java.time.LocalDateTime

interface MealRepository: MongoRepository<Meal, ObjectId> {
    fun findByOwnerId(ownerId: ObjectId): List<Meal>
    fun findByOwnerIdAndDate(ownerId: ObjectId, date: LocalDate): List<Meal>
}