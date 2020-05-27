import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import software.amazon.awssdk.services.sqs.model.Message

fun main() = runBlocking {
    launch {
        println("1")
    }

    coroutineScope {
        launch {
            println("2")
        }

        println("3")
    }

    coroutineScope {
        launch {
            println("4")
        }

        println("5")
    }

    launch {
        println("6")
    }

    for (i in 7..100) {
        println(i.toString())
    }

    println("101")
}

fun test() = GlobalScope.launch {
    launch {
        delay(200L)
        println("1")
    }

    launch {
        launch {
            delay(500L)
            println("2")
        }

        delay(100L)
        println("3")
    }

//    println("4")
}

//큐메세지를 polling하는 작업을 무한히 반복할 수 있도록 하는 coroutine.
private suspend fun CoroutineScope.repeatUntilCancelled(suspendFunc: suspend() -> Unit) {
    while(isActive) {
        try {
            suspendFunc()
            yield()
        } catch (ex: CancellationException) {
            println("coroutine on ${Thread.currentThread().name} cancelled")
        } catch (ex: Exception) {
            println("${Thread.currentThread().name} failed with {$ex}. Retrying...")
            ex.printStackTrace()
        }
    }
}

//Worker : 큐 메세지를 받아 실제 작업을 처리.
private fun CoroutineScope.launchWorker(channel: ReceiveChannel<Message>) = launch {
    repeatUntilCancelled {
        for (i in (1..100)) {
            println(i)
        }
    }
}

class Test {
    val value = 3
    fun test() {
        println("test")
    }
}

fun Test.otherTest() {
    value
}