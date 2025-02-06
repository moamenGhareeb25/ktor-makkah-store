package com.example.routes

import com.example.auth.FirebaseNotificationService
import com.example.firebase.FirebaseStorageService
import com.example.model.*
import com.example.repository.*
import com.example.service.ActionType
import com.example.service.AuthorizationService
import com.example.service.ProfileService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

fun Application.configureRouting(
    chatRepository: ChatRepository,
    profileRepository: ProfileRepository,
    taskRepository: TaskRepository,
    kpiRepository: KPIRepository,
    workLogRepository: WorkLogRepository,
    delegationRepository: DelegationRepository,
    profileService: ProfileService,
    notificationService: NotificationService,
    authorizationService: AuthorizationService,
    firebaseNotificationService: FirebaseNotificationService
) {
    routing {
        intercept(ApplicationCallPipeline.Monitoring) {
            println("📥 Incoming request: ${call.request.httpMethod.value} ${call.request.uri}")
        }
        rootRoutes()
        profileRoutes(profileService, notificationService, authorizationService)
        chatRoutes(chatRepository)
        taskRoutes(taskRepository, kpiRepository)
        statusRoutes(profileRepository)
        deviceTokenRoutes(profileRepository)
        kpiRoutes(kpiRepository, delegationRepository)
        dashboardRoutes(taskRepository, kpiRepository)
        workRoutes(workLogRepository)
        delegationRoutes(delegationRepository, authorizationService)
        profileReviewRoutes(profileService, delegationRepository)
        notification(firebaseNotificationService)
    }
}


private fun Route.rootRoutes() {
    get("/") {
        call.respondText("Welcome to the Ktor application!", ContentType.Text.Plain)
    }
}

/**
 * Configures the profile-related routes, handling profile creation, updates, deletion, checks,
 * and real-time online/offline status management.
 */
private fun Route.profileRoutes(
    profileService: ProfileService,
    notificationService: NotificationService,
    authorizationService: AuthorizationService
) {
    route("/profile") {

        // Retrieve the authenticated user's profile
        get {
            val userId = call.validateAndExtractUserId()
                ?: return@get call.respond(HttpStatusCode.Unauthorized, "Invalid User ID")

            val profile = profileService.getProfile(userId)
            if (profile != null) {
                call.respond(HttpStatusCode.OK, profile)
            } else {
                call.respond(HttpStatusCode.NotFound, "Profile not found")
            }
        }

        // Retrieve any profile by userId
        get("/{userId}") {
            val userId = call.parameters["userId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "User ID required")

            val profile = profileService.getProfile(userId)
            if (profile != null) {
                call.respond(HttpStatusCode.OK, profile)
            } else {
                call.respond(HttpStatusCode.NotFound, "Profile not found")
            }
        }

        // Create a new profile
        post {
            val requesterId = call.validateAndExtractUserId()
                ?: return@post call.respond(HttpStatusCode.Unauthorized, "Invalid User ID")

            val profile = call.receive<Profile>()

            // Check if requester is authorized to create the profile
            if (!authorizationService.isAuthorizedForProfileAction(requesterId, profile.userId, ActionType.CREATE)) {
                call.respond(HttpStatusCode.Forbidden, "Not authorized to create this profile.")
                return@post
            }

            // Create the profile and notify reviewers/owner
            profileService.createProfile(profile, requesterId)
            notificationService.notifyOwnerOrReviewer(
                title = "New Profile Created",
                message = "A new profile for ${profile.name} has been created by $requesterId.",
                recipientId = authorizationService.getOwnerId()
            )
            call.respond(HttpStatusCode.Created, "Profile created successfully.")
        }

        // Update an existing profile
        put {
            val requesterId = call.validateAndExtractUserId()
                ?: return@put call.respond(HttpStatusCode.Unauthorized, "Invalid User ID")

            val updatedProfile = call.receive<Profile>()

            // Update the profile or save as pending if unauthorized
            profileService.updateProfile(updatedProfile, requesterId)
            call.respond(HttpStatusCode.Accepted, "Profile update submitted successfully.")
        }

        // Delete a profile
        delete {
            val requesterId = call.validateAndExtractUserId()
                ?: return@delete call.respond(HttpStatusCode.Unauthorized, "Invalid User ID")

            val userIdToDelete = call.parameters["userId"]
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "User ID required.")

            // Check if requester is authorized to delete the profile
            if (!authorizationService.isAuthorizedForProfileAction(requesterId, userIdToDelete, ActionType.DELETE)) {
                return@delete call.respond(HttpStatusCode.Forbidden, "Not authorized to delete this profile.")
            }

            // Attempt to delete the profile
            profileService.deleteProfile(userIdToDelete, requesterId)
            call.respond(HttpStatusCode.OK, "Profile deleted successfully.")
        }

        // Check profile existence and handle pending updates if necessary
        get("/check") {
            val requesterId = call.validateAndExtractUserId()
                ?: return@get call.respond(HttpStatusCode.Unauthorized, "Invalid User ID")

            val userIdToCheck = call.parameters["userId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "User ID required.")

            // Check profile existence and notify owner/reviewer if needed
            profileService.checkProfile(userIdToCheck, authorizationService.getOwnerId())
            call.respond(HttpStatusCode.Accepted, "Profile check initiated.")
        }

        // Review pending updates
        post("/review") {
            val params = call.receive<Map<String, String>>()
            val profileId = params["profileId"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Profile ID required.")
            val reviewerId = call.validateAndExtractUserId()
                ?: return@post call.respond(HttpStatusCode.Unauthorized, "Invalid User ID")
            val decision = params["decision"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Decision required.")

            // Process the review
            profileService.reviewPendingUpdates(profileId, decision, reviewerId)
            call.respond(HttpStatusCode.OK, "Profile review processed successfully.")
        }

        // Get all profiles
        get("/all") {
            val profiles = profileService.getAllProfiles()
            call.respond(HttpStatusCode.OK, profiles)
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

private fun Route.taskRoutes(taskRepository: TaskRepository, kpiRepository: KPIRepository) {
    route("/tasks") {
        // Fetch all tasks
        get {
            val tasks = taskRepository.getAllTasks()
            call.respond(HttpStatusCode.OK, tasks)
        }

        // Create a new task
        post {
            val task = call.receive<Task>()
            val taskId = taskRepository.createTask(task)
            call.respond(HttpStatusCode.Created, mapOf("taskId" to taskId))
        }

        // Fetch a specific task by ID
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

        // Update an existing task
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

        // Delete a task
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

        // Complete a task and update its associated KPI
        post("/{taskId}/complete") {
            val taskId = call.parameters["taskId"]?.toIntOrNull()
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Invalid task ID")
            val userId = call.parameters["userId"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, "User ID required")

            try {
                completeTaskAndUpdateKPI(taskId, userId, kpiRepository, taskRepository)
                call.respond(HttpStatusCode.OK, "Task completed and KPI updated")
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Error completing task")
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
    route("/device-token") {

        // Save or update a device token
        post {
            val requesterId = call.validateAndExtractUserId()
                ?: return@post call.respond(HttpStatusCode.Unauthorized, "Invalid User ID")

            val requestData = call.receive<Map<String, String>>()
            val token = requestData["token"]

            if (token.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Device token is required.")
                return@post
            }

            try {
                profileRepository.saveDeviceToken(requesterId, token)
                call.respond(HttpStatusCode.OK, "Device token saved successfully.")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to save device token: ${e.message}")
            }
        }

        // Retrieve a user's device token
        get {
            val requesterId = call.validateAndExtractUserId()
                ?: return@get call.respond(HttpStatusCode.Unauthorized, "Invalid User ID")

            try {
                val deviceToken = profileRepository.getDeviceToken(requesterId)
                if (deviceToken != null) {
                    call.respond(HttpStatusCode.OK, mapOf("deviceToken" to deviceToken))
                } else {
                    call.respond(HttpStatusCode.NotFound, "No device token found for this user.")
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Error fetching device token: ${e.message}")
            }
        }

        // Remove a device token (e.g., on logout)
        delete {
            val requesterId = call.validateAndExtractUserId()
                ?: return@delete call.respond(HttpStatusCode.Unauthorized, "Invalid User ID")

            try {
                profileRepository.deleteDeviceToken(requesterId)
                call.respond(HttpStatusCode.OK, "Device token removed successfully.")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to remove device token: ${e.message}")
            }
        }
    }
}


    private fun Route.kpiRoutes(kpiRepository: KPIRepository,delegationRepository:DelegationRepository) {
        route("/kpis") {
            // Fetch KPIs for a specific user
            get("/{userId}") {
                val userId =
                    call.parameters["userId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "User ID required")
                val kpis = kpiRepository.getKPIsByUser(userId)
                call.respond(HttpStatusCode.OK, kpis)
            }

            // Add a new KPI
            post {
                val kpi = call.receive<KPI>()
                val kpiId = kpiRepository.addKPI(kpi)
                call.respond(HttpStatusCode.Created, mapOf("kpiId" to kpiId))
            }

            // Update a KPI value
            put("/{kpiId}") {
                val kpiId = call.parameters["kpiId"]?.toIntOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid KPI ID")
                val newValue = call.receive<Map<String, Int>>()["value"]
                    ?: return@put call.respond(HttpStatusCode.BadRequest, "Value required")

                val success = kpiRepository.updateKPI(kpiId, newValue)
                if (success) {
                    call.respond(HttpStatusCode.OK, "KPI updated successfully")
                } else {
                    call.respond(HttpStatusCode.NotFound, "KPI not found")
                }
            }

            // Delete a KPI
            delete("/{kpiId}") {
                val kpiId = call.parameters["kpiId"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid KPI ID")

                val success = kpiRepository.deleteKPI(kpiId)
                if (success) {
                    call.respond(HttpStatusCode.OK, "KPI deleted successfully")
                } else {
                    call.respond(HttpStatusCode.NotFound, "KPI not found")
                }
            }
            post("/kpis/review") {
                val params = call.receive<Map<String, String>>()
                val kpiId = params["kpiId"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest, "Invalid KPI ID")
                val reviewerId = params["reviewerId"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Reviewer ID required")
                val newValue = params["value"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest, "Value required")

                if (delegationRepository.getRoles(reviewerId).any { it.role == "KPIUpdater" }) {
                    kpiRepository.updateKPI(kpiId, newValue)
                    call.respond(HttpStatusCode.OK, "KPI updated successfully")
                } else {
                    call.respond(HttpStatusCode.Forbidden, "Not authorized to update KPIs")
                }
            }
        }
    }

    private fun Route.dashboardRoutes(taskRepository: TaskRepository, kpiRepository: KPIRepository) {
        get("/dashboard/{userId}") {
            val userId =
                call.parameters["userId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "User ID required")

            val tasks = taskRepository.getAllTasks().filter { it.assignedTo == userId }
            val kpis = kpiRepository.getKPIsByUser(userId)

            call.respond(HttpStatusCode.OK, mapOf("tasks" to tasks, "kpis" to kpis))
        }
    }

        fun completeTaskAndUpdateKPI(
            taskId: Int,
            userId: String,
            kpiRepository: KPIRepository,
            taskRepository: TaskRepository
        ) {
            transaction {
                // Fetch the task by ID
                val task = taskRepository.getTask(taskId)
                    ?: throw IllegalArgumentException("Task not found")

                // Fetch the KPI linked to the task
                val kpi = kpiRepository.getKPIsByUser(userId).find { it.taskId == taskId }
                    ?: throw IllegalArgumentException("KPI not found for the task")

                // Increment KPI value
                val newValue = kpi.value + 1
                kpiRepository.updateKPI(kpi.kpiId!!, newValue)

                // Mark the task as completed
                taskRepository.updateTask(taskId, task.copy(status = "completed"))
            }
        }
private fun Route.workRoutes(workLogRepository: WorkLogRepository) {
    route("/work") {
        post("/start") {
            val userId = call.parameters["userId"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, "User ID required")
            val logId = workLogRepository.startWork(userId)
            call.respond(HttpStatusCode.Created, mapOf("logId" to logId, "message" to "Work started"))
        }

        post("/end") {
            val userId = call.parameters["userId"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, "User ID required")
            val success = workLogRepository.endWork(userId)
            if (success) {
                call.respond(HttpStatusCode.OK, "Work ended")
            } else {
                call.respond(HttpStatusCode.NotFound, "No active work session found")
            }
        }

        get("/hours/{userId}") {
            val userId = call.parameters["userId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "User ID required")
            val totalHours = workLogRepository.getTotalWorkHours(userId)
            call.respond(HttpStatusCode.OK, mapOf("totalHours" to totalHours))
        }
    }
}

private fun Route.delegationRoutes(
    delegationRepository: DelegationRepository,
    authorizationService: AuthorizationService
) {
    route("/delegations") {

        /**
         * Assigns a delegation role to a manager.
         * Only admins can assign roles.
         */
        post("/assign") {
            val requesterId = call.validateAndExtractUserId()
                ?: return@post call.respond(HttpStatusCode.Unauthorized, "Invalid User ID")

            val params = call.receive<Map<String, String>>()
            val managerId = params["managerId"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Manager ID required")
            val role = params["role"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Role required")

            // Ensure only admins can assign roles
            if (!authorizationService.isAuthorizedForProfileAction(requesterId, managerId, ActionType.CREATE)) {
                return@post call.respond(HttpStatusCode.Forbidden, "Not authorized to assign roles.")
            }

            delegationRepository.assignRole(managerId, role, requesterId)
            call.respond(HttpStatusCode.Created, "Role $role assigned successfully to $managerId")
        }

        /**
         * Revokes a role from a manager.
         * Only admins can revoke roles.
         */
        post("/revoke") {
            val requesterId = call.validateAndExtractUserId()
                ?: return@post call.respond(HttpStatusCode.Unauthorized, "Invalid User ID")

            val params = call.receive<Map<String, String>>()
            val managerId = params["managerId"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Manager ID required")
            val role = params["role"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Role required")

            // Ensure only admins can revoke roles
            if (!authorizationService.isAuthorizedForProfileAction(requesterId, managerId, ActionType.DELETE)) {
                return@post call.respond(HttpStatusCode.Forbidden, "Not authorized to revoke roles.")
            }

            delegationRepository.revokeRole(managerId, role)
            call.respond(HttpStatusCode.OK, "Role $role revoked successfully from $managerId")
        }

        /**
         * Retrieves all roles assigned to a specific manager.
         * Only admins or the manager themselves can view their roles.
         */
        get("/{managerId}") {
            val requesterId = call.validateAndExtractUserId()
                ?: return@get call.respond(HttpStatusCode.Unauthorized, "Invalid User ID")

            val managerId = call.parameters["managerId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Manager ID required")

            // Ensure the requester is either the admin or the manager themselves
            if (requesterId != managerId && !authorizationService.isAdmin(requesterId)) {
                return@get call.respond(HttpStatusCode.Forbidden, "Not authorized to view roles.")
            }

            val roles = delegationRepository.getRoles(managerId)
            call.respond(HttpStatusCode.OK, roles)
        }
    }
}
private fun Route.profileReviewRoutes(
    profileService: ProfileService,
    delegationRepository: DelegationRepository
) {
    route("/profiles/review") {

        /**
         * Handles profile update reviews (accept/reject/modify).
         * Only delegated reviewers can perform this action.
         */
        post {
            try {
                val reviewerId = call.validateAndExtractUserId()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, "Invalid User ID")

                val params = call.receive<Map<String, String>>()
                val profileId = params["profileId"]
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "Profile ID required")
                val decision = params["decision"]
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "Decision required")

                // Ensure only Profile Reviewers can review
                if (!delegationRepository.getRoles(reviewerId).any { it.role == "ProfileReviewer" }) {
                    return@post call.respond(HttpStatusCode.Forbidden, "Not authorized to review profiles")
                }

                when (decision.uppercase()) {
                    "ACCEPT" -> {
                        profileService.reviewPendingUpdates(profileId, decision, reviewerId)
                        call.respond(HttpStatusCode.OK, "Profile updates for $profileId approved")
                    }

                    "REJECT" -> {
                        profileService.reviewPendingUpdates(profileId, decision, reviewerId)
                        call.respond(HttpStatusCode.OK, "Profile updates for $profileId rejected")
                    }

                    "MODIFY" -> {
                        val modifiedProfile = call.receive<Profile>()
                        profileService.modifyPendingUpdates(profileId, modifiedProfile, reviewerId)
                        call.respond(HttpStatusCode.OK, "Profile updates for $profileId modified and applied")
                    }

                    else -> call.respond(HttpStatusCode.BadRequest, "Invalid decision type")
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Error reviewing profile: ${e.message}")
            }
        }


        /**
         * Retrieves all profiles that require review.
         * Only profile reviewers can access this list.
         */
        get("/pending") {
            val reviewerId = call.validateAndExtractUserId()
                ?: return@get call.respond(HttpStatusCode.Unauthorized, "Invalid User ID")

            // Ensure only Profile Reviewers can access pending profiles
            if (!delegationRepository.getRoles(reviewerId).any { it.role == "ProfileReviewer" }) {
                return@get call.respond(HttpStatusCode.Forbidden, "Not authorized to view pending profiles")
            }

            val pendingProfiles = profileService.getPendingProfiles()
            call.respond(HttpStatusCode.OK, pendingProfiles)
        }
    }
}
private fun Route.notification(firebaseNotificationService: FirebaseNotificationService) {
    route("/send-notification") {
        post {
            val request = call.receive<Map<String, String>>()
            val token = request["token"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Token required")
            val title = request["title"] ?: "New Notification"
            val body = request["body"] ?: "You have a new update."
            val sound = request["sound"] ?: "default_notification"
            val targetScreen = request["targetScreen"] ?: "MainActivity"
            val showDialog = request["showDialog"]?.toBoolean() ?: false

            // Convert `data` safely
            val data = try {
                request["data"]?.let { Json.decodeFromString<Map<String, String>>(it) } ?: emptyMap()
            } catch (e: Exception) {
                emptyMap()
            }

            firebaseNotificationService.sendNotification(token, title, body, sound, targetScreen, showDialog, data)

            call.respond(HttpStatusCode.OK, "Notification sent successfully")
        }
    }
}



