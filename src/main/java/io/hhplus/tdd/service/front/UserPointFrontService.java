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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class UserPointFrontService {

    private final UserPointService userPointService;
    private final PointHistoryService pointHistoryService;
    private final Map<Long, Lock> userLocks = new ConcurrentHashMap<>();


    public RestResult getPointById(Long id) {
        UserPoint resultUserPoint = userPointService.getPointById(id);
        return new RestResult("200", "Retrieve success", Map.of("data", resultUserPoint));
    }

    public RestResult RetrieveUserHistoryById(Long userId) {
        return new RestResult("200",
                "Retrieve UserHistory Success",
                Map.of("data", pointHistoryService.selectAllByUserId(userId)));
    }

    public RestResult chargeUserPoint(Long id, Long amount) {
        if (amount == null || amount <= 0) {
            throw new UserPointRuntimeException("충전금액이 올바르지 않습니다.");
        }


        Lock lock = userLocks.computeIfAbsent(id, k -> new ReentrantLock(true));
        boolean isLocked = false;

        try{
            isLocked = lock.tryLock(5, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new UserPointRuntimeException("락 획득 실패: 사용중 예외발생");
            }
            UserPoint resultUserPoint = userPointService.getPointById(id);

            long updatedPoint = resultUserPoint.point() + amount;
            UserPoint updatedUserPoint = userPointService.saveOrUpdateUserPoint(id, updatedPoint);

            PointHistory updatedPointHistory = pointHistoryService.insertHistory(id, amount, TransactionType.CHARGE);

            return new RestResult("200", "User Charge Success",
                    Map.of("updatedUserPoint", updatedUserPoint, "updatedPointHistory", updatedPointHistory));
        }catch (Exception e){
            throw new UserPointRuntimeException("충전 중 예외 발생: " + e.getMessage());
        }finally {
            lock.unlock();
        }

    }

    public RestResult useUserPoint(Long id, Long amount) {
        if (amount == null || amount <= 0) {
            throw new UserPointRuntimeException("사용금액이 올바르지 않습니다.");
        }

        Lock lock = userLocks.computeIfAbsent(id, k -> new ReentrantLock(true));
        boolean isLocked = false;
        try{
            isLocked = lock.tryLock(5, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new UserPointRuntimeException("락 획득 실패: 충전중 예외발생");
            }
            UserPoint resultUserPoint = userPointService.getPointById(id);

            if (resultUserPoint.point() < amount) {
                throw new UserPointRuntimeException("잔액이 부족합니다.");
            }

            long updatedPoint = resultUserPoint.point() - amount;
            UserPoint updatedUserPoint = userPointService.saveOrUpdateUserPoint(id, updatedPoint);

            PointHistory updatedPointHistory = pointHistoryService.insertHistory(id, amount, TransactionType.USE);

            return new RestResult("200", "User Use Success",
                    Map.of("updatedUserPoint", updatedUserPoint, "updatedPointHistory", updatedPointHistory));
        }catch (Exception e){
            throw new UserPointRuntimeException("충전 중 예외 발생: " + e.getMessage());
        }finally {
            lock.unlock();
        }



    }




}
