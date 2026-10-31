package in.cynbit.audit.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ApiResponse<T>(
        boolean ok,
        T data,
        ErrorDetail error
) {
    public record InquiryReceipt(
            String id,
            Instant receivedAt,
            boolean idempotentReplay
    ) {}

    public record ErrorDetail(
            String code,
            String message,
            Map<String, List<String>> fields
    ) {}

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> validationError(Map<String, List<String>> fieldErrors) {
        return new ApiResponse<>(
                false,
                null,
                new ErrorDetail("VALIDATION_ERROR", "One or more fields failed validation", fieldErrors)
        );
    }
}
