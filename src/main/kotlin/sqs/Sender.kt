package sqs

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.SendMessageRequest

fun main() {
    runBlocking {
        val sqs = SqsClient.builder()
            .region(Region.AP_NORTHEAST_2)
            .build()

        val SQS_URL = "https://sqs.ap-northeast-2.amazonaws.com/182756308452/ticket_reservation_data_queue"

        var id = 0

        while (true) {
            id++
            val sendMsgRequest = SendMessageRequest.builder()
                .queueUrl(SQS_URL)
                .messageBody("hello world $id")
                .build()

            sqs.sendMessage(sendMsgRequest)
            println("Message sent with id: $id")
            delay(1000L)
        }
    }
}