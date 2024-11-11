package com.example.services

import com.example.models.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

sealed class TaskResult {
    data class Success(val task: TaskResponse?) : TaskResult()
    data class NotFound(val message: String) : TaskResult()
    data class Unauthorized(val message: String) : TaskResult()
    data class Error(val message: String) : TaskResult()
}

class TaskService(private val client: HttpClient, private val baseUrl: String) {
    // Fetch all tasks
    suspend fun fetchAllTasks(): List<TaskResponse> {
        return try {
            val response: HttpResponse = client.get("$baseUrl/tasks") {
                accept(ContentType.Application.Json)
            }
            Json.decodeFromString<List<TaskResponse>>(response.bodyAsText())
        } catch (e: Exception) {
            println("Error occurred while fetching tasks: ${e.message}")
            emptyList()
        }
    }

    // Fetch all tasks
    suspend fun fetchAllTasksByUser(authToken: String): List<TaskResponse> {
        return try {
            val response: HttpResponse = client.get("$baseUrl/tasks") {
                headers {
                    set(HttpHeaders.Authorization, "Bearer $authToken")
                }
                accept(ContentType.Application.Json)
            }
            Json.decodeFromString<List<TaskResponse>>(response.bodyAsText())
        } catch (e: Exception) {
            println("Error occurred while fetching tasks: ${e.message}")
            emptyList()
        }
    }

    // Fetch a task by ID
    suspend fun fetchTaskById(taskId: String, authToken: String): TaskResult {
        return try {
            val response = client.get("$baseUrl/tasks/$taskId") {
                headers {
                    set(HttpHeaders.Authorization, "Bearer $authToken")
                }
                accept(ContentType.Application.Json)
            }

            when (response.status) {
                HttpStatusCode.OK -> {
                    // Success - decode the response body
                    TaskResult.Success(Json.decodeFromString<TaskResponse>(response.bodyAsText()))
                }

                HttpStatusCode.NotFound -> {
                    // Task not found
                    TaskResult.NotFound("Task with ID $taskId not found")
                }

                HttpStatusCode.Unauthorized -> {
                    // Unauthorized access
                    TaskResult.Unauthorized("Unauthorized access to task with ID $taskId")
                }

                HttpStatusCode.Forbidden -> {
                    // Forbidden access
                    TaskResult.Unauthorized("Forbidden access to task with ID $taskId")
                }

                HttpStatusCode.InternalServerError -> {
                    // Server error
                    TaskResult.Error("Internal server error while fetching task with ID $taskId")
                }

                else -> {
                    // Unexpected response status
                    TaskResult.Error("Unexpected response status: ${response.status}")
                }
            }
        } catch (e: Exception) {
            // Catch unexpected exceptions
            TaskResult.Error("Error fetching task by ID: ${e.message}")
        }
    }

    // Fetch a task by Name
    suspend fun fetchTaskByName(name: String): TaskResponse? {
        return try {
            client.get("$baseUrl/tasks/byName/$name") {
                accept(ContentType.Application.Json)
            }.body<TaskResponse>()
        } catch (e: Exception) {
            println("Error fetching task by Name: ${e.message}")
            null
        }
    }

    // Fetch a task by Name
    suspend fun fetchTasksByPriority(priority: Priority): List<TaskResponse>? {
        return try {
            client.get("$baseUrl/tasks/byPriority/$priority") {
                accept(ContentType.Application.Json)
            }.body<List<TaskResponse>>()
        } catch (e: Exception) {
            println("Error fetching task by Priority: ${e.message}")
            null
        }
    }

    suspend fun createTask(task: TaskRequest, authToken: String): TaskResult {
        return try {
            val response: HttpResponse = client.post("$baseUrl/tasks") {
                headers {
                    set(HttpHeaders.Authorization, "Bearer $authToken")
                }
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(task))
            }

            when (response.status) {
                HttpStatusCode.Created -> {
                    // Task created successfully
                    TaskResult.Success(null)
                }

                HttpStatusCode.BadRequest -> {
                    // Bad request - possibly validation error
                    TaskResult.Error("Bad request: ${response.bodyAsText()}")
                }

                HttpStatusCode.Unauthorized -> {
                    // Unauthorized access
                    TaskResult.Unauthorized("Unauthorized access to create a task")
                }

                HttpStatusCode.Forbidden -> {
                    // Forbidden access
                    TaskResult.Unauthorized("Forbidden access to create a task")
                }

                HttpStatusCode.InternalServerError -> {
                    // Server error
                    TaskResult.Error("Internal server error while creating task")
                }

                else -> {
                    // Unexpected response status
                    TaskResult.Error("Unexpected response status: ${response.status}")
                }
            }
        } catch (e: Exception) {
            // Catch unexpected exceptions
            TaskResult.Error("Error creating task: ${e.message}")
        }
    }


    // Update an existing task
    suspend fun updateTask(taskId: String, updatedTask: TaskUpdate, authToken: String): Boolean {
        return try {
            val response: HttpResponse = client.patch("$baseUrl/tasks/$taskId") {
                headers {
                    set(HttpHeaders.Authorization, "Bearer $authToken")
                }
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(updatedTask))
            }
            println("*** Update response: $response")
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            println("Error updating task: ${e.message}")
            false
        }
    }

    // Delete a task
    suspend fun deleteTask(taskId: String, token: String): TaskResult {
        return try {
//            val response: HttpResponse = client.delete("$baseUrl/tasks/$taskId")
//            response.status == HttpStatusCode.NoContent

            val response = client.delete("$baseUrl/tasks/$taskId") {
                headers {
                    set(HttpHeaders.Authorization, "Bearer $token")
                }
                accept(ContentType.Application.Json)
            }

            println("*** Response: $response")

            when (response.status) {
                HttpStatusCode.OK -> {
                    // Success - decode the response body
                    TaskResult.Success(null)
                }

                HttpStatusCode.NotFound -> {
                    // Task not found
                    TaskResult.NotFound("Task with ID $taskId not found")
                }

                HttpStatusCode.Unauthorized -> {
                    // Unauthorized access
                    TaskResult.Unauthorized("Unauthorized access to delete task with ID $taskId")
                }

                HttpStatusCode.Forbidden -> {
                    // Forbidden access
                    TaskResult.Unauthorized("Forbidden access to task with ID $taskId")
                }

                HttpStatusCode.InternalServerError -> {
                    // Server error
                    TaskResult.Error("Internal server error while fetching task with ID $taskId")
                }

                else -> {
                    // Unexpected response status
                    TaskResult.Error("Unexpected response status: ${response.status}")
                }
            }
        } catch (e: Exception) {
            // Catch unexpected exceptions
            TaskResult.Error("Error fetching task by ID: ${e.message}")
        }
    }
}