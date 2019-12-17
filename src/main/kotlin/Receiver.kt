import kotlinx.coroutines.runBlocking
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsAsyncClient

fun main() {
    runBlocking {
        val sqs = SqsAsyncClient.builder()
            .region(Region.AP_NORTHEAST_2)
            .build()

        val consumer = SqsConsumer(sqs)

        consumer.start()
    }
}