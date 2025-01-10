package com.example

import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {

    @Test
    fun testRoot() {
        withTestApplication({ module() }) { // Ensure `module()` is not called multiple times
            handleRequest(HttpMethod.Get, "/").apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
        }
    }


}
