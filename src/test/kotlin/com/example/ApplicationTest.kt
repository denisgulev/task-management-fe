package com.example

import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertTrue

class ApplicationTest {
    @Test
    fun testRoot() = testApplication {
        assertTrue { true }
    }
}
