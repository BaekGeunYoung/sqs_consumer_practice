import kotlinx.coroutines.*

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