package db

import kotliquery.*;
import java.sql.DriverManager
import java.util.*

data class Member(
    val id: Long? = null,
    val name: String,
    val age: Int
)

fun main() {
//    db.connect()

    HikariCP.default("jdbc:mysql://localhost:3306/kotliquery?serverTimezone=UTC", "root", "dkdltm123")

    using(sessionOf(HikariCP.dataSource()))
    {
        it.run(queryOf("""
            create table messages (
                id serial not null primary key,
                message varchar(64),
                created_at timestamp not null
            )
        """).asExecute)
//
//        val insertQuery = "insert into members (name, age) values (?, ?)"
//
//        it.run(queryOf(insertQuery, "alice", 20).asUpdate)
//        it.run(queryOf(insertQuery, "bob", 25).asUpdate)
//
//        val allRowsQuery =
//            queryOf("select * from members")
//                .map { row ->
//                    db.Member(
//                        id = row.long("id"),
//                        name = row.string("name"),
//                        age = row.int("age")
//                    )
//                }.asList
//        val rows: List<db.Member> = it.run(allRowsQuery)
//
//        for(row in rows) {
//            println(row)
//        }
    }
}

fun connect() {
    val connectionProps = Properties()
    connectionProps["user"] = "root"
    connectionProps["password"] = "dkdltm123"
    connectionProps["serverTimezone"] = "UTC"
    try {
        Class.forName("com.mysql.cj.jdbc.Driver").newInstance()
        DriverManager.getConnection("jdbc:mysql://localhost:3306/kotliquery", connectionProps)
    } catch (ex: Exception) {
        ex.printStackTrace()
    }
}