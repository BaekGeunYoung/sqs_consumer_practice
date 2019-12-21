package synchronization

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    GlobalScope.massiveRun {
            counter++
    }

    println("Counter = $counter")
}