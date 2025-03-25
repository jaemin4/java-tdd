package io.hhplus.tdd.service.front;

import io.hhplus.tdd.exception.UserPointRuntimeException;
import io.hhplus.tdd.model.entity.UserPoint;
import io.hhplus.tdd.model.result.RestResult;
import io.hhplus.tdd.service.persist.PointHistoryService;
import io.hhplus.tdd.service.persist.UserPointService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Map;

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



}
