package com.raffleease.raffleease.Common.Exceptions;

import java.util.Map;

public final class ValidationErrorCodeMapper {
    private ValidationErrorCodeMapper() {}

    private static final Map<String, String> SPRING_TO_APP_CODES = Map.ofEntries(
            // Required field validations
            Map.entry("NotNull", "REQUIRED"),
            Map.entry("NotBlank", "REQUIRED"),
            Map.entry("NotEmpty", "REQUIRED"),

            // Format validations
            Map.entry("Email", "INVALID_EMAIL"),
            Map.entry("Pattern", "INVALID_FORMAT"),
            Map.entry("ValidPassword", "INVALID_FORMAT"),
            Map.entry("Digits", "INVALID_DIGITS"),
            Map.entry("URL", "INVALID_URL"),

            // Size and range
            Map.entry("Size", "INVALID_LENGTH"),
            Map.entry("Length", "INVALID_LENGTH"),
            Map.entry("Min", "TOO_SMALL"),
            Map.entry("Max", "TOO_LARGE"),
            Map.entry("DecimalMin", "TOO_SMALL"),
            Map.entry("DecimalMax", "TOO_LARGE"),

            // Number validations
            Map.entry("Positive", "MUST_BE_POSITIVE"),
            Map.entry("PositiveOrZero", "MUST_BE_POSITIVE_OR_ZERO"),
            Map.entry("Negative", "MUST_BE_NEGATIVE"),
            Map.entry("NegativeOrZero", "MUST_BE_NEGATIVE_OR_ZERO"),

            // Date validations
            Map.entry("Past", "MUST_BE_IN_PAST"),
            Map.entry("PastOrPresent", "MUST_BE_IN_PAST_OR_PRESENT"),
            Map.entry("Future", "MUST_BE_IN_FUTURE"),
            Map.entry("FutureOrPresent", "MUST_BE_IN_FUTURE_OR_PRESENT"),

            // Custom validations
            Map.entry("StartDateNotAfterEndDate", "END_DATE_VALIDATION_ERROR"),

            // Assertive checks
            Map.entry("AssertTrue", "MUST_BE_TRUE"),
            Map.entry("AssertFalse", "MUST_BE_FALSE"),

            // Type conversion
            Map.entry("typeMismatch", "INVALID_TYPE"),

            // Fallback
            Map.entry("Valid", "INVALID_NESTED_FIELD")
    );

    public static String toAppCode(String springCode) {
        return SPRING_TO_APP_CODES.getOrDefault(springCode, "INVALID_FIELD");
    }
}
