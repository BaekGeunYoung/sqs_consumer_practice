package synchronization

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.system.measureTimeMillis

suspend fun CoroutineScope.massiveRun(action: suspend () -> Unit)
{
    val n = 100
    val k = 1000
    val time = measureTimeMillis {
        val jobs = List(n) {
            launch {
                repeat(k) {
                    action()
                }
            }
        }

        jobs.forEach { it.join() }
    }
    println("Completed ${n * k} actions in $time ms")
}

val mutex = Mutex();
var counter = 0

fun main() = runBlocking {
    GlobalScope.massiveRun {
        mutex.withLock {
            counter++
        }
    }

    println("Counter = $counter")
}
