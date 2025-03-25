package io.hhplus.tdd.service.persist;


import io.hhplus.tdd.constants.TransactionType;
import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.model.entity.PointHistory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PointHistoryService {

    private final PointHistoryTable pointHistoryTable;

    public List<PointHistory> selectAllByUserId(long userId){
        return pointHistoryTable.selectAllByUserId(userId);
    }

    public PointHistory insertHistory(long userId, long amount, TransactionType type){
        PointHistory resultPointHistory = pointHistoryTable.insert(userId,amount,type,System.currentTimeMillis());

        log.info("Point History Save : {}", resultPointHistory);
        return resultPointHistory;
    }

}
