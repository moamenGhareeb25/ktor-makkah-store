package com.example.repository

import com.example.database.Tasks
import com.example.model.Task
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction


class TaskRepository {
    fun getAllTasks(): List<Task> {
        return transaction {
            Tasks.selectAll().map { row ->
                Task(
                    taskId = row[Tasks.taskId],
                    title = row[Tasks.title],
                    description = row[Tasks.description],
                    status = row[Tasks.status],
                    dueDate = row[Tasks.dueDate],
                    assignedTo = row[Tasks.assignedTo],
                    createdBy = row[Tasks.createdBy],
                    createdAt = row[Tasks.createdAt]
                )
            }
        }
    }

    fun createTask(task: Task): Int {
        return transaction {
            Tasks.insert {
                it[title] = task.title
                it[description] = task.description
                it[status] = task.status
                it[dueDate] = task.dueDate
                it[assignedTo] = task.assignedTo
                it[createdBy] = task.createdBy
            }[Tasks.taskId]
        }
    }


    fun getTask(taskId: Int): Task? {
        return transaction {
            Tasks.select { Tasks.taskId eq taskId }
                .map { row ->
                    Task(
                        taskId = row[Tasks.taskId],
                        title = row[Tasks.title],
                        description = row[Tasks.description],
                        status = row[Tasks.status],
                        dueDate = row[Tasks.dueDate],
                        assignedTo = row[Tasks.assignedTo],
                        createdBy = row[Tasks.createdBy],
                        createdAt = row[Tasks.createdAt]
                    )
                }.singleOrNull()
        }
    }

    fun updateTask(taskId: Int, updatedTask: Task): Boolean {
        return transaction {
            Tasks.update({ Tasks.taskId eq taskId }) {
                it[title] = updatedTask.title
                it[description] = updatedTask.description
                it[status] = updatedTask.status
                it[dueDate] = updatedTask.dueDate
                it[assignedTo] = updatedTask.assignedTo
            } > 0
        }
    }

    fun deleteTask(taskId: Int): Boolean {
        return transaction {
            Tasks.deleteWhere { Tasks.taskId eq taskId } > 0
        }
    }
}
