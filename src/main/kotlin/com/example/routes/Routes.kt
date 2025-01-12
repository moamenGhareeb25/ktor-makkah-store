package com.example.routes

import com.example.auth.FirebaseAuth
import com.example.firebase.FirebaseStorageService
import com.example.model.CreateChatRequest
import com.example.model.CreateGroupChatRequest
import com.example.model.Message
import com.example.model.Profile
import com.example.repository.ChatRepository
import com.example.repository.ProfileRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File

fun Application.configureRouting(chatRepository: ChatRepository, profileRepository: ProfileRepository) {

    routing {
        // Root route
        get("/") {
            call.respondText("Welcome to the Ktor application!", ContentType.Text.Plain)
        }

        route("/main") {
            get("/users") {
                val users = profileRepository.getAllUsers()
                call.respond(HttpStatusCode.OK, users)
            }
        }

        // Profile routes
        route("/profile") {
            get {
                val idToken = call.request.headers["Authorization"]?.removePrefix("Bearer ")
                if (idToken != null) {
                    val decodedToken = FirebaseAuth.verifyIdToken(idToken)
                    if (decodedToken != null) {
                        val userId = decodedToken.uid
                        val profile = profileRepository.getProfile(userId)
                        if (profile != null) {
                            call.respond(profile)
                        } else {
                            call.respond(HttpStatusCode.NotFound, "Profile not found")
                        }
                    } else {
                        call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                    }
                } else {
                    call.respond(HttpStatusCode.Unauthorized, "Missing token")
                }
            }

            post {
                try {
                    val idToken = call.request.headers["Authorization"]?.removePrefix("Bearer ")
                    if (idToken != null) {
                        val decodedToken = FirebaseAuth.verifyIdToken(idToken)
                        if (decodedToken != null) {
                            val userId = decodedToken.uid
                            val profile = call.receive<Profile>()
                            if (profile.userId == userId) {
                                profileRepository.createProfile(profile)
                                call.respond(HttpStatusCode.Created, "Profile created")
                            } else {
                                call.respond(HttpStatusCode.BadRequest, "User ID mismatch")
                            }
                        } else {
                            call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                        }
                    } else {
                        call.respond(HttpStatusCode.Unauthorized, "Missing token")
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Internal server error: ${e.message}")
                }
            }

            put {
                val idToken = call.request.headers["Authorization"]?.removePrefix("Bearer ")
                if (idToken != null) {
                    val decodedToken = FirebaseAuth.verifyIdToken(idToken)
                    if (decodedToken != null) {
                        val userId = decodedToken.uid
                        val updatedProfile = call.receive<Profile>()
                        if (updatedProfile.userId == userId) {
                            profileRepository.updateProfile(updatedProfile)
                            call.respond(HttpStatusCode.OK, "Profile updated")
                        } else {
                            call.respond(HttpStatusCode.BadRequest, "User ID mismatch")
                        }
                    } else {
                        call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                    }
                } else {
                    call.respond(HttpStatusCode.Unauthorized, "Missing token")
                }
            }

            delete {
                val idToken = call.request.headers["Authorization"]?.removePrefix("Bearer ")
                if (idToken != null) {
                    val decodedToken = FirebaseAuth.verifyIdToken(idToken)
                    if (decodedToken != null) {
                        val userId = decodedToken.uid
                        profileRepository.deleteProfile(userId)
                        call.respond(HttpStatusCode.OK, "Profile deleted")
                    } else {
                        call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                    }
                } else {
                    call.respond(HttpStatusCode.Unauthorized, "Missing token")
                }
            }

            get("/check") {
                val idToken = call.request.headers["Authorization"]?.removePrefix("Bearer ")
                if (idToken != null) {
                    val decodedToken = FirebaseAuth.verifyIdToken(idToken)
                    if (decodedToken != null) {
                        val userId = decodedToken.uid
                        val profile = profileRepository.getProfile(userId)

                        if (profile != null) {
                            call.respond(profile)
                        } else {
                            val newProfile = Profile(
                                userId = userId,
                                name = decodedToken.name ?: "Unknown",
                                email = decodedToken.email ?: "No email",
                                personalNumber = null,
                                workNumber = null,
                                profilePictureUrl = null,
                                createdAt = System.currentTimeMillis()
                            )
                            profileRepository.createProfile(newProfile)
                            call.respond(HttpStatusCode.Created, newProfile)
                        }
                    } else {
                        call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                    }
                } else {
                    call.respond(HttpStatusCode.Unauthorized, "Missing token")
                }
            }

            get("/{userId}/status") {
                val userId = call.parameters["userId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "User ID required")
                val (isOnline, lastSeen) = profileRepository.getUserStatus(userId)
                call.respond(HttpStatusCode.OK, mapOf("isOnline" to isOnline, "lastSeen" to lastSeen))
            }
        }

        post("/private") {
            try {
                // Deserialize the incoming JSON into CreateChatRequest
                val request = call.receive<CreateChatRequest>()

                // Validate participants
                if (request.participants.size < 2) {
                    call.respond(HttpStatusCode.BadRequest, "At least two participants are required.")
                    return@post
                }
                // Create the private chat
                val chat = chatRepository.createPrivateChat(request.participants[0], request.participants[1])

                // Respond with the created chat
                call.respond(HttpStatusCode.Created, chat)
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.BadRequest, "Invalid request payload: ${e.message}")
            }
        }

            post("/group") {
                val request = call.receive<CreateGroupChatRequest>()
                val chat = chatRepository.createGroupChat(request.adminId, request.groupName, request.participants)
                call.respond(HttpStatusCode.Created, chat)
            }

            post("/{chatId}/message") {
                val chatId = call.parameters["chatId"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Chat ID required")
                val message = call.receive<Message>()

                if (message.contentType == "file") {
                    val file = File(message.content)
                    val fileUrl = FirebaseStorageService.uploadFile(file, "application/octet-stream")
                    if (fileUrl != null) {
                        val updatedMessage = message.copy(content = fileUrl)
                        val messageId = chatRepository.storeMessage(chatId, updatedMessage.senderId, updatedMessage)
                        chatRepository.notifyParticipants(chatId, updatedMessage, updatedMessage.senderId)
                        call.respond(HttpStatusCode.Created, mapOf("messageId" to messageId))
                    } else {
                        call.respond(HttpStatusCode.InternalServerError, "Failed to upload file")
                    }
                } else {
                    val messageId = chatRepository.storeMessage(chatId, message.senderId, message)
                    chatRepository.notifyParticipants(chatId, message, message.senderId)
                    call.respond(HttpStatusCode.Created, mapOf("messageId" to messageId))
                }
            }

            get {
                val userId = call.request.queryParameters["userId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val chats = chatRepository.getActiveChats(userId)
                call.respond(HttpStatusCode.OK, chats)
            }

            get("/details") {
                val userId = call.parameters["userId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "User ID required")
                val chatDetails = chatRepository.getChatsWithDetails(userId)
                call.respond(HttpStatusCode.OK, chatDetails)
            }

        route("/status") {
            post {
                val userId = call.parameters["userId"] ?: return@post call.respond(HttpStatusCode.BadRequest, "User ID required")
                val online = call.parameters["online"]?.toBoolean() ?: false
                profileRepository.updateAndBroadcastStatus(userId, online)
                call.respond(HttpStatusCode.OK, "Status updated")
            }
        }

        post("/device-token") {
            val userId = call.parameters["userId"]
            val token = call.parameters["token"]
            if (userId != null && token != null) {
                profileRepository.saveDeviceToken(userId, token)
                call.respond(HttpStatusCode.OK, "Device token saved")
            } else {
                call.respond(HttpStatusCode.BadRequest, "User ID and token required")
            }
        }
    }
}
