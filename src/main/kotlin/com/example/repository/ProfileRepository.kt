package com.example.repository

import com.example.database.ProfileTable
import com.example.model.Profile
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class ProfileRepository {
    fun getProfile(userId: String): Profile? {
        return transaction {
            ProfileTable.select { ProfileTable.userId eq userId }
                .map { row ->
                    Profile(
                        userId = row[ProfileTable.userId],
                        name = row[ProfileTable.name],
                        email = row[ProfileTable.email],
                        personalNumber = row[ProfileTable.personalNumber],
                        workNumber = row[ProfileTable.workNumber],
                        profilePictureUrl = row[ProfileTable.profilePictureUrl]
                    )
                }.singleOrNull()
        }
    }

    fun updateProfile(profile: Profile) {
        transaction {
            ProfileTable.update({ ProfileTable.userId eq profile.userId }) {
                it[name] = profile.name
                it[email] = profile.email
                it[personalNumber] = profile.personalNumber
                it[workNumber] = profile.workNumber
                it[profilePictureUrl] = profile.profilePictureUrl
            }
        }
    }

    fun createProfile(profile: Profile) {
        transaction {
            ProfileTable.insert {
                it[userId] = profile.userId
                it[name] = profile.name
                it[email] = profile.email
                it[personalNumber] = profile.personalNumber
                it[workNumber] = profile.workNumber
                it[profilePictureUrl] = profile.profilePictureUrl
            }
        }
    }

    fun deleteProfile(userId: String) {
        transaction {
            ProfileTable.deleteWhere { ProfileTable.userId eq userId }
        }
    }
}