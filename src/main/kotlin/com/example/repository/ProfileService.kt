package com.example.service

import com.example.auth.FirebaseAuthManager
import com.example.model.Profile
import com.example.repository.NotificationService
import com.example.repository.ProfileRepository

/**
 * Handles profile-related operations, including creation, updates, deletion, and reviews.
 */
class ProfileService(
    private val profileRepository: ProfileRepository,
    private val pendingUpdateService: PendingUpdateService,
    private val notificationService: NotificationService,
    private val authorizationService: AuthorizationService
) {

    /**
     * Retrieves a profile by user ID.
     * @param userId The ID of the user whose profile is to be retrieved.
     * @return The profile if found, null otherwise.
     */
    fun getProfile(userId: String): Profile? {
        return profileRepository.getProfile(userId)
    }

    /**
     * Creates a new profile and notifies the owner.
     * @param profile The profile to be created.
     * @param requesterId The ID of the requester performing the action.
     */
    suspend fun createProfile(profile: Profile, requesterId: String) {
        if (!authorizationService.isAuthorizedForProfileAction(requesterId, profile.userId, ActionType.CREATE)) {
            throw IllegalArgumentException("Unauthorized to create this profile.")
        }
        profileRepository.createProfile(profile)
        notificationService.notifyOwnerOrReviewer(
            title = "Profile Created",
            message = "A new profile for ${profile.name} has been created by $requesterId.",
            recipientId = requesterId,
            type = "new profile created"
        )
    }

    /**
     * Checks if a profile exists. If not, notifies the owner to create it.
     * If there are pending updates, notifies the owner to review them.
     * @param userId The ID of the user whose profile is to be checked.
     * @param ownerId The ID of the owner to be notified.
     */
    suspend fun checkProfile(userId: String, ownerId: String): Profile? {
        val profile = profileRepository.getProfile(userId)
        if (profile != null) {
            val pendingUpdates = pendingUpdateService.getPendingUpdates(userId)
            if (pendingUpdates.isNotEmpty()) {
                notificationService.notifyOwnerOrReviewer(
                    title = "Profile Update Pending",
                    message = "A profile update for ${profile.name} requires your review.",
                    recipientId = ownerId,
                    type = "check profile "
                )
            }
            return profile
        }

        // Check if the user exists in Firebase but not in the database
        val firebaseProfileExists = FirebaseAuthManager.checkUserInFirebase(userId)
        if (firebaseProfileExists) {
            notificationService.notifyOwnerOrReviewer(
                title = "Profile Exists in Firebase",
                message = "The profile for user ID $userId exists in Firebase but not in the database. Please review and create it.",
                recipientId = ownerId,
                type = "profile update"

            )
            return null
        }

        // Notify the owner if the profile is missing completely
        notificationService.notifyOwnerOrReviewer(
            title = "Profile Missing",
            message = "The profile for user ID $userId is missing. Please create it.",
            recipientId = ownerId,
            type = "profile update"

        )
        return null
    }

    /**
     * Updates a profile. If unauthorized, saves changes as pending updates and notifies the reviewer.
     * @param profile The profile with updated information.
     * @param requesterId The ID of the requester performing the update.
     */
    suspend fun updateProfile(profile: Profile, requesterId: String) {
        if (!authorizationService.isAuthorizedForProfileAction(requesterId, profile.userId, ActionType.UPDATE)) {
            pendingUpdateService.savePendingUpdate(profile)
            notificationService.notifyOwnerOrReviewer(
                title = "Profile Update Pending",
                message = "A profile update for ${profile.name} requires your review.",
                recipientId = requesterId,
                type = "profile update"
            )
        } else {
            profileRepository.updateProfile(profile)
            notificationService.notifyUser(
                title = "Profile Updated",
                message = "Your profile has been updated successfully.",
                recipientId = profile.userId,
                type = "profile update"
            )
        }
    }

    /**
     * Deletes a profile. Only the owner or an admin can perform this action.
     * @param userId The ID of the user whose profile is to be deleted.
     * @param requesterId The ID of the requester performing the deletion.
     */
    suspend fun deleteProfile(userId: String, requesterId: String) {
        if (!authorizationService.isAuthorizedForProfileAction(requesterId, userId, ActionType.DELETE)) {
            throw IllegalArgumentException("Unauthorized to delete this profile.")
        }
        profileRepository.deleteProfile(userId)
        notificationService.notifyUser(
            title = "Profile Deleted",
            message = "Your profile has been deleted.",
            recipientId = userId,
            type = "deleteProfile"
        )
    }

    /**
     * Retrieves all profiles in the database.
     * @return A list of all profiles.
     */
    fun getAllProfiles(): List<Profile> {
        return profileRepository.getAllProfiles()
    }

    /**
     * Handles the review of pending updates for a profile.
     * @param profileId The ID of the profile being reviewed.
     * @param decision The decision ("ACCEPT" or "REJECT").
     * @param reviewerId The ID of the reviewer.
     */
    suspend fun reviewPendingUpdates(profileId: String, decision: String, reviewerId: String) {
        val pendingUpdates = pendingUpdateService.getPendingUpdates(profileId)

        when (decision.uppercase()) {
            "ACCEPT" -> {
                pendingUpdateService.applyPendingUpdates(profileId)
                notificationService.notifyUser(
                    title = "Profile Update Approved",
                    message = "Your profile updates have been approved.",
                    recipientId = profileId,
                    type = "review decision"
                )
            }
            "REJECT" -> {
                pendingUpdateService.clearPendingUpdates(profileId)
                notificationService.notifyUser(
                    title = "Profile Update Rejected",
                    message = "Your profile updates were rejected.",
                    recipientId = profileId,
                    type = "review decision"
                )
            }
            else -> throw IllegalArgumentException("Invalid decision: $decision")
        }
    }

    /**
     * Updates the online status of a user.
     * @param userId The ID of the user.
     * @param isOnline The online status to set.
     */
    suspend fun updateOnlineStatus(userId: String, isOnline: Boolean) {
        profileRepository.updateAndBroadcastStatus(userId, isOnline)
    }
    /**
     * **Modifies and applies pending updates after review.**
     */
    suspend fun modifyPendingUpdates(profileId: String, modifiedProfile: Profile, reviewerId: String) {
        if (!authorizationService.isAuthorizedForProfileAction(reviewerId, profileId, ActionType.UPDATE)) {
            throw IllegalArgumentException("Unauthorized to modify profile updates.")
        }

        profileRepository.updateProfile(modifiedProfile)
        notificationService.notifyUser(
            title = "Profile Updated",
            message = "Your profile has been modified and updated.",
            recipientId = profileId,
            type = "update profile "
        )
    }

    /**
     * Retrieves a list of all profiles that require review.
     */
    fun getPendingProfiles(): List<Profile> {
        return profileRepository.getAllPendingProfiles()
    }
    fun getAllUsers(): List<String> {
        return profileRepository.getAllUsers()
    }

}
