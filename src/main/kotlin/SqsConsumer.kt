import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.future.await
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.Message
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import java.lang.Thread.currentThread
import kotlin.coroutines.CoroutineContext


class SqsConsumer (private val sqs: SqsAsyncClient): CoroutineScope {
    private val supervisorJob = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + supervisorJob

    private val N_WORKERS = 100
    private val SQS_URL = "https://sqs.ap-northeast-2.amazonaws.com/182756308452/ticket_reservation_data_queue"

    fun start() = launch {
        val messageChannel = Channel<Message>()
        repeat(N_WORKERS) {
            launchWorker(messageChannel)
        }
        launchMsgReceiver(messageChannel)
    }

    //MsgReceiver : SQS로 주기적으로 polling하여 큐 메세지를 받아오고, channel을 통해 worker로 큐 메세지를 넘긴다.
    private fun CoroutineScope.launchMsgReceiver(channel: SendChannel<Message>) = launch {
        repeatUntilCancelled {
            val receiveRequest = ReceiveMessageRequest.builder()
                .queueUrl(SQS_URL)
                .waitTimeSeconds(20)
                .maxNumberOfMessages(10)
                .build()

            val messages = sqs.receiveMessage(receiveRequest).await().messages()
            println("${Thread.currentThread().name} Retrieved ${messages.size} messages")

            messages.forEach {
                channel.send(it)
            }
        }
    }

    //큐메세지를 polling하는 작업을 무한히 반복할 수 있도록 하는 coroutine.
    private suspend fun CoroutineScope.repeatUntilCancelled(block: suspend() -> Unit) {
        while(isActive) {
            try {
                block()
                yield()
            } catch (ex: CancellationException) {
                println("coroutine on ${currentThread().name} cancelled")
            } catch (ex: Exception) {
                println("${currentThread().name} failed with {$ex}. Retrying...")
                ex.printStackTrace()
            }
        }

        println("coroutine on ${currentThread().name} exiting")
    }

    //Worker : 큐 메세지를 받아 실제 작업을 처리.
    private fun CoroutineScope.launchWorker(channel: ReceiveChannel<Message>) = launch {
        repeatUntilCancelled {
            for (msg in channel) {
                try {
                    processMsg(msg)
                    deleteMessage(msg)
                } catch (ex: Exception) {
                    println("${Thread.currentThread().name} exception trying to process message ${msg.body()}")
                    ex.printStackTrace()
                    changeVisibility(msg)
                }
            }
        }
    }

    //큐 메세지를 처리하는 예시 코드
    private suspend fun processMsg(message: Message) {
        println("${Thread.currentThread().name} Started processing message: ${message.body()}")
        delay((1000L..2000L).random())
        println("${Thread.currentThread().name} Finished processing of message: ${message.body()}")
    }

    private suspend fun deleteMessage(message: Message) {
        sqs.deleteMessage { req ->
            req.queueUrl(SQS_URL)
            req.receiptHandle(message.receiptHandle())
        }.await()
        println("${Thread.currentThread().name} Message deleted: ${message.body()}")
    }

    private suspend fun changeVisibility(message: Message) {
        sqs.changeMessageVisibility { req ->
            req.queueUrl(SQS_URL)
            req.receiptHandle(message.receiptHandle())
            req.visibilityTimeout(10)
        }.await()
        println("${Thread.currentThread().name} Changed visibility of message: ${message.body()}")
    }
}