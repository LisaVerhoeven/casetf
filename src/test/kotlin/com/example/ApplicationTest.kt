import io.ktor.http.*
import io.ktor.server.testing.*
import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SchemaUtils
import com.example.database.Users
import com.example.module
import com.example.plugins.loggedInUserID

import io.ktor.client.request.*
import io.ktor.client.statement.*
import org.junit.jupiter.api.Assertions.assertTrue



class ApplicationTest {
    @BeforeEach
    fun setup() {
        // Setup in-memory database
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;", driver = "org.h2.Driver")
        transaction {
            SchemaUtils.create(Users)
        }
    }

    @Test
    fun testRegister() = testApplication {
        application {
            module()
        }
        val response = client.post("/register") {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(listOf("name" to "John Doe", "age" to "30", "username" to "johndoe", "password" to "secret").formUrlEncode())
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("Successful registration"))
    }

    @Test
    fun testLogin() = testApplication {
        application { module() }

        val registerResponse = client.post("/register") {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(listOf("name" to "Test User", "age" to "25", "username" to "testuser", "password" to "password123").formUrlEncode())
        }
        assertEquals(HttpStatusCode.OK, registerResponse.status)

        // Attempt to log in
        val loginResponse = client.post("/login") {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(listOf("username" to "testuser", "password" to "password123").formUrlEncode())
        }

        assertEquals(HttpStatusCode.Found, loginResponse.status)
        assertTrue(loginResponse.headers.contains("Location", "/userdetails"))
    }

    @Test
    fun testUserDetails() = testApplication {
        application { module() }
        // Directly set loggedInUserID assuming login is successful and user ID is 1 for simplicity
        loggedInUserID = 1

        val response = client.get("/userdetails")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("User Details"))
    }

    @Test
    fun testDeleteUser() = testApplication {
        application { module() }
        // Simulate user being logged in
        loggedInUserID = 1

        val response = client.post("/delete")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("User deleted"))
    }

    @Test
    fun testEditUser() = testApplication {
        application { module() }
        // Simulate user being logged in
        loggedInUserID = 1

        val response = client.post("/edit") {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(listOf("name" to "New Name").formUrlEncode())
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("User updated"))

    }


}
