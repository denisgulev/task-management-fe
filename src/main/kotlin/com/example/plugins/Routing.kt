package com.example.plugins

import com.example.models.*
import com.example.services.TaskResult
import com.example.services.TaskService
import com.example.services.UserService
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.client.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.routing.route
import io.ktor.server.sessions.*
import io.ktor.server.thymeleaf.*

fun Application.configureRouting(client: HttpClient) {
    val taskService = TaskService(
        client, environment.config.propertyOrNull("env.TASK_SERVICE_URL")?.getString()
            ?: System.getenv("TASK_SERVICE_URL") ?: "http://task-management:8080"
    )
    val userService =
        UserService(
            client, environment.config.propertyOrNull("env.USER_SERVICE_URL")?.getString()
                ?: System.getenv("USER_SERVICE_URL") ?: "http://user-management:8080"
        )

    routing {
        get("/") { call.respondRedirect("/tasks") }

        route("/tasks") {
            get { showAllTasks(call, taskService) }
            get("/new") { call.respond(ThymeleafContent("task-form", emptyMap())) }
            get("/{id}") { handleTaskDetails(call, taskService) }
            get("/byName/{name}") { handleTaskByName(call, taskService) }
            get("/byPriority/{priority}") { handleTasksByPriority(call, taskService) }
            get("/edit/{id}") { handleTaskEdit(call, taskService) }
            get("/delete/{id}") { handleTaskDelete(call, taskService) }
            post("/{id}") { handleTaskUpdate(call, taskService) }
            post { handleTaskCreation(call, taskService) }
        }


        get("/users") { displayAllUsers(call, userService) }
        get("/login") { call.respond(ThymeleafContent("login-form", emptyMap())) }
        get("/logout") { handleLogout(call) }
        post("/login") { handleLogin(call, userService) }
    }
}

suspend fun showAllTasks(call: ApplicationCall, taskService: TaskService) {
    val userSession = call.sessions.get<UserSession>()
    val tasks = if (userSession?.token != null) {
        taskService.fetchAllTasksByUser(userSession.token)
    } else {
        taskService.fetchAllTasks()
    }
    val tasksWithUrls = tasks.map { task ->
        task to generateTaskUrls(task.id)
    }
    call.respond(
        ThymeleafContent(
            "all-tasks", mapOf(
                "tasksWithUrls" to tasksWithUrls,
                "loggedIn" to (userSession != null),
                "username" to (userSession?.username ?: "")
            )
        )
    )
}

fun generateTaskUrls(taskId: String) = mapOf(
    "viewUrl" to "/tasks/$taskId",
    "editUrl" to "/tasks/edit/$taskId",
    "deleteUrl" to "/tasks/delete/$taskId"
)

suspend fun handleTaskDetails(call: ApplicationCall, taskService: TaskService) {
    val taskId = call.parameters["id"] ?: return call.respond(HttpStatusCode.BadRequest, "Missing ID")
    val userSession = call.sessions.get<UserSession>() ?: return call.respond(HttpStatusCode.Unauthorized)
    respondWithTaskResult(call, taskService.fetchTaskById(taskId, userSession.token), "task-details")
}

suspend fun handleTaskByName(call: ApplicationCall, taskService: TaskService) {
    val name = call.parameters["taskName"] ?: return call.respond(HttpStatusCode.BadRequest)
    taskService.fetchTaskByName(name)?.let {
        call.respond(ThymeleafContent("single-task", mapOf("task" to it)))
    } ?: call.respond(HttpStatusCode.NotFound)
}

suspend fun handleTasksByPriority(call: ApplicationCall, taskService: TaskService) {
    val priorityAsText = call.parameters["priority"] ?: return call.respond(HttpStatusCode.BadRequest)
    try {
        val priority = Priority.valueOf(priorityAsText)
        val tasks = taskService.fetchTasksByPriority(priority) ?: return call.respond(HttpStatusCode.BadRequest)
        if (tasks.isEmpty()) return call.respond(HttpStatusCode.NotFound)

        call.respond(ThymeleafContent("tasks-by-priority", mapOf("priority" to priority, "tasks" to tasks)))
    } catch (ex: IllegalArgumentException) {
        call.respond(HttpStatusCode.BadRequest)
    }
}

suspend fun handleTaskEdit(call: ApplicationCall, taskService: TaskService) {
    val taskId = call.parameters["id"] ?: return call.respond(HttpStatusCode.BadRequest, "Missing ID")
    val userSession = call.sessions.get<UserSession>() ?: return call.respond(HttpStatusCode.Unauthorized)
    respondWithTaskResult(call, taskService.fetchTaskById(taskId, userSession.token), "task-edit-form")
}

suspend fun handleTaskDelete(call: ApplicationCall, taskService: TaskService) {
    val taskId = call.parameters["id"] ?: return call.respond(HttpStatusCode.BadRequest, "Missing ID")
    val userSession = call.sessions.get<UserSession>() ?: return call.respond(HttpStatusCode.Unauthorized)

    when (val result = taskService.deleteTask(taskId, userSession.token)) {
        is TaskResult.Success -> call.respondRedirect("/tasks")
        else -> respondWithError(call, result, "Task deletion failed")
    }
}

suspend fun handleTaskUpdate(call: ApplicationCall, taskService: TaskService) {
    val taskId = call.parameters["id"] ?: return call.respond(HttpStatusCode.BadRequest, "Missing ID")
    val userSession = call.sessions.get<UserSession>() ?: return call.respond(HttpStatusCode.Unauthorized)
    val formParameters = call.receiveParameters()

    val taskToUpdate = TaskUpdate(
        id = taskId,
        name = formParameters["name"] ?: "",
        description = formParameters["description"] ?: "",
        priority = Priority.valueOf(formParameters["priority"] ?: Priority.Low.name)
    )

    if (taskService.updateTask(taskId, taskToUpdate, userSession.token)) {
        val updatedTask = taskService.fetchTaskById(taskId, userSession.token)
        call.respond(ThymeleafContent("task-edit-form", mapOf("task" to (updatedTask as TaskResult.Success).task!!)))
    } else {
        call.respond(ThymeleafContent("task-edit-form", mapOf("task" to taskToUpdate)))
    }
}

suspend fun handleTaskCreation(call: ApplicationCall, taskService: TaskService) {
    val formParameters = call.receiveParameters()
    val userSession = call.sessions.get<UserSession>() ?: return call.respond(HttpStatusCode.Unauthorized)

    val newTask = TaskRequest(
        name = formParameters["name"] ?: "",
        description = formParameters["description"] ?: "",
        priority = formParameters["priority"] ?: Priority.Low.name
    )

    val result = taskService.createTask(newTask, userSession.token)
    if (result !is TaskResult.Success) {
        respondWithError(call, result, "Task creation failed")
        return
    }

    showAllTasks(call, taskService)
}

suspend fun displayAllUsers(call: ApplicationCall, userService: UserService) {
    val users = userService.fetchAllUsers()
    if (users != null) {
        println("Users: $users")
        call.respondText("Users displayed in console")
    } else {
        call.respondText("Failed to fetch users")
    }
}

suspend fun handleLogout(call: ApplicationCall) {
    call.sessions.clear<UserSession>()
    call.respondRedirect("/tasks")
}

suspend fun handleLogin(call: ApplicationCall, userService: UserService) {
    val parameters = call.receiveParameters()
    val username = parameters["username"] ?: return call.respond(HttpStatusCode.BadRequest, "Username is required")
    val password = parameters["password"] ?: return call.respond(HttpStatusCode.BadRequest, "Password is required")

    userService.login(username, password)?.let {
        call.sessions.set(UserSession(token = it.token, username = it.user.username))
        call.respondRedirect("/tasks")
    } ?: call.respond(ThymeleafContent("login-form", mapOf("error" to "Invalid credentials")))
}

suspend fun respondWithTaskResult(call: ApplicationCall, result: TaskResult, template: String) {
    when (result) {
        is TaskResult.Success -> call.respond(ThymeleafContent(template, mapOf("task" to result.task!!)))
        else -> respondWithError(call, result)
    }
}

suspend fun respondWithError(
    call: ApplicationCall,
    result: TaskResult,
    defaultErrorMessage: String = "An error occurred"
) {
    val errorMessage = when (result) {
        is TaskResult.NotFound -> result.message
        is TaskResult.Unauthorized -> result.message
        is TaskResult.Error -> defaultErrorMessage
        else -> defaultErrorMessage
    }
    call.respond(ThymeleafContent("error", mapOf("errorMessage" to errorMessage)))
}