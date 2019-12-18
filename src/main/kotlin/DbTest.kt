import kotliquery.*;
import java.sql.DriverManager
import java.sql.SQLException
import java.util.*

data class Member(
    val id: Long? = null,
    val name: String,
    val age: Int
)

fun main() {
    connect()

    HikariCP.default("jdbc:mysql://localhost:3306/kotliquery?serverTimezone=UTC", "root", "dkdltm123")

    using(sessionOf(HikariCP.dataSource()))
    {
//        it.run(queryOf("""
//            create table members (
//                id serial not null primary key,
//                name varchar(64),
//                age int
//            )
//        """).asExecute)

        val insertQuery = "insert into members (name, age) values (?, ?)"

        it.run(queryOf(insertQuery, "alice", 20).asUpdate)
        it.run(queryOf(insertQuery, "bob", 25).asUpdate)

        val allRowsQuery =
            queryOf("select * from members")
                .map { row ->
                    Member(
                        id = row.long("id"),
                        name = row.string("name"),
                        age = row.int("age")
                    )
                }.asList
        val rows: List<Member> = it.run(allRowsQuery)

        for(row in rows) {
            println(row)
        }
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