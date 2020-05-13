import kotlinx.coroutines.*

fun main() {
    test()
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