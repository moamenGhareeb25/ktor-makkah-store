package com.example.service

import com.example.repository.DelegationRepository

/**
 * Handles authorization for different user actions across profiles, KPIs, and delegations.
 */
class AuthorizationService(private val delegationRepository: DelegationRepository) {

    companion object {
        const val ROLE_OWNER = "Owner"
        const val ROLE_ADMIN = "Admin"
        const val ROLE_PROFILE_REVIEWER = "ProfileReviewer"
        const val ROLE_KPI_UPDATER = "KPIUpdater"
    }

    /**
     * Retrieves the system's owner or primary administrator.
     * This ensures that the correct person gets notified for reviews and approvals.
     */
    fun getOwnerId(): String {
        return delegationRepository.getRolesForRole(ROLE_OWNER).firstOrNull()?.managerId
            ?: throw IllegalStateException("No owner found in the system")
    }

    /**
     * Checks if a user is authorized to perform an action on a profile.
     */
    fun isAuthorizedForProfileAction(requesterId: String, targetUserId: String, actionType: ActionType): Boolean {
        return when (actionType) {
            ActionType.CREATE -> isOwner(requesterId, targetUserId) || isAdmin(requesterId)
            ActionType.UPDATE -> isOwner(requesterId, targetUserId) || isProfileReviewer(requesterId)
            ActionType.DELETE -> isOwner(requesterId, targetUserId) || isAdmin(requesterId)
        }
    }

    /**
     * Checks if a user is authorized to review and approve profile updates.
     */
    fun isAuthorizedForProfileReview(requesterId: String): Boolean {
        return isAdmin(requesterId) || isProfileReviewer(requesterId)
    }

    /**
     * Checks if a user is authorized to update KPI values.
     */
    fun isAuthorizedForKPIUpdate(requesterId: String): Boolean {
        return isAdmin(requesterId) || isKPIUpdater(requesterId)
    }

    /**
     * Checks if a user is the owner of a profile.
     * Owners have full control over their own profiles.
     */
    private fun isOwner(requesterId: String, targetUserId: String): Boolean {
        return requesterId == targetUserId
    }

    /**
     * Checks if a user has a specific assigned role.
     */
    private fun hasRole(requesterId: String, role: String): Boolean {
        return delegationRepository.isReviewer(requesterId, role)
    }

    /**
     * Checks if the requester is a Profile Reviewer.
     * Reviewers can approve or reject profile changes.
     */
    private fun isProfileReviewer(requesterId: String): Boolean {
        return hasRole(requesterId, ROLE_PROFILE_REVIEWER)
    }

    /**
     * Checks if the requester has Admin privileges.
     * Admins have access to all management functions.
     */
     fun isAdmin(requesterId: String): Boolean {
        return hasRole(requesterId, ROLE_ADMIN)
    }

    /**
     * Checks if the requester is assigned as a KPI Updater.
     * KPI Updaters manage user performance data.
     */
    private fun isKPIUpdater(requesterId: String): Boolean {
        return hasRole(requesterId, ROLE_KPI_UPDATER)
    }
}

/**
 * Enum representing different user actions requiring authorization.
 */
enum class ActionType {
    CREATE,
    UPDATE,
    DELETE
}
