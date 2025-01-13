package com.example.routes

import com.example.firebase.FirebaseStorageService
import com.example.model.*
import com.example.repository.ChatRepository
import com.example.repository.ProfileRepository
import com.example.repository.TaskRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File

fun Application.configureRouting(
    chatRepository: ChatRepository,
    profileRepository: ProfileRepository,
    taskRepository: TaskRepository
) {
    routing {
        rootRoutes()
        profileRoutes(profileRepository)
        chatRoutes(chatRepository)
        taskRoutes(taskRepository)
        statusRoutes(profileRepository)
        deviceTokenRoutes(profileRepository)
    }
}

private fun Route.rootRoutes() {
    get("/") {
        call.respondText("Welcome to the Ktor application!", ContentType.Text.Plain)
    }
}

private fun Route.profileRoutes(profileRepository: ProfileRepository) {
    route("/profile") {
        // Get profile by ID token
        get {
            val userId = call.validateAndExtractUserId() ?: return@get
            val profile = profileRepository.getProfile(userId)
            if (profile != null) {
                call.respond(profile)
            } else {
                call.respond(HttpStatusCode.NotFound, "Profile not found")
            }
        }

        // Create a new profile
        post {
            val userId = call.validateAndExtractUserId() ?: return@post
            val profile = call.receive<Profile>()
            if (profile.userId != userId) {
                call.respond(HttpStatusCode.BadRequest, "User ID mismatch")
                return@post
            }
            profileRepository.createProfile(profile)
            call.respond(HttpStatusCode.Created, "Profile created")
        }

        // Update an existing profile
        put {
            val userId = call.validateAndExtractUserId() ?: return@put
            val updatedProfile = call.receive<Profile>()
            if (updatedProfile.userId != userId) {
                call.respond(HttpStatusCode.BadRequest, "User ID mismatch")
                return@put
            }
            profileRepository.updateProfile(updatedProfile)
            call.respond(HttpStatusCode.OK, "Profile updated")
        }

        // Delete a profile
        delete {
            val userId = call.validateAndExtractUserId() ?: return@delete
            profileRepository.deleteProfile(userId)
            call.respond(HttpStatusCode.OK, "Profile deleted")
        }

        // Check if a profile exists, create if not
        get("/check") {
            val userId = call.validateAndExtractUserId() ?: return@get
            val profile = profileRepository.getProfile(userId)

            if (profile != null) {
                call.respond(profile)
            } else {
                val newProfile = Profile(
                    userId = userId,
                    name = "Unknown",
                    email = "No email",
                    personalNumber = null,
                    workNumber = null,
                    profilePictureUrl = null,
                    userRule = null,
                    createdAt = System.currentTimeMillis()
                )
                profileRepository.createProfile(newProfile)
                call.respond(HttpStatusCode.Created, newProfile)
            }
        }

        // Get user online status
        get("/{userId}/status") {
            val userId = call.parameters["userId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "User ID required")
            val (isOnline, lastSeen) = profileRepository.getUserStatus(userId)
            call.respond(HttpStatusCode.OK, mapOf("isOnline" to isOnline, "lastSeen" to lastSeen))
        }

        // Handle profile update decisions
        put("/{profileId}/decision") {
            val profileId = call.parameters["profileId"]
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Profile ID required")
            val decision = call.request.queryParameters["decision"]
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Decision required")

            val modifiedProfile = if (decision == "MODIFY") call.receive<Profile>() else null
            profileRepository.handleProfileUpdateDecision(profileId, decision, modifiedProfile)
            call.respond(HttpStatusCode.OK, "Profile update decision applied")
        }
    }
}

private fun Route.chatRoutes(chatRepository: ChatRepository) {
    route("/chat") {
        post("/private") {
            val request = call.receive<CreateChatRequest>()
            if (request.participants.size < 2) {
                call.respond(HttpStatusCode.BadRequest, "At least two participants are required.")
                return@post
            }
            val chat = chatRepository.createPrivateChat(request.participants[0], request.participants[1])
            call.respond(HttpStatusCode.Created, chat)
        }

        post("/group") {
            val request = call.receive<CreateGroupChatRequest>()
            val chat = chatRepository.createGroupChat(request.adminId, request.groupName, request.participants)
            call.respond(HttpStatusCode.Created, chat)
        }

        // Get all users
        get("/all") {
            val users = chatRepository.getAllUsers()
            call.respond(HttpStatusCode.OK, users)
        }

        post("/{chatId}/message") {
            val chatId = call.parameters["chatId"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Chat ID required")
            val message = call.receive<Message>()

            if (message.contentType == "file") {
                val file = File(message.content)
                val fileUrl = FirebaseStorageService.uploadFile(file, "application/octet-stream")
                if (fileUrl == null) {
                    call.respond(HttpStatusCode.InternalServerError, "Failed to upload file")
                    return@post
                }
                val updatedMessage = message.copy(content = fileUrl)
                val messageId = chatRepository.storeMessage(chatId, updatedMessage.senderId, updatedMessage)
                chatRepository.notifyParticipants(chatId, updatedMessage, updatedMessage.senderId)
                call.respond(HttpStatusCode.Created, mapOf("messageId" to messageId))
            } else {
                val messageId = chatRepository.storeMessage(chatId, message.senderId, message)
                chatRepository.notifyParticipants(chatId, message, message.senderId)
                call.respond(HttpStatusCode.Created, mapOf("messageId" to messageId))
            }
        }

        get {
            val userId = call.request.queryParameters["userId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "User ID required")
            val chats = chatRepository.getActiveChats(userId)
            call.respond(HttpStatusCode.OK, chats)
        }

        get("/details") {
            val userId = call.request.queryParameters["userId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "User ID required")
            val chatDetails = chatRepository.getChatsWithDetails(userId)
            call.respond(HttpStatusCode.OK, chatDetails)
        }
    }
}

private fun Route.taskRoutes(taskRepository: TaskRepository) {
    route("/tasks") {
        get {
            val tasks = taskRepository.getAllTasks()
            call.respond(HttpStatusCode.OK, tasks)
        }

        post {
            val task = call.receive<Task>()
            val taskId = taskRepository.createTask(task)
            call.respond(HttpStatusCode.Created, mapOf("taskId" to taskId))
        }

        get("/{taskId}") {
            val taskId = call.parameters["taskId"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid task ID")
            val task = taskRepository.getTask(taskId)
            if (task == null) {
                call.respond(HttpStatusCode.NotFound, "Task not found")
            } else {
                call.respond(HttpStatusCode.OK, task)
            }
        }

        put("/{taskId}") {
            val taskId = call.parameters["taskId"]?.toIntOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid task ID")
            val updatedTask = call.receive<Task>()
            val success = taskRepository.updateTask(taskId, updatedTask)
            if (success) {
                call.respond(HttpStatusCode.OK, "Task updated successfully")
            } else {
                call.respond(HttpStatusCode.NotFound, "Task not found")
            }
        }

        delete("/{taskId}") {
            val taskId = call.parameters["taskId"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid task ID")
            val success = taskRepository.deleteTask(taskId)
            if (success) {
                call.respond(HttpStatusCode.OK, "Task deleted successfully")
            } else {
                call.respond(HttpStatusCode.NotFound, "Task not found")
            }
        }
    }
}

private fun Route.statusRoutes(profileRepository: ProfileRepository) {
    route("/status") {
        post {
            val userId = call.parameters["userId"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, "User ID required")
            val online = call.parameters["online"]?.toBoolean() ?: false
            profileRepository.updateAndBroadcastStatus(userId, online)
            call.respond(HttpStatusCode.OK, "Status updated")
        }
    }
}

private fun Route.deviceTokenRoutes(profileRepository: ProfileRepository) {
    post("/device-token") {
        val userId = call.parameters["userId"]
        val token = call.parameters["token"]
        if (userId == null || token == null) {
            call.respond(HttpStatusCode.BadRequest, "User ID and token required")
            return@post
        }
        profileRepository.saveDeviceToken(userId, token)
        call.respond(HttpStatusCode.OK, "Device token saved")
    }
}

