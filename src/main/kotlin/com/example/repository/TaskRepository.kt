package com.example.repository

import com.example.database.Tasks
import com.example.model.Task
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class TaskRepository {
    /**
     * Retrieves all tasks.
     */
    fun getAllTasks(): List<Task> = transaction {
        Tasks.selectAll().map(::rowToTask)
    }

    /**
     * Retrieves a specific task.
     */
    fun getTask(taskId: Int): Task? = transaction {
        Tasks.select { Tasks.taskId eq taskId }
            .map(::rowToTask)
            .singleOrNull()
    }

    /**
     * Creates a new task.
     */
    fun createTask(task: Task) = transaction {
        Tasks.insert {
            it[title] = task.title
            it[status] = task.status
        } get Tasks.taskId
    }

    /**
     * Updates an existing task.
     */
    fun updateTask(taskId: Int, updatedTask: Task): Boolean = transaction {
        Tasks.update({ Tasks.taskId eq taskId }) {
            it[title] = updatedTask.title
            it[status] = updatedTask.status
        } > 0
    }

    /**
     * Deletes a task.
     */
    fun deleteTask(taskId: Int): Boolean = transaction {
        Tasks.deleteWhere { Tasks.taskId eq taskId } > 0
    }

    /**
     * Maps a database row to a Task object.
     */
    private fun rowToTask(row: ResultRow) = Task(
        taskId = row[Tasks.taskId],
        title = row[Tasks.title],
        status = row[Tasks.status],
        dueDate = row[Tasks.dueDate],
        assignedTo = row[Tasks.assignedTo],
        createdBy = row[Tasks.createdBy],
        createdAt = row[Tasks.createdAt]
    )

}
