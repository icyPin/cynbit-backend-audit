package in.cynbit.audit.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class InquiryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Observed: /contact-observed returns 200 OK with minimal boolean")
    void observedEndpointReturnsSimpleBoolean() throws Exception {
        mockMvc.perform(post("/contact-observed")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Hardened: Valid inquiry returns 201 Created and defensive headers")
    void validInquiryReturns201WithReceiptAndSecurityHeaders() throws Exception {
        String payload = """
                {
                  "fullName": "Alex Rivera",
                  "email": "alex@example.com",
                  "company": "Acme Corp",
                  "industry": "Fintech",
                  "message": "We are looking to architect an autonomous AI workflow."
                }
                """;

        mockMvc.perform(post("/api/v1/inquiries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.receivedAt").isNotEmpty());
    }

    @Test
    @DisplayName("Validation: Invalid email and short message return 422 Unprocessable Entity")
    void invalidPayloadReturns422WithStructuredFieldErrors() throws Exception {
        String invalidPayload = """
                {
                  "fullName": "A",
                  "email": "not-an-email",
                  "message": "short"
                }
                """;

        mockMvc.perform(post("/api/v1/inquiries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields.fullName").isArray())
                .andExpect(jsonPath("$.error.fields.email").isArray())
                .andExpect(jsonPath("$.error.fields.message").isArray());
    }

    @Test
    @DisplayName("Idempotency: Replaying Idempotency-Key returns existing receipt")
    void duplicateSubmissionWithIdempotencyKeyIsSafelyDeduplicated() throws Exception {
        String payload = """
                {
                  "fullName": "Alex Rivera",
                  "email": "alex@example.com",
                  "message": "Testing idempotent retry behaviour on unstable networks."
                }
                """;

        String key = "test-idempotency-key-12345";

        mockMvc.perform(post("/api/v1/inquiries")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.idempotentReplay").value(false));

        mockMvc.perform(post("/api/v1/inquiries")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.idempotentReplay").value(true));
    }
}
