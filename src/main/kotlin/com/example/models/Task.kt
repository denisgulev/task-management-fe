package com.example.models

import kotlinx.serialization.Serializable

@Serializable
data class Task(
    val id: String,
    val name: String,
    val description: String,
    val priority: Priority
)

@Serializable
data class TaskResponse(
    val id: String,
    val name: String,
    val description: String,
    val priority: String,
    val userId: String
)

@Serializable
data class TaskRequest @JvmOverloads constructor(
    val name: String,
    val description: String,
    val priority: String,
    val userId: String? = null
)

@Serializable
data class TaskUpdate(
    val id: String,
    val name: String,
    val description: String,
    val priority: Priority
)

@Serializable
enum class Priority {
    Low, Medium, High, Vital
}