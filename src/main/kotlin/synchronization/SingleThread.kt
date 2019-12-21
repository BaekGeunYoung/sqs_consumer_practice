package synchronization

import kotlinx.coroutines.*

@ObsoleteCoroutinesApi
val counterContext = newSingleThreadContext("CounterContext")

@ObsoleteCoroutinesApi
fun main() {
    runBlocking {
        GlobalScope.massiveRun {
            withContext(counterContext) {
                counter++
            }
        }
        println("Counter = $counter")
    }
}