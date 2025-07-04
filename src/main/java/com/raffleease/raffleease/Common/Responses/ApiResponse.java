package com.raffleease.raffleease.Common.Responses;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public abstract class ApiResponse {
        private final boolean success;
        private final String message;
        private final LocalDateTime timestamp;

        public ApiResponse(boolean success, String message) {
                this.success = success;
                this.message = message;
                this.timestamp = LocalDateTime.now();
        }
}