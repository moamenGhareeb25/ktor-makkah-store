package com.example.database

import org.jetbrains.exposed.sql.Table

object Delegations : Table("delegations") {
    val userId = varchar("user_id", 50).references(ProfileTable.userId)

    val delegationId = integer("delegation_id").autoIncrement()
    val managerId = varchar("manager_id", 50).references(ProfileTable.userId) // Assigned manager
    val role = varchar("role", 50) // Role type: "ProfileReviewer" or "KPIUpdater"
    val assignedBy = varchar("assigned_by", 50).references(ProfileTable.userId) // Who assigned the role
    val assignedAt = long("assigned_at").default(System.currentTimeMillis())
    override val primaryKey = PrimaryKey(userId, role)
}

