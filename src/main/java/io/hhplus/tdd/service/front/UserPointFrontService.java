package io.hhplus.tdd.service.front;

import io.hhplus.tdd.constants.TransactionType;
import io.hhplus.tdd.exception.UserPointRuntimeException;
import io.hhplus.tdd.model.entity.PointHistory;
import io.hhplus.tdd.model.entity.UserPoint;
import io.hhplus.tdd.model.result.RestResult;
import io.hhplus.tdd.service.persist.PointHistoryService;
import io.hhplus.tdd.service.persist.UserPointService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class UserPointFrontService {

    private final UserPointService userPointService;
    private final PointHistoryService pointHistoryService;


    public RestResult getPointById(Long id) {
        UserPoint resultUserPoint = userPointService.getPointById(id);

    /*
    TODO - 1.checkPoint
        id validation 포함해서 사용자가 없으면 생성하는 구조이기 때문에 의미 없을지도?
    */
        if (resultUserPoint == null) {
            throw new UserPointRuntimeException("사용자를 찾을 수 없습니다.");
        }

        return new RestResult("200", "Retrieve success", Map.of("data", resultUserPoint));
    }

    public RestResult RetrieveUserHistoryById(Long userId) {
        return new RestResult("200",
                "Retrieve UserHistory Success",
                Map.of("data", pointHistoryService.selectAllByUserId(userId)));
    }

    public RestResult chargeUserPoint(Long id, Long amount) {
        if (amount == null) {
            throw new UserPointRuntimeException("Validation error");
        }
        UserPoint resultUserPoint = userPointService.getPointById(id);
        if (resultUserPoint == null) {
            throw new UserPointRuntimeException("존재하지 않는 사용자 입니다.");
        }

        long updatedPoint = resultUserPoint.point() + amount;
        UserPoint updatedUserPoint = userPointService.saveOrUpdateUserPoint(id, updatedPoint);

        PointHistory updatedPointHistory = pointHistoryService.insertHistory(id, amount, TransactionType.CHARGE);

        return new RestResult("200", "User Charge Success",
                Map.of("updatedUserPoint", updatedUserPoint, "updatedPointHistory", updatedPointHistory));
    }



}
