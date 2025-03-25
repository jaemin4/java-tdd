package io.hhplus.tdd.controller;

import io.hhplus.tdd.model.result.RestResult;
import io.hhplus.tdd.service.front.UserPointFrontService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/point")
@RequiredArgsConstructor
public class PointController {

    private static final Logger log = LoggerFactory.getLogger(PointController.class);
    private final UserPointFrontService userPointFrontService;

    /**
     * TODO - 특정 유저의 포인트를 조회하는 기능을 작성해주세요.
     */
    @GetMapping("{id}")
    public RestResult point(@PathVariable long id) {
        log.info("/{} : {}", id, id);

        return userPointFrontService.getPointById(id);
    }
    /**
     * TODO - 특정 유저의 포인트 충전/이용 내역을 조회하는 기능을 작성해주세요.
     */
    @GetMapping("{id}/histories")
    public RestResult history(@PathVariable long id){
        log.info("/{}/histories : {}", id, id);

        return userPointFrontService.RetrieveUserHistoryById(id);
    }
    /**
     * TODO - 특정 유저의 포인트를 충전하는 기능을 작성해주세요.
     */
    @PatchMapping("{id}/charge")
    public RestResult charge(@PathVariable long id, @RequestBody long amount) {
        log.info("/{}/charge : {}, {}", id, id, amount);

        return userPointFrontService.chargeUserPoint(id,amount);
    }


    /**
     * TODO - 특정 유저의 포인트를 사용하는 기능을 작성해주세요.
     */
    @PatchMapping("{id}/use")
    public RestResult use(@PathVariable long id,@RequestBody long amount){
        log.info("/{}/use : {}, {}", id, id, amount);

        return userPointFrontService.useUserPoint(id,amount);
    }
}
