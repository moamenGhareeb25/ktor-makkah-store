package com.example.routes

import com.example.auth.FirebaseAuth
import com.example.model.Profile
import com.example.repository.ProfileRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting(profileRepository: ProfileRepository) {
    routing {
        // Root route
        get("/") {
            call.respondText("Welcome to the Ktor application!", ContentType.Text.Plain)
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
                        println("Received ID token: $idToken")
                        val decodedToken = FirebaseAuth.verifyIdToken(idToken)
                        if (decodedToken != null) {
                            val userId = decodedToken.uid
                            println("User ID: $userId")
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
                    e.printStackTrace()
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
        }
    }
}