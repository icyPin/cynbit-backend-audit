package in.cynbit.audit.controller;

import in.cynbit.audit.dto.ApiResponse;
import in.cynbit.audit.dto.ApiResponse.InquiryReceipt;
import in.cynbit.audit.dto.InquiryRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class InquiryController {

    private final Map<String, InquiryReceipt> idempotencyStore = new ConcurrentHashMap<>();

    @PostMapping("/contact-observed")
    public ResponseEntity<?> submitObserved() {
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/api/v1/inquiries")
    public ResponseEntity<ApiResponse<InquiryReceipt>> submitInquiry(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody InquiryRequest request
    ) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            InquiryReceipt existing = idempotencyStore.get(idempotencyKey);
            if (existing != null) {
                InquiryReceipt replayReceipt = new InquiryReceipt(
                        existing.id(),
                        existing.receivedAt(),
                        true
                );
                return ResponseEntity.ok(ApiResponse.success(replayReceipt));
            }
        }

        InquiryReceipt receipt = new InquiryReceipt(
                UUID.randomUUID().toString(),
                Instant.now(),
                false
        );

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencyStore.put(idempotencyKey, receipt);
        }

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(receipt));
    }
}
