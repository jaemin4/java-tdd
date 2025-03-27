package io.hhplus.tdd;


import io.hhplus.tdd.constants.TransactionType;
import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.exception.UserPointRuntimeException;
import io.hhplus.tdd.model.entity.PointHistory;
import io.hhplus.tdd.model.entity.UserPoint;
import io.hhplus.tdd.model.result.RestResult;
import io.hhplus.tdd.service.front.UserPointFrontService;
import io.hhplus.tdd.service.persist.PointHistoryService;
import io.hhplus.tdd.service.persist.UserPointService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class UserPointFrontServiceTest {

    @Autowired
    UserPointFrontService userPointFrontService;

    @Autowired
    UserPointService userPointService;

    @Autowired
    PointHistoryService pointHistoryService;

    @Autowired
    UserPointTable userPointTable;

    @Autowired
    PointHistoryTable pointHistoryTable;

    @BeforeEach
    void init(){
        userPointTable.insertOrUpdate(1L,5000L);
    }

    @DisplayName("특정 유저의 userPoint정보가 정상적으로 출력된다.")
    @Test
    void getPointByIdTest(){
        //given
        long testId = 1L;

        //when
        RestResult result = userPointFrontService.getPointById(testId);
        UserPoint resultData = (UserPoint)result.getData().get("data");

        //then (Rest Result값이 예상했던대로 정상 반환 되는지 테스트)
        assertEquals("200",result.getStatus());
        assertEquals("Retrieve success",result.getMessage());
        assertEquals(1L,resultData.id());
        assertEquals(5000L,resultData.point());
    }

    @Test
    @DisplayName("[UserPointFrontService] chargeUserPoint - 동시성 충전 테스트 FAIL")
    public void chargeUserPointConcurrencyTestFail() throws InterruptedException{
        concurrencyCommTest(41L,100L,10,"TestCharge");
    }

    @Test
    @DisplayName("[UserPointFrontService] useUserPoint - 동시성 사용 테스트 FAIL")
    public void useUserPointConcurrencyTestFail() throws InterruptedException{
        concurrencyCommTest(1L,100L,5,"TestUse");
    }



    @DisplayName("동시에 5개의 스레드가 chargeUserPoint 메서드에 접근했을때 정상적으로 충전이 완료되는지")
    @Test
    void chargeUserPoint() throws InterruptedException {
        concurrencyCommTest(2L,100L,5,"chargeUserPoint");
    }

    @DisplayName("충전 금액이 0일때 예외가 발생하는지")
    @Test
    void chargeUserPointAmountZero(){
         assertThrows(UserPointRuntimeException.class, () ->{
             userPointFrontService.chargeUserPoint(3L,0L);
        }) ;
    }

    @DisplayName("충전 금액이 0 이하 일때 예외가 발생하는지")
    @Test
    void chargeUserPointAmountMinus(){
        assertThrows(UserPointRuntimeException.class, () ->{
            userPointFrontService.chargeUserPoint(3L,-99L);
        }) ;
    }

    @DisplayName("충전 금액이 0일때 예외가 발생하는지")
    @Test
    void useUserPointAmountZero(){
        assertThrows(UserPointRuntimeException.class, () ->{
            userPointFrontService.useUserPoint(3L,0L);
        }) ;
    }

    @DisplayName("충전 금액이 0 이하 일때 예외가 발생하는지")
    @Test
    void useUserPointAmountMinus(){
        assertThrows(UserPointRuntimeException.class, () ->{
            userPointFrontService.useUserPoint(3L,-99L);
        }) ;
    }

    @DisplayName("잔액이 부족할때 예외가 정상적으로 던져진다.")
    @Test
    void useUserPointException() {
        assertThrows(UserPointRuntimeException.class, () -> {
            userPointFrontService.useUserPoint(1L,6000L);
        });

    }
    //useUserPoint 메서드일 경우 @BeforeEach UserPoint(1L,5000L) 테스팅 사용 고정
    @DisplayName("동시에 10개의 스레드가 chargeUserPoint 메서드에 접근했을때 정상적으로 사용이 완료되는지")
    @Test
    void useUserPoint() throws InterruptedException {
        concurrencyCommTest(1L,100L,10,"useUserPoint");
    }

    @DisplayName("chargeUserPoint 메서드에서 amount가 null값일 때 예외가 던져진다.")
    @Test
    void chargeUserPointException()  {
        Long amount = null;
        assertThrows(UserPointRuntimeException.class, () -> {
            userPointFrontService.chargeUserPoint(3L, amount);
        });
    }

    @DisplayName("useUserPoint 메서드에서 amount가 null값일 때 예외가 던져진다.")
    @Test
    void useUserPointNullException(){
        Long amount = null;
        assertThrows(UserPointRuntimeException.class, () -> {
            userPointFrontService.useUserPoint(4L,amount);
        });

    }

    //useUserPoint 메서드일 경우 @BeforeEach UserPoint(1L,5000L) 테스팅 사용 고정
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
                        case "TestCharge" -> TestCharge(id,amount);
                        case "TestUse" -> TestUse(id,amount);

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

        if(methodName.equals("useUserPoint") || methodName.equals("TestUse")){
            expectedPoint = 5000L - (threadCount * amount);
        }


        long updatedSize = updatedPointHistory.size();
        long expectedSize = threadCount;


        //모든 history가 정상 저장되었는지 확인
        for(PointHistory pointHistory : updatedPointHistory){
            assertEquals(pointHistory.userId(),id);
            assertEquals(pointHistory.amount(),amount);
        }
        switch (methodName) {
            case "chargeUserPoint" -> assertEquals(expectedPoint, updatedPoint, "모든 충전이 정상 처리되었는지 확인");
            case "useUserPoint" -> assertEquals(expectedPoint, updatedPoint, "모든 사용이 정상 처리되었는지 확인");
            case "TestCharge" -> assertNotEquals(expectedPoint, updatedPoint, "충전 동시성 실패 케이스");
            case "TestUse" -> assertNotEquals(expectedPoint, updatedPoint, "사용 동시성 실패 케이스");
        }

        if(methodName.equals("TestUse") || methodName.equals("TestCharge")){
            assertNotEquals(updatedSize,expectedSize,"동시성 실패 케이스");
        } else{
            assertEquals(updatedSize,expectedSize,"모든 history가 정상 저장 되었는지 확인");

        }

    }



    @DisplayName("[UserPointFrontService] chargeUserPoint - 동시성 제어 실패 케이스")
    public RestResult TestCharge(Long id, Long amount) {
        if (amount == null) {
            throw new UserPointRuntimeException("Validation error");
        }

        UserPoint resultUserPoint = userPointService.getPointById(id);
        long updatedPoint = resultUserPoint.point() + amount;
        UserPoint updatedUserPoint = userPointService.saveOrUpdateUserPoint(id, updatedPoint);

        PointHistory updatedPointHistory = pointHistoryService.insertHistory(id, amount, TransactionType.CHARGE);

        return new RestResult("200", "User Charge Success",
                Map.of("updatedUserPoint", updatedUserPoint, "updatedPointHistory", updatedPointHistory));
    }

    @DisplayName("[UserPointFrontService] useUserPoint - 동시성 제어 실패 케이스")
    public RestResult TestUse(Long id, Long amount) {
        if (amount == null) {
            throw new UserPointRuntimeException("Validation error");
        }

        UserPoint resultUserPoint = userPointService.getPointById(id);
        long updatedPoint = resultUserPoint.point() - amount;
        UserPoint updatedUserPoint = userPointService.saveOrUpdateUserPoint(id, updatedPoint);

        PointHistory updatedPointHistory = pointHistoryService.insertHistory(id, amount, TransactionType.USE);

        return new RestResult("200", "User Use Success",
                Map.of("updatedUserPoint", updatedUserPoint, "updatedPointHistory", updatedPointHistory));

    }


}
