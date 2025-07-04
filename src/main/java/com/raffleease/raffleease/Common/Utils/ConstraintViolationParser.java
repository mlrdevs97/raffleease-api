package com.raffleease.raffleease.Common.Utils;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConstraintViolationParser {
    public static Optional<String> extractConstraintName(Throwable ex) {
        Throwable cause = ex;
        while (cause != null) {
            String message = cause.getMessage();
            if (message != null && message.contains("uk_")) {
                Matcher matcher = Pattern.compile("uk_[a-zA-Z0-9_]+").matcher(message.toLowerCase());
                if (matcher.find()) {
                    return Optional.of(matcher.group());
                }
            }
            cause = cause.getCause();
        }
        return Optional.empty();
    }
}
