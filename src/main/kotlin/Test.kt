import kotlinx.coroutines.*

fun main() {
    testFun { println("test") }
}

fun testFun(param: () -> Unit) {
    param()
    println("hi")
}