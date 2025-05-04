package com.example.prismfit.database.repository

import com.example.prismfit.database.model.Activity
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface ActivityRepository : MongoRepository<Activity, ObjectId> {
    fun findByOwnerId(ownerId: ObjectId): List<Activity>
}