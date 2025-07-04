package com.raffleease.raffleease.Domains.Notifications.Model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MailResponse {
    @JsonProperty("request_id")
    private String requestId;

    @JsonProperty("data")
    private MailResponseData data;

    @Data
    public static class MailResponseData {
        @JsonProperty("succeeded")
        private Integer succeeded;

        @JsonProperty("failed")
        private Integer failed;

        @JsonProperty("email_id")
        private String emailId;

        @JsonProperty("error_code")
        private String errorCode;

        @JsonProperty("error")
        private String error;
    }
} 