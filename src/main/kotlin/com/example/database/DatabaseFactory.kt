package com.example.database

import org.jetbrains.exposed.sql.Database

object DatabaseFactory {
    fun init(useH2: Boolean = false) {
        if (useH2) {
            // Connect to an in-memory H2 database
            Database.connect(
                url = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;",
                driver = "org.h2.Driver"
            )
        } else {
            // Connect to PostgreSQL (or any other database)
            Database.connect(
                url = "jdbc:postgresql://localhost:5432/mydatabase",
                driver = "org.postgresql.Driver",
                user = "postgres",
                password = "lisa" // Use the actual password here
            )
        }
    }
}
