# sqs_consumer_practice
SQS 큐메세지를 받아 작업을 처리하는 sqs consumer를 작성해보는 연습 코드입니다.

## 서론
12월에는 '멀티스레드 환경에서 다수의 요청을 효율적으로 처리하는 WAS + Background worker 아키텍쳐 구현하기' 프로젝트를 계획했었다. 이 아키텍쳐의 핵심은 뒷단에서 멀티스레드로 원하는 작업을 효율적으로 처리하는 Background worker 부분이었는데, 이 부분을 구현하기 위해 학습한 것들과 실습 내용을 공유하려 한다.

## 실습 내용
실습에서 구현한 코드의 전체적인 구조와 각 요소에 대한 설명은 아래와 같다.

![image.png](https://images.velog.io/post-images/dvmflstm/52100a10-23bf-11ea-9390-258c47fdc7d5/image.png)

- #### Queue message receiver
	+ sqs로부터 큐메세지를 받아와서 이를 channel로 넘겨줌.
- #### Channel
	+ message receiver와 worker가 큐메세지를 주고 받기 위한 layer.
- #### Worker
	+ message receiver가 넘겨주는 큐메세지를 channel로부터 전달받아 실제로 작업을 수행하는 layer. 멀티스레드 환경(정확히는 coroutine)이므로 여러 개의 worker가 동시에 돌아감.

### SQS Provider

원래는 WAS 단에서 클라이언트로부터 request를 받아 consumer 단으로 큐 메세지를 뿌려주어야 하는데, 본 실습에서는 클라이언트와 WAS가 마련되어있지 않으므로 간단한 큐메세지 생성기를 작성했다.
`Sender.kt`
```kotlin
fun main() {
    runBlocking {
        val sqs = SqsClient.builder()
            .region(Region.AP_NORTHEAST_2)
            .build()

        val SQS_URL = "https://sqs.ap-northeast-2.amazonaws.com/182756308452/ticket_reservation_data_queue"

        var id = 0

        while (true) {
            id++
            val sendMsgRequest = SendMessageRequest.builder()
                .queueUrl(SQS_URL)
                .messageBody("hello world $id")
                .build()

            sqs.sendMessage(sendMsgRequest)
            println("Message sent with id: $id")
            delay(1000L)
        }
    }
}
```
이 Sender는 1초에 한번씩 queue message를 생성해 지정해준 엔드포인트로 해당 메세지를 날린다.
```
Message sent with id: 1
Message sent with id: 2
Message sent with id: 3
Message sent with id: 4
Message sent with id: 5
Message sent with id: 6
Message sent with id: 7
...
```
코드를 실행시켜보면 위와 같이 1초마다 메세지를 보내고 있음을 알 수 있고, AWS에 접속해서 큐메세지가 잘 전송됨을 확인할 수 있다.

![image.png](https://images.velog.io/post-images/dvmflstm/46ad7fd0-23c0-11ea-97cb-015e468e9011/image.png)

### Message Receiver
```kotlin
    //MsgReceiver : SQS로 주기적으로 polling하여 큐 메세지를 받아오고, channel을 통해 worker로 큐 메세지를 넘긴다.
    private fun CoroutineScope.launchMsgReceiver(channel: SendChannel<Message>) = launch {
        repeatUntilCancelled {
            val receiveRequest = ReceiveMessageRequest.builder()
                .queueUrl(SQS_URL)
                .waitTimeSeconds(20)
                .maxNumberOfMessages(10)
                .build()

            val messages = sqs.receiveMessage(receiveRequest).await().messages()
            println("${currentThread().name} Retrieved ${messages.size} messages")

            messages.forEach {
                channel.send(it)
            }
        }
    }
```
주기적으로 해당 엔드포인트로부터 큐 메세지를 받아와 channel에다 넘겨준다. repeatUntilCancelled는 원하는 작업을 해당 coroutine이 취소되기 전까지 무한히 반복할 수 있도록 해주는 custom coroutine이다.

### Channel
channel의 경우 추가적으로 무언가 구현할 필요는 없고, 동일한 Channel 객체를 MsgReceiver에는 SendChannel의 형태로, Worker에는 ReceiveChannel의 형태로 넘겨주면된다.
```kotlin
private fun CoroutineScope.launchMsgReceiver(channel: SendChannel<Message>);
private fun CoroutineScope.launchWorker(channel: ReceiveChannel<Message>);
```

### Worker
```kotlin
    //Worker : 큐 메세지를 받아 실제 작업을 처리.
    private fun CoroutineScope.launchWorker(channel: ReceiveChannel<Message>) = launch {
        repeatUntilCancelled {
            for (msg in channel) {
                try {
                    processMsg(msg)
                    deleteMessage(msg)
                } catch (ex: Exception) {
                    println("${currentThread().name} exception trying to process message ${msg.body()}")
                    ex.printStackTrace()
                    changeVisibility(msg)
                }
            }
        }
    }
```
channel에 담겨져 오는 큐메세지를 꺼내 적절히 처리를 해주고, 정상적으로 처리가 끝난 메세지는 삭제한다. SQS는 기본적으로 분산 환경에서 돌아가는 서비스이기 때문에 처리가 끝난 메세지를 자동으로 지워주지 않는다. 그래서 정상적으로 처리가 끝난 경우 다른 worker에서 이 메세지를 다시 처리하는 일이 없도록 메세지를 삭제해줘야 한다. 만약 메세지를 정상적으로 처리하지 못했다면, 큐의 visibility를 변경해 잠시동안 다른 워커에서 접근할 수 없는 상태로 만들고, visibility timeout이 끝나면 그때 다시 다른 워커에서 처리를 시도할 수 있도록 해준다. visibility timeout에 관한 자세한 설명은 https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-visibility-timeout.html
를 참고하기 바란다.
실제로 큐메세지를 처리하는 코드는 아래와 같다.
```kotlin
    //큐 메세지를 처리하는 예시 코드
    private suspend fun processMsg(message: Message) {
        println("${currentThread().name} Started processing message: ${message.body()}")

        using(sessionOf(HikariCP.dataSource()))
        {
            val insertQuery = "insert into messages (message, created_at) values (?, ?)"
            it.run(queryOf(insertQuery, message.body(), Date()).asUpdate)
        }

        println("${currentThread().name} Finished processing of message: ${message.body()}")
    }
```
실습 단계이므로 여기서는 단순히 해당 메세지를 DB에 저장하도록 해주었다.
결과적으로 코드를 실행해보면 아래와 같은 출력을 확인할 수 있다.

![image.png](https://images.velog.io/post-images/dvmflstm/9205c260-23c2-11ea-a50b-95f7993a19a1/image.png)

다수의 coroutine에서 동시다발적으로 작업을 수행하고 있음을 확인할 수 있고, 큐에 더 이상 메세지가 남아 있지 않을 때는 20초에 한번씩 polling을 시도한다. AWS console을 통해서도 모니터링이 잘 됨을 확인할 수 있다.

![image.png](https://images.velog.io/post-images/dvmflstm/ded1a410-23c2-11ea-8a1d-cf2562fb4be8/image.png)

## 결론
이번 실습을 통해 AWS SQS + Kotlin + Coroutine을 이용한 SQS consumer를 구현해 볼 수 있었고, 원래 계획했던 12월 프로젝트를 잘 진행할 수 있을 것 같다. 실습에서는 그런 상황이 없었지만, 다수의 coroutine이 동일한 자원에 접근해 race condition이 발생하는 경우를 어떻게 처리할 수 있을 지에 대해 좀 더 조사해봐야겠다.
