package com.example.models

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val name: String,
    val email: String
)

@Serializable
data class UserLogin(
    val username: String,
    val password: String
)

@Serializable
data class UserSession(val token: String, val username: String)

@Serializable
data class UserResponse(
    val id: String,
    val username: String,
    val email: String,
    val role: Role,
    val permission: Permission,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class UserWithTokenDto(
    val user: UserResponse,
    val token: String
)

@Serializable
enum class Role(val mongoRole: String) {
    ADMIN("admin"),
    USER("user")
}

@Serializable
enum class Permission(val permission: String) {
    ALL("all"),
    VIEW("view"),
    CREATE("create"),
    DELETE("delete")
}