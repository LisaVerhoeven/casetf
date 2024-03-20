package com.example.plugins

import com.example.database.Users
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import org.mindrot.jbcrypt.BCrypt

//global variable to keep track of user
var loggedInUserID = -1;

fun Application.configureRouting() {
    routing {
        //home page with login and register
        get("/") {
            val htmlContent = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <title>Home</title>
                    </head>
                    <body>
                        <h2>Login</h2>
                        <form action="/login" method="post">
                            <input type="text" name="username" placeholder="Username"><br>
                            <input type="password" name="password" placeholder="Password"><br>
                            <input type="submit" value="Login">
                        </form>
                        <h2>Register</h2>
                        <form action="/register" method="post">
                            <input type="text" name="name" placeholder="Name"><br>
                            <input type="number" name="age" placeholder="Age"><br>
                            <input type="text" name="username" placeholder="Username"><br>
                            <input type="password" name="password" placeholder="Password"><br>
                            <input type="submit" value="Register">
                        </form>
                    </body>
                    </html>
                """.trimIndent()
            call.respondText(htmlContent, ContentType.Text.Html)
        }

        post("/login") {
            //checking credentials of user
            val postParameters = call.receiveParameters()
            val username = postParameters["username"] ?: ""
            val password = postParameters["password"] ?: ""

            val user = transaction {
                Users.select { Users.username eq username }.singleOrNull()
            }

            if (user == null) {
                // User does not exist
                val userNotExistMessage = """
        <html>
        <head>
            <title>Login Error</title>
        </head>
        <body>
            <h1>Login Error</h1>
            <p>User does not exist. Please try again or register.</p>
            <a href="/">Go back to login</a>
        </body>
        </html>
    """.trimIndent()
                call.respondText(userNotExistMessage, ContentType.Text.Html)
            }
            else if (BCrypt.checkpw(password, user?.get(Users.password) ?: "")) {
                // Redirect to user details page if login is successful
                call.respondRedirect("/userdetails")
                loggedInUserID = user?.get(Users.id) ?:-1;


            } else {
                // Redirect back to login page and show an error message
                val errorMessage = """
            <html>
            <head>
                <title>Login Error</title>
            </head>
            <body>
                <h1>Login Error</h1>
                <p>Invalid username or password. Please try again.</p>
                <a href="/">Go back to login</a>
            </body>
            </html>
        """.trimIndent()

                call.respondText(errorMessage, ContentType.Text.Html)
            }
        }

        post("/register") {

            val postParameters = call.receiveParameters()
            val name = postParameters["name"] ?: ""
            val age = postParameters["age"]?.toIntOrNull() ?: 0
            val username = postParameters["username"] ?: ""
            val password = postParameters["password"] ?: ""


            val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt())

            try {
                // Insert the user into the database
                transaction {
                    Users.insert {
                        it[Users.name] = name
                        it[Users.age] = age
                        it[Users.username] = username
                        it[Users.password] = hashedPassword
                    }
                }


                val redirectLink = "/"
                call.respondText("Successful registration. <a href=\"$redirectLink\">Go back to login</a>", ContentType.Text.Html)
            } catch (e: Exception) {
                // Log and display errors
                println("Error inserting user: ${e.message}")
                call.respond(HttpStatusCode.InternalServerError, "Error registering user.")
            }
        }

        get("/userdetails") {

            val userDetails = transaction {
                Users.select { Users.id eq loggedInUserID }.singleOrNull()
            }

            if (userDetails != null) {
                val name = userDetails[Users.name]
                val age = userDetails[Users.age]
                val username = userDetails[Users.username]

                val userDetailsHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>User Details</title>
            </head>
            <body>
                <h1>User Details</h1>
                <div id="userDetails">
                    <p>Name: $name</p>
                    <p>Age: $age</p>
                    <p>Username: $username</p>
                    <!-- Edit User Button -->
                    <button onclick="document.getElementById('editForm').style.display='block';document.getElementById('userDetails').style.display='none';">Edit</button>
                </div>
                <!-- Edit User Form -->
                <form id="editForm" action="/edit" method="post" style="display:none;">
                    <input type="text" name="name" placeholder="New Name"><br>
                    <input type="submit" value="Save Changes">
                </form>
                <!-- Delete User Button -->
                <form action="/delete" method="post">
                    <input type="submit" value="Delete User">
                </form>
                <!-- Logout Button -->
                <form action="/logout" method="post">
                    <input type="submit" value="Logout">
                </form>
            </body>
            </html>
        """.trimIndent()

                call.respondText(userDetailsHtml, ContentType.Text.Html)
            } else {
                // Handle case where user details are not found
                call.respondText("User details not found.", ContentType.Text.Plain)
            }
        }


        post("/logout") {
            //when logged out the id of user is -1
            loggedInUserID=-1
            call.respondRedirect("/")
        }


        post("/delete") {
            try {
                transaction {
                    // Execute the deletion query
                    Users.deleteWhere { Users.id eq loggedInUserID }
                }
                call.respondText("User deleted. Redirecting to home...<br><br><a href=\"/\">Go back to home</a>", ContentType.Text.Html)
            } catch (e: Exception) {
                call.respondText("Error deleting user: ${e.message}", ContentType.Text.Plain)
            }
        }


        post("/edit") {
            val postParameters = call.receiveParameters()
            val newName = postParameters["name"] ?: ""

            try {
                transaction {
                    // Update the user's information in the database
                    Users.update({ Users.id eq loggedInUserID }) {
                        it[Users.name] = newName
                    }
                }
                val redirectLink = "/userdetails" // Link to the user details page
                call.respondText("User updated with name: $newName. <a href=\"$redirectLink\">Go back to user details</a>", ContentType.Text.Html)
            } catch (e: Exception) {
                call.respondText("Error updating user: ${e.message}", ContentType.Text.Plain)
            }
        }
    }
}
