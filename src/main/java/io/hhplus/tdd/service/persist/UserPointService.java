package io.hhplus.tdd.service.persist;

import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.model.entity.UserPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserPointService {

    private final UserPointTable userPointRepository;
    public UserPoint getPointById(Long id){
        return userPointRepository.selectById(id);
    }

    public UserPoint saveOrUpdateUserPoint(long id, long amount){
        UserPoint resultUserPoint = userPointRepository.insertOrUpdate(id,amount);

        log.info("UserPoint Save : {}",resultUserPoint);
        return resultUserPoint;
    }

}
