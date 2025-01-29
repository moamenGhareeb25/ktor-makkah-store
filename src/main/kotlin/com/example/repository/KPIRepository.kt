package com.example.repository

import com.example.database.KPIs
import com.example.model.KPI
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class KPIRepository {
    /**
     * Retrieves all KPIs for a user.
     */
    fun getKPIsByUser(userId: String): List<KPI> = transaction {
        KPIs.select { KPIs.userId eq userId }
            .map(::rowToKPI)
    }

    /**
     * Adds a new KPI entry.
     */
    fun addKPI(kpi: KPI): Int = transaction {
        KPIs.insert {
            it[userId] = kpi.userId
            it[category] = kpi.category
            it[value] = kpi.value
        } get KPIs.kpiId
    }

    /**
     * Updates an existing KPI.
     */
    fun updateKPI(kpiId: Int, newValue: Int): Boolean = transaction {
        KPIs.update({ KPIs.kpiId eq kpiId }) {
            it[value] = newValue
        } > 0
    }

    /**
     * Deletes a KPI entry.
     */
    fun deleteKPI(kpiId: Int): Boolean = transaction {
        KPIs.deleteWhere { KPIs.kpiId eq kpiId } > 0
    }

    /**
     * Maps a database row to a KPI object.
     */
    private fun rowToKPI(row: ResultRow) = KPI(
        kpiId = row[KPIs.kpiId],
        userId = row[KPIs.userId],
        category = row[KPIs.category],
        value = row[KPIs.value],
        createdAt = row[KPIs.createdAt],
        createdBy = row[KPIs.createdBy]
    )
}
