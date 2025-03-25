package io.hhplus.tdd.model.entity;

import io.hhplus.tdd.constants.TransactionType;

public record PointHistory(
        long id,
        long userId,
        long amount,
        TransactionType type,
        long updateMillis
) {
}
