import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*

@ExperimentalCoroutinesApi
fun main() = runBlocking {
    val producer = produceNumbers()
    repeat(5) { launchProcessor(it, producer) }
    delay(950)
//    producer.cancel() // cancel producer coroutine and thus kill them all
}

@ExperimentalCoroutinesApi
fun CoroutineScope.produceNumbers() = produce {
    var x = 1 // start from 1
    while (true) {
        send(x++) // produce next
        delay(100) // wait 0.1s
    }
}

fun CoroutineScope.launchProcessor(id: Int, channel: ReceiveChannel<Int>) = launch {
    for (msg in channel) {
        println("Processor #$id received $msg")
    }
}