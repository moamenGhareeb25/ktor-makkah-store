package com.example.repository

import com.example.database.Delegations
import com.example.model.Delegation
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

class DelegationRepository {

    /**
     * Assigns a role to a manager.
     */
    fun assignRole(managerId: String, role: String, assignedBy: String) {
        transaction {
            Delegations.insert {
                it[this.managerId] = managerId
                it[this.role] = role
                it[this.assignedBy] = assignedBy
            }
        }
    }

    /**
     * Revokes a role from a manager.
     */
    fun revokeRole(managerId: String, role: String) {
        transaction {
            Delegations.deleteWhere { (Delegations.managerId eq managerId) and (Delegations.role eq role) }
        }
    }

    /**
     * Gets all roles assigned to a specific manager.
     */
    fun getRoles(managerId: String): List<Delegation> {
        return transaction {
            Delegations.select { Delegations.managerId eq managerId }.map {
                Delegation(
                    delegationId = it[Delegations.delegationId],
                    managerId = it[Delegations.managerId],
                    role = it[Delegations.role],
                    assignedBy = it[Delegations.assignedBy],
                    assignedAt = it[Delegations.assignedAt]
                )
            }
        }
    }

    /**
     * Checks if a user has a specific delegation role.
     * @param userId The user ID to check.
     * @param role The role to verify (e.g., "ProfileReviewer").
     * @return True if the user has the role, false otherwise.
     */
    fun isReviewer(userId: String, role: String): Boolean {
        return transaction {
            Delegations.select {
                (Delegations.managerId eq userId) and (Delegations.role eq role)
            }.count() > 0
        }
    }

    /**
     * Retrieves all users assigned to a specific role.
     * @param role The role name (e.g., "Owner").
     * @return List of Delegation objects representing assigned users.
     */

    fun getRolesForRole(role: String): List<Delegation> {
        return transaction {
            Delegations.select { Delegations.role eq role }
                .map {
                    Delegation(
                        delegationId = it[Delegations.delegationId],
                        managerId = it[Delegations.managerId],
                        role = it[Delegations.role],
                        assignedBy = it[Delegations.assignedBy],
                        assignedAt = it[Delegations.assignedAt]
                    )
                }
        }
    }

}

