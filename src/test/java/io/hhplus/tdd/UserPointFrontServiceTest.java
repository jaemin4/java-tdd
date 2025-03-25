package io.hhplus.tdd;


import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.exception.UserPointRuntimeException;
import io.hhplus.tdd.model.entity.PointHistory;
import io.hhplus.tdd.model.entity.UserPoint;
import io.hhplus.tdd.model.result.RestResult;
import io.hhplus.tdd.service.front.UserPointFrontService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
public class UserPointFrontServiceTest {

    @Autowired
    UserPointFrontService userPointFrontService;

    @Autowired
    UserPointTable userPointTable;

    @Autowired
    PointHistoryTable pointHistoryTable;

    @BeforeEach
    void init(){
        userPointTable.insertOrUpdate(1L,5000L);
    }

    @DisplayName("[UserPointFrontService]/getPointByIdTest : " +
            "특정 유저의 포인트 조회(History) 정상적으로 출력되는지 | SUCCESS")
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


    @DisplayName("[UserPointFrontService]/chargeUserPoint : " +
            "특정 유저의 포인트 충전 정상적으로 적용되는지 | SUCCESS")
    @Test
    void chargeUserPoint() throws InterruptedException {
        concurrencyCommTest(2L,100L,3,"chargeUserPoint");
    }


    //useUserPoint 메서드일 경우 @BeforeEach UserPoint(1L,5000L) 테스팅 사용 고정
    @DisplayName("[UserPointFrontService]/useUserPoint : " +
            "특정 유저의 포인트 사용 정상적으로 적용되는지 | SUCCESS")
    @Test
    void useUserPoint() throws InterruptedException {
        concurrencyCommTest(1L,100L,20,"useUserPoint");
    }



    @DisplayName("[UserPointFrontService]/getPointByIdTest : " +
            "amount 값이 없을때 exception이 정상적으로 던져지는지 | FAIL")
    @Test
    void chargeUserPointException()  {
        Long amount = null;
        assertThrows(UserPointRuntimeException.class, () -> {
            userPointFrontService.chargeUserPoint(3L, amount);
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






}
