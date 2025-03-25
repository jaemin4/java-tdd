package io.hhplus.tdd;


import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.model.entity.UserPoint;
import io.hhplus.tdd.model.result.RestResult;
import io.hhplus.tdd.service.front.UserPointFrontService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class UserPointFrontServiceTest {

    @Autowired
    UserPointFrontService userPointFrontService;

    @Autowired
    UserPointTable userPointTable;

    @BeforeEach
    void init(){
        userPointTable.insertOrUpdate(1L,5000L);
    }

    @DisplayName(value = "[UserPointFrontService]/getPointByIdTest : " +
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




}
