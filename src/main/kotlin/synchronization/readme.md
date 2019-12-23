## 멀티스레드 환경의 문제

![race condition.jpg](https://images.velog.io/post-images/dvmflstm/1e550400-23f1-11ea-b434-6932261a0e25/race-condition.jpg)

(↑ race condition을 설명하기에 너무나 적절한 사진.)

멀티 스레드는 어플리케이션의 퍼포먼스 측면에서 싱글 스레드에 비해 큰 이득을 가져다주지만, race condition을 적절히 제어하지 않을 경우 데이터의 손실이 발생할 수 있다. race condition이란 여러 개의 스레드가 하나의 공유 가능하고 변경 가능한 자원에 접근하게 되는 상황을 뜻한다. 데이터에 손실이 발생하는 것은 절대 있어서는 안 될 일이므로, synchronization을 통해 race condition을 적절히 제어하는 것은 멀티스레드 환경의 개발을 할 때에 매우 중요하다. 아래는 적절한 synchronization 없이 race condition이 발생하는 코드와 이를 실행했을 때 나오는 결과이다.
`WithNoControl.kt`
```java
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

var counter = 0
fun main() = runBlocking {
    GlobalScope.massiveRun {
            counter++
    }

    println("Counter = $counter")
}
```
```
Completed 100000 actions in 136 ms
Counter = 95029

Process finished with exit code 0
```
프로그램 실행시간은 136ms로 매우 빠른 실행속도를 보였지만, 100000이 출력되길 기대하는데 실제로는 이상한 값이 출력되었다. 다수의 스레드가 하나의 공유 가능하고 변경 가능한 자원인 counter에 접근하면서 발생한 오류이다. 이를 제어하기 위한 방법으로 크게 3가지가 있다.

### 1. Single Thread
가장 단순한 해결책은 스레드를 하나만 이용하는 것이다. 싱글스레드를 사용하도록 코루틴의 context를 지정해주면, 데이터의 결함 없이 결과를 도출할 수 있지만, 멀티스레드 환경에 비해 매우 느리다.

`SingleThread.kt`
```java
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
```
```
Completed 100000 actions in 1553 ms
Counter = 100000

Process finished with exit code 0
```

### 2. Mutual Exclusion (Mutex)
mutual exclusion은 공유자원에 변경이 일어나는 순간에 적절한 block을 통해 race condition의 발생을 막는 동기화 제어 기법이다. 코루틴에서 제공하는 mutex의 함수들을 이용해 간단하게 mutex 방식의 동기화 제어가 가능하다.
`WithLock.kt`
```java
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

```
```
Completed 100000 actions in 789 ms
Counter = 100000

Process finished with exit code 0

```
싱글스레드 방식에 비해 약 2배 정도 뛰어난 성능을 보이는 것을 확인할 수 있다.

### 3. Actor
동기화 이슈가 있는 자원을 actor 내에서 관리하도록 하며, actor 클래스의 멤버변수로 정의되어 있는 Channel을 통해 자원으로의 접근이 가능하다. channel은 FIFO 방식의 queue 형태로 구현되어 있기 때문에 sequential한 접근을 보장해 동기화 이슈를 해결한다.

`Actor.kt`
```java
sealed class CounterMsg
object IncCounter : CounterMsg()
class GetCounter(val response: CompletableDeferred<Int>) : CounterMsg()

@ObsoleteCoroutinesApi
fun CoroutineScope.counterActor() = actor<CounterMsg> {
    var counter = 0 // actor state
    for (msg in channel) {
        when (msg) {
            is IncCounter -> counter++
            is GetCounter -> msg.response.complete(counter)
        }
    }
}

@ObsoleteCoroutinesApi
fun main() {
    runBlocking {
        val counter = counterActor()
        GlobalScope.massiveRun {
            counter.send(IncCounter)
        }

        val response = CompletableDeferred<Int>()
        counter.send(GetCounter(response))
        println("Counter = ${response.await()}")
        counter.close() // shutdown the actor
    }
}
```
```
Completed 100000 actions in 1202 ms
Counter = 100000

Process finished with exit code 0
```
자원으로의 접근이 하나의 큐로만 관리되고 있어서 그런지 성능이 그렇게 좋아보이지는 않는다.

## 결론
멀티 스레드 환경(정확히는 코틀린 코루틴 환경)에서의 동기화 제어 방법에 대해 알아보았다. 현재 진행하고 있는 12월 프로젝트에서 동기화 제어가 중요 포인트가 될 것 같은데, 위 방법들 중 적절한 것을 골라 적용하면 될 것 같다.
