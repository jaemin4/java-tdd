package io.hhplus.tdd;


import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.model.entity.UserPoint;
import io.hhplus.tdd.model.result.RestResult;
import io.hhplus.tdd.service.front.UserPointFrontService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.sql.ResultSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

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


    @DisplayName("[UserPointFrontService]/getPointByIdTest : " +
            "amount 값이 없을때 exception이 정상적으로 던져지는지 | FAIL")
    @Test
    void chargeUserPointException()  {



    }
    @DisplayName("동시성 테스트 환경 제공")
    void concurrencyCommTest(long id, long amount,Integer threadCount,String methodName) throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    // 톰캣 멀티스레드 환경에서 동시에 호출
                    switch (methodName){
                        case "chargeUserPoint" -> userPointFrontService.chargeUserPoint(id,amount);
                    }
                } finally {
                    countDownLatch.countDown();
                }
            });
        }
        countDownLatch.await();

        RestResult updatedResult = userPointFrontService.getPointById(id);
        UserPoint updatedUserPoint = (UserPoint)updatedResult.getData().get("data");

        long updatedPoint = updatedUserPoint.point();
        long expectedPoint = threadCount * amount;

        assertEquals(expectedPoint,updatedPoint,"모든 충전이 정상 처리되었는지 확인");
    }






}
