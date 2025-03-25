## ReentrantLock 개념 및 활용 

## 기본 구조

- **AQS(AbstractQueuedSynchronizer)**를 기반으로 구현된 락 메커니즘
- **FIFO 방식의 큐**를 사용한 스레드 대기 관리

## 주요 특징

- 재진입 가능한 락
    - **동일 스레드의 락 재획득** 가능
    - **획득한 만큼 해제** 필요
- 락 획득 정책
    - **공정 락**: 먼저 요청한 스레드 우선 처리
    - **비공정 락**: 임의 처리로 성능 최적화
- 세밀한 락 제어
    - **tryLock()**으로 타임아웃 설정
    - **lockInterruptibly()**로 인터럽트 응답

## 기능 요약

| **기능** | **설명** |
| --- | --- |
| 재진입성 | 동일 스레드의 중복 락 허용 |
| 락 제어 | 명시적 lock/unlock 메서드 제공 |
| 공정성 | 순차적 스레드 처리 지원 |
| 대기 관리 | 타임아웃과 인터럽트 기능 |
| 조건 처리 | 정교한 스레드 동기화 |

Java의 멀티스레딩 환경에서는 스레드 안전성 확보를 위해 **synchronized** 키워드나 **ReentrantLock**을 주로 활용

**ReentrantLock의 공정 락(Fair Lock)**을 사용하면, 락을 요청한 스레드들이 **FIFO(선입선출) 큐**에 저장되어 대기 순서대로 락을 획득하게 됨. 이로 인해 **동시성 제어**와 **실행 순서의 일관성**을 보장할 수 있다. 락이 해제되면, 가장 먼저 대기열에 들어온 스레드가 우선적으로 자원을 획득하여 작업을 수행하게 된다.

## ReentrantLock의 주요 단점

### 1. 공정 락의 FIFO 큐 관리 비용

- **큐 관리 오버헤드**: ReentrantLock(true)는 스레드들이 락을 얻기 위해 대기열 FIFO에 등록됨. 락을 해제할 때는 항상 가장 먼저 들어온 스레드를 선택해야 하므로, 큐를 순회하고 관리하는 비용이 발생.
- **락 처리 프로세스**: 락 획득 → 큐 삽입/락 해제 → 큐에서 poll() 후 contextSwitch 락 전달 절차가 필요
비공정 락은 락이 비었을 때 먼저 도착한 스레드가 아니라 운좋게 CPU를 먼저 잡은 스레드에게 락을 주기 때문에 이런 큐관리가 없어 더 효율적임

### 2. 컨텍스트 스위칭 비용

**스위칭 오버헤드**: 락이 해제될 때 다음 스레드에게 락을 넘기려면, 현재 실행중인 스레드를 중단하고 대기 중인 다른 스레드로 전환이 필요하게 됨.
이 전환은 **CPU 캐시를 FLUSH, 프로세서 레지스터 상태 저장/복원, 커널 모드 진입/탈출**이 필요하므로 비싼 연산이다.
공정 락은 항상 가장 오래 기다린 스레드를 꺼내야 하므로, 지금 실행중이 아닌 스레드로 전환될 확률이 높음

### 3. 메모리 일관성 비용과 캐시 무효화

- **캐시 동기화**: 여러 스레드가 공유하는 락 객체를 계속해서 접근하며 상태를 바꾸기 때문에 CPU 캐시 간 동기화가 필요함
- **캐시 라인 무효화**: 락 객체의 상태가 바뀔 때마다 **MESI 캐시 프로토콜**에 따라 캐시 라인이 Invalidated되고, 메모리 버스를 통해 다시 불러옴
- **성능 저하**: 특히 공정 락은 상태가 자주 바뀌고, 락 전달시 공유 메모리 접근이 많아져서 성능 저하가 발생

### 4. Thread Starvation과 Wake-up 비용

- **강제 대기**: 공정 락에서는 스레드가 **FIFO 큐**에 따라 wake-up되기 때문에 적절한 시점이 되기 전까지 무조건 대기해야 함
- **CPU 자원 소비**: 스레드를 sleep 상태로 유지하다가 다시 wake-up 시키는 작업도 상당한 CPU 자원을 소비함

### 정리

ReentrantLock은 강력한 동시성 제어 기능을 제공하지만, 무분별하게 사용하면 성능 저하의 문제가 발생할 수 있다. 따라서 꼭 필요한 경우에만 사용하는 것이 바람직하다. 이제는 ReentrantLock의 구현 부분을 자세히 살펴보도록 하자.

### ReentrantLock(true)를 활용한 동시성 제어

```
private final Map<Long, Lock> userLocks = new ConcurrentHashMap<>();

public RestResult chargeUserPoint(Long id, Long amount) {
    if (amount == null) {
        throw new UserPointRuntimeException("Validation error");
    }

    Lock lock = userLocks.computeIfAbsent(id, k -> new ReentrantLock(true));
    lock.lock();
    try {
        UserPoint resultUserPoint = userPointService.getPointById(id);
        if (resultUserPoint == null) {
            throw new UserPointRuntimeException("존재하지 않는 사용자입니다.");
        }

        long updatedPoint = resultUserPoint.point() + amount;
        UserPoint updatedUserPoint = userPointService.saveOrUpdateUserPoint(id, updatedPoint);

        PointHistory updatedPointHistory = pointHistoryService.insertHistory(id, amount, TransactionType.CHARGE);

        return new RestResult("200", "User Charge Success",
                Map.of("updatedUserPoint", updatedUserPoint, "updatedPointHistory", updatedPointHistory));
    } catch (Exception e) {
        throw new UserPointRuntimeException("충전 중 예외 발생: " + e.getMessage());
    } finally {
        lock.unlock();
    }
}
```

위 코드는 Validation이 검증되고 난후 해당 메서드에서 id(userId)로 getPointById 조회를 하는 시점에 락을 걸었다. 이 시점에 락을 거는 이유는 Update되기전에 조회되는 ~ 문제를 방지하기 위해서다. 

위 코드는 사용자 포인트 충전 시스템의 동시성을 제어하는 예시인데, 주요 특징을 설명하면:

- **ConcurrentHashMap을 사용한 락 관리**: 각 사용자 ID별로 독립적인 ReentrantLock을 관리하여 서로 다른 사용자의 트랜잭션이 서로 방해되지 않도록 함
- **공정 락(Fair Lock) 사용**: ReentrantLock(true)로 공정 락을 설정하여 먼저 요청한 스레드가 우선적으로 처리되도록 보장
- **안전한 락 해제**: finally 블록에서 락을 해제하여 예외 발생 시에도 반드시 락이 해제되도록 보장

이러한 구현을 통해 다음과 같은 문제들을 방지할 수 있습니다:

- 동시에 여러 스레드가 같은 사용자의 포인트를 수정하려 할 때 발생할 수 있는 경쟁 상태(Race Condition) 방지
- 포인트 조회와 업데이트 사이의 일관성 보장
- 트랜잭션의 원자성(Atomicity) 보장

### ReentrantLock(true)테스팅 코드

아래의 테스트 코드는 ReentrantLock의 동시성 제어 기능을 검증하기 위한 것이다. 여러 스레드가 동시에 포인트를 충전하거나 사용하는 상황에서 데이터의 정합성이 유지되는지 확인한다.

```
 @DisplayName("동시성 테스트 환경 제공")
    void concurrencyCommTest(long id, long amount,Integer threadCount,String methodName) throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    // 톰캣 멀티스레드 환경에서 동시에 호출
                    switch (methodName){
                        case "chargeUserPoint" -> {
                            userPointFrontService.chargeUserPoint(id,amount);
                        }
                        case "useUserPoint" -> {
                            userPointFrontService.useUserPoint(id,amount);
                        }

                    }
                } finally {
                    countDownLatch.countDown();
                }
            });
        }

        countDownLatch.await();

        //userPoint
        RestResult updatedResult = userPointFrontService.getPointById(id);
        UserPoint updatedUserPoint = (UserPoint)updatedResult.getData().get("data");

        //pointHistory
        RestResult updatedResult2 = userPointFrontService.RetrieveUserHistoryById(id);
        List<PointHistory> updatedPointHistory = (List<PointHistory>) updatedResult2.getData().get("data");

        long updatedPoint = updatedUserPoint.point();
        long expectedPoint = threadCount * amount;

        if(methodName.equals("useUserPoint")){
            expectedPoint = 5000L - (threadCount * amount);
        }

        long updatedSize = updatedPointHistory.size();
        long expectedSize = threadCount;

        //모든 history가 정상 저장되었는지 확인
        for(PointHistory pointHistory : updatedPointHistory){
            assertEquals(pointHistory.userId(),id);
            assertEquals(pointHistory.amount(),amount);
        }
        if(methodName.equals("chargeUserPoint")){
            assertEquals(expectedPoint,updatedPoint,"모든 충전이 정상 처리되었는지 확인");
        } else if (methodName.equals("useUserPoint")) {
            assertEquals(expectedPoint,updatedPoint,"모든 사용이 정상 처리되었는지 확인");
        }

        assertEquals(updatedSize,expectedSize,"모든 history가 정상 저장 되었는지 확인");

    }
```

이 테스트 코드의 주요 특징은 다음과 같다:

- **ExecutorService**를 사용해 지정된 수의 스레드를 동시에 실행한다.
    - ExecutorService는 자바의 동시성 프레임워크가 제공하는 인터페이스로**, 스레드 풀을 관리**하고 비동기 작업을 실행한다.
    - 이 테스트 코드에서는 **여러 스레드 풀을 생성하여 포인트 충전/사용 메서드에 동시 접근하는 용도**로 사용했다.
- **CountDownLatch**로 모든 스레드의 작업 완료를 보장한다.
    - CountDownLatch는 **여러 스레드가 다른 스레드들의 작업 완료를 대기할 수 있게 하는 동기화** **도구**다.
    - 초기화 시점에 지정된 카운트 값으로 시작한다.
    - **await()** 메서드를 호출한 스레드는 카운트가 0이 될 때까지 대기한다.
    - **countDown()** 메서드가 호출될 때마다 카운트가 1씩 감소한다.
    - 이 테스트에서는 CountDownLatch를 사용해 모든 **스레드의 작업이 완료될 때까지 기다린 후 최종 결과값을 검증**한다.
- 포인트 충전과 사용에 대한 동시성 테스트를 수행한다.
- 최종 결과값을 검증하여 동시성 제어의 성공 여부를 확인한다.
