package in.cynbit.audit.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InquiryRequest(
        @NotBlank(message = "Full name is required")
        @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
        String fullName,

        @NotBlank(message = "Email address is required")
        @Email(message = "Must be a valid email address")
        @Size(max = 254, message = "Email address must not exceed 254 characters")
        String email,

        @Size(max = 100, message = "Company name must not exceed 100 characters")
        String company,

        @Size(max = 100, message = "Industry must not exceed 100 characters")
        String industry,

        @NotBlank(message = "Message is required")
        @Size(min = 10, max = 2000, message = "Message must be between 10 and 2000 characters")
        String message
) {}
