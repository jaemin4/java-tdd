package io.hhplus.tdd;



import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.exception.UserPointRuntimeException;
import io.hhplus.tdd.service.front.UserPointFrontService;
import io.hhplus.tdd.service.persist.UserPointService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
public class UnitTest {
    @Autowired
    UserPointFrontService userPointFrontService;

    @Autowired
    UserPointTable userPointTable;
    @DisplayName("충전 금액이 0일때 예외가 발생하는지")
    @Test
    void chargeUserPointAmountZero(){
        assertThrows(UserPointRuntimeException.class, () ->{
            userPointFrontService.chargeUserPoint(3L,0L);
        }) ;
    }

    @DisplayName("충전 금액이 0 미만 일때 예외가 발생하는지")
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

    @DisplayName("충전 금액이 0 미만 일때 예외가 발생하는지")
    @Test
    void useUserPointAmountMinus(){
        assertThrows(UserPointRuntimeException.class, () ->{
            userPointFrontService.useUserPoint(3L,-99L);
        }) ;
    }

    @DisplayName("잔액이 부족할때 예외가 정상적으로 던져진다.")
    @Test
    void useUserPointException() {
        userPointTable.insertOrUpdate(1L,5000L);

        assertThrows(UserPointRuntimeException.class, () -> {
            userPointFrontService.useUserPoint(1L,6000L);
        });

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
}
