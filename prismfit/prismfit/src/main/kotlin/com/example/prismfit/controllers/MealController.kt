package com.example.prismfit.controllers

import com.example.prismfit.database.model.Dish
import com.example.prismfit.database.model.Meal
import com.example.prismfit.database.repository.MealRepository
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import org.bson.types.ObjectId
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/meals")
class MealController(
    private val mealRepository: MealRepository
) {

    data class DishRequest(
        @field:NotBlank(message = "Name of the dish cannot be blank")
        val name: String,
        @field:Min(value = 1, message = "Weight must be at least 1g")
        val weight: Double,
        @field:Min(value = 0, message = "Calories must be non-negative")
        val caloriesPer100: Double,
        @field:Min(value = 0, message = "Protein must be non-negative")
        val proteinPer100: Double,
        @field:Min(value = 0, message = "Fat must be non-negative")
        val fatPer100: Double,
        @field:Min(value = 0, message = "Carbs must be non-negative")
        val carbsPer100: Double
    )

    data class MealRequest(
        val id: String?,
        @field:NotBlank(message = "Meal type cannot be blank")
        @field:Size(max = 20, message = "Meal type cannot be longer than 20 characters")
        val type: String,
        @field:NotEmpty(message = "At least one dish must be added")
        val dishes: List<DishRequest>,
        val date: LocalDate? = null
    )

    data class MealResponse(
        val id: String,
        val type: String,
        val date: String,
        val dishes: List<DishRequest>
    )

    @PostMapping
    fun save(@Valid @RequestBody body: MealRequest): MealResponse {
        val ownerId = SecurityContextHolder.getContext().authentication.principal as String
        val meal = mealRepository.save(
            Meal(
                id = body.id?.let { ObjectId(it) } ?: ObjectId.get(),
                type = body.type.take(20),
                date = body.date ?: LocalDate.now(),
                dishes = body.dishes.map {
                    Dish(
                        name = it.name,
                        weight = it.weight,
                        caloriesPer100 = it.caloriesPer100,
                        proteinPer100 = it.proteinPer100,
                        fatPer100 = it.fatPer100,
                        carbsPer100 = it.carbsPer100
                    )
                },
                ownerId = ObjectId(ownerId)
            )
        )
        return meal.toResponse()
    }

    @GetMapping
    fun getTodayMeals(): List<MealResponse> {
        val ownerId = SecurityContextHolder.getContext().authentication.principal as String
//        val meals = mealRepository.findByOwnerIdAndDate(ObjectId(ownerId), System.currentTimeMillis())

        val meals = mealRepository.findByOwnerIdAndDate(ObjectId(ownerId), LocalDate.now())
        return meals.map { it.toResponse() }
    }

    @GetMapping("/all")
    fun getAllMeals(): List<MealResponse> {
        val ownerId = SecurityContextHolder.getContext().authentication.principal as String
        val meals = mealRepository.findByOwnerId(ObjectId(ownerId))
        return meals.map { it.toResponse() }
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: String) {
        val meal = mealRepository.findById(ObjectId(id)).orElseThrow {
            IllegalArgumentException("Meal not found")
        }
        val ownerId = SecurityContextHolder.getContext().authentication.principal
        if (meal.ownerId.toHexString() == ownerId) {
            mealRepository.deleteById(meal.id)
        }
    }

    private fun Meal.toResponse(): MealResponse {
        return MealResponse(
            id = id.toHexString(),
            type = type,
            date = date.toString(),
            dishes = dishes.map {
                DishRequest(
                    name = it.name,
                    weight = it.weight,
                    caloriesPer100 = it.caloriesPer100,
                    proteinPer100 = it.proteinPer100,
                    fatPer100 = it.fatPer100,
                    carbsPer100 = it.carbsPer100
                )
            }
        )
    }
}