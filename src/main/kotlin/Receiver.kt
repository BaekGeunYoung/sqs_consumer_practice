import kotlinx.coroutines.runBlocking
import kotliquery.HikariCP
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsAsyncClient

fun main() {
    runBlocking {
        HikariCP.default("jdbc:mysql://localhost:3306/kotliquery?serverTimezone=UTC", "root", "dkdltm123")

        val sqs = SqsAsyncClient.builder()
            .region(Region.AP_NORTHEAST_2)
            .build()

        val consumer = SqsConsumer(sqs)
        consumer.start()
    }
}