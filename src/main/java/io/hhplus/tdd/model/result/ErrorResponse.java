package io.hhplus.tdd.model.result;

public record ErrorResponse(
        String code,
        String message
) {
}
