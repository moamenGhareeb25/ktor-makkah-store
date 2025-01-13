package com.example.database

import org.jetbrains.exposed.sql.Table

object Tasks : Table("tasks") {
    val taskId = integer("task_id").autoIncrement() // Primary key column
    val title = varchar("title", 255)
    val description = text("description").nullable()
    val status = varchar("status", 50).default("pending")
    val dueDate = long("due_date").nullable()
    val assignedTo = varchar("assigned_to", 50).nullable() // References profiles.userId
    val createdBy = varchar("created_by", 50) // References profiles.userId
    val createdAt = long("created_at").default(System.currentTimeMillis())

    override val primaryKey = PrimaryKey(taskId)
}
