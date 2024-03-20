package com.example.database

import org.jetbrains.exposed.sql.Table

object Users : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", length = 255)
    val age = integer("age")
    val username = varchar("username", length = 50).uniqueIndex()
    val password = varchar("password", length = 64)
    override val primaryKey = PrimaryKey(id)
}
