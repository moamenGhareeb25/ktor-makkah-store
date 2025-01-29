package com.example.utils

import io.ktor.server.application.*
import io.ktor.server.request.*

fun ApplicationCall.validateAndExtractUserId(): String? {
    return request.header("Authorization")?.removePrefix("Bearer ")
}
