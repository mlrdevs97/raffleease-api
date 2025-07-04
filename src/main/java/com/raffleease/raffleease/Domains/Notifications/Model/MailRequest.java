package com.raffleease.raffleease.Domains.Notifications.Model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MailRequest {
    @JsonProperty("sender")
    private String sender;

    @JsonProperty("to")
    private List<String> to;

    @JsonProperty("subject")
    private String subject;

    @JsonProperty("html_body")
    private String htmlBody;

    @JsonProperty("text_body")
    private String textBody;
} 