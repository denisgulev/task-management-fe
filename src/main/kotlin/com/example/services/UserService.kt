package com.example.services

import com.example.models.User
import com.example.models.UserLogin
import com.example.models.UserWithTokenDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class UserService(private val client: HttpClient, private val baseUrl: String) {
    suspend fun fetchAllUsers(): List<User>? {
        return try {
            val response: HttpResponse = client.get("$baseUrl/users")
            println("*** BODY AS TEXT ${response.bodyAsText()}")
            if (response.status.value == 200) {
                response.body()
            } else {
                println("Failed to fetch users. Status: ${response.status}")
                null
            }
        } catch (e: Exception) {
            println("Error occurred while fetching users: ${e.message}")
            null
        }
    }

    suspend fun login(username: String, password: String): UserWithTokenDto? {
        return try {
            val response: HttpResponse = client.post("$baseUrl/users/login") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(UserLogin(username = username, password = password)))
            }
            if (response.status == HttpStatusCode.OK) {
                Json.decodeFromString<UserWithTokenDto>(response.bodyAsText())
            } else {
                null
            }
        } catch (e: Exception) {
            println("Error during login: ${e.message}")
            null
        }
    }

    // Add other methods like fetchUserById, createUser, etc.
}