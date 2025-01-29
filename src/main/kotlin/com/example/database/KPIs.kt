package com.example.database

import org.jetbrains.exposed.sql.Table

object KPIs : Table("kpis") {
    val kpiId = integer("kpi_id").autoIncrement() // Primary key
    val userId = varchar("user_id", 50).references(ProfileTable.userId) // User associated with this KPI
    val taskId = integer("task_id").references(Tasks.taskId).nullable() // Linked task
    val category = varchar("category", 100) // KPI category (e.g., "Orders Completed")
    val value = integer("value").default(0) // Value of the KPI (e.g., "10 orders completed")
    val createdBy = varchar("created_by", 50) // Who inputted this KPI
    val createdAt = long("created_at").default(System.currentTimeMillis())

    override val primaryKey = PrimaryKey(kpiId)
}
