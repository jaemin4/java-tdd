package io.hhplus.tdd.model.entity;

public record UserPoint(
        long id,
        long point,
        long updateMillis
) {


    public static UserPoint empty(long id) {
        return new UserPoint(id, 0, System.currentTimeMillis());
    }
    public UserPoint(long id, long point) {
        this(id, point, System.currentTimeMillis());
    }

}
