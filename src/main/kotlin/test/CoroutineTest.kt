package test

import kotlinx.coroutines.*



fun main() {
    runBlocking {
        repeat(100) {
            launch {
                repeatUntilCancelled { println(it) }
            }
        }
    }
}

private fun CoroutineScope.launchWorker(number: Int) = launch {
    repeatUntilCancelled {
        println(number)
    }
}

private suspend fun CoroutineScope.repeatUntilCancelled(suspendFunc: suspend() -> Unit) {
    while(isActive) {
        try {
            println("${Thread.currentThread().name} running")
            suspendFunc()
            yield()
        } catch (ex: Exception) {
            println("${Thread.currentThread().name} failed with {$ex}. Retrying...")
            ex.printStackTrace()
        }
    }

    println("coroutine on ${Thread.currentThread().name} exiting")
}