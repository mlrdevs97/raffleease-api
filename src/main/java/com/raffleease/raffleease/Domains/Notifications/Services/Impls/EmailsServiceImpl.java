package com.raffleease.raffleease.Domains.Notifications.Services.Impls;

import com.raffleease.raffleease.Domains.Customers.Model.Customer;
import com.raffleease.raffleease.Domains.Notifications.Model.NotificationType;
import com.raffleease.raffleease.Domains.Notifications.Model.MailRequest;
import com.raffleease.raffleease.Domains.Notifications.Model.MailResponse;
import com.raffleease.raffleease.Domains.Notifications.Services.EmailsService;
import com.raffleease.raffleease.Domains.Notifications.Services.NotificationsService;
import com.raffleease.raffleease.Domains.Orders.Model.Order;
import com.raffleease.raffleease.Domains.Orders.Model.OrderItem;
import com.raffleease.raffleease.Domains.Payments.Model.Payment;
import com.raffleease.raffleease.Domains.Users.Model.User;
import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.CustomMailException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Arrays;

import static com.raffleease.raffleease.Domains.Notifications.Model.EmailTemplate.EMAIL_VERIFICATION;
import static com.raffleease.raffleease.Domains.Notifications.Model.EmailTemplate.ORDER_CREATED;
import static com.raffleease.raffleease.Domains.Notifications.Model.EmailTemplate.ORDER_COMPLETED;
import static com.raffleease.raffleease.Domains.Notifications.Model.EmailTemplate.ORDER_CANCELLED;
import static com.raffleease.raffleease.Domains.Notifications.Model.EmailTemplate.ORDER_REFUNDED;
import static com.raffleease.raffleease.Domains.Notifications.Model.EmailTemplate.ORDER_UNPAID;
import static com.raffleease.raffleease.Domains.Notifications.Model.EmailTemplate.PASSWORD_RESET;
import static com.raffleease.raffleease.Domains.Notifications.Model.EmailTemplate.EMAIL_UPDATE_VERIFICATION;
import static com.raffleease.raffleease.Domains.Notifications.Model.EmailTemplate.USER_CREATION_VERIFICATION;
import static com.raffleease.raffleease.Domains.Notifications.Model.NotificationChannel.EMAIL;

@Slf4j
@RequiredArgsConstructor
@Service
public class EmailsServiceImpl implements EmailsService {
    private final NotificationsService notificationsService;
    private final SpringTemplateEngine templateEngine;
    private final RestTemplate restTemplate;

    @Value("${mail.API-KEY}")
    private String apiKey;

    @Value("${mail.API-URL}")
    private String apiUrl;

    @Value("${mail.SENDER-EMAIL}")
    private String senderEmail;

    @Override
    @Async
    public void sendEmailVerificationEmail(User user, String link) {
        Map<String, Object> variables = createEmailVerificationEmailVariables(user, link);
        String htmlContent = processTemplate(EMAIL_VERIFICATION.getTemplate(), variables);
        sendEmail(user.getEmail(), EMAIL_VERIFICATION.getSubject(), htmlContent);
        notificationsService.create(NotificationType.EMAIL_VERIFICATION, EMAIL);
    }

    @Override
    @Async
    public void sendPasswordResetEmail(User user, String link) {
        Map<String, Object> variables = createPasswordResetEmailVariables(user, link);
        String htmlContent = processTemplate(PASSWORD_RESET.getTemplate(), variables);
        sendEmail(user.getEmail(), PASSWORD_RESET.getSubject(), htmlContent);
        notificationsService.create(NotificationType.PASSWORD_RESET, EMAIL);
    }

    @Override
    @Async
    public void sendOrderCreatedEmail(Order order) {
        Map<String, Object> variables = createOrderLifecycleEmailVariables(order);
        String htmlContent = processTemplate(ORDER_CREATED.getTemplate(), variables);
        sendEmail(order.getCustomer().getEmail(), ORDER_CREATED.getSubject(), htmlContent);
        notificationsService.create(NotificationType.ORDER_CREATED, EMAIL);
    }

    @Override
    @Async
    public void sendOrderCompletedEmail(Order order) {
        Map<String, Object> variables = createOrderLifecycleEmailVariables(order);
        String htmlContent = processTemplate(ORDER_COMPLETED.getTemplate(), variables);
        sendEmail(order.getCustomer().getEmail(), ORDER_COMPLETED.getSubject(), htmlContent);
        notificationsService.create(NotificationType.ORDER_COMPLETED, EMAIL);
    }

    @Override
    @Async
    public void sendOrderCancelledEmail(Order order) {
        Map<String, Object> variables = createOrderLifecycleEmailVariables(order);
        String htmlContent = processTemplate(ORDER_CANCELLED.getTemplate(), variables);
        sendEmail(order.getCustomer().getEmail(), ORDER_CANCELLED.getSubject(), htmlContent);
        notificationsService.create(NotificationType.ORDER_CANCELLED, EMAIL);
    }

    @Override
    @Async
    public void sendOrderRefundedEmail(Order order) {
        Map<String, Object> variables = createOrderLifecycleEmailVariables(order);
        String htmlContent = processTemplate(ORDER_REFUNDED.getTemplate(), variables);
        sendEmail(order.getCustomer().getEmail(), ORDER_REFUNDED.getSubject(), htmlContent);
        notificationsService.create(NotificationType.ORDER_REFUNDED, EMAIL);
    }

    @Override
    @Async
    public void sendOrderUnpaidEmail(Order order) {
        Map<String, Object> variables = createOrderLifecycleEmailVariables(order);
        String htmlContent = processTemplate(ORDER_UNPAID.getTemplate(), variables);
        sendEmail(order.getCustomer().getEmail(), ORDER_UNPAID.getSubject(), htmlContent);
        notificationsService.create(NotificationType.ORDER_UNPAID, EMAIL);
    }

    @Override
    @Async
    public void sendEmailUpdateVerificationEmail(User user, String newEmail, String link) {
        Map<String, Object> variables = createEmailUpdateVerificationEmailVariables(user, newEmail, link);
        String htmlContent = processTemplate(EMAIL_UPDATE_VERIFICATION.getTemplate(), variables);
        sendEmail(newEmail, EMAIL_UPDATE_VERIFICATION.getSubject(), htmlContent);
        notificationsService.create(NotificationType.EMAIL_UPDATE_VERIFICATION, EMAIL);
    }

    @Override
    @Async
    public void sendUserCreationVerificationEmail(User user, String associationName, String link) {
        Map<String, Object> variables = createUserCreationVerificationEmailVariables(user, associationName, link);
        String htmlContent = processTemplate(USER_CREATION_VERIFICATION.getTemplate(), variables);
        sendEmail(user.getEmail(), USER_CREATION_VERIFICATION.getSubject(), htmlContent);
        notificationsService.create(NotificationType.USER_CREATION_VERIFICATION, EMAIL);
    }

    /**
     * Capitalizes each word in a full name properly.
     * Converts "john doe" to "John Doe" or "miguel angel revilla gonzalez" to "Miguel Angel Revilla Gonzalez".
     * Handles accented characters correctly.
     * 
     * @param fullName the full name to capitalize (can be null or empty)
     * @return the properly capitalized full name, or empty string if input is null/empty
     */
    private String capitalizeFullName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "";
        }
        
        return Arrays.stream(fullName.trim().split("\\s+"))
                .map(word -> {
                    if (word.isEmpty()) {
                        return word;
                    }
                    // Handle accented characters properly
                    return word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase();
                })
                .collect(Collectors.joining(" "));
    }

    private Map<String, Object> createEmailVerificationEmailVariables(User user, String link) {
        Map<String, Object> variables = new HashMap<>();
        String formattedRegistrationDate = user.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm"));

        variables.put("customerName", capitalizeFullName(user.getUserName()));
        variables.put("customerEmail", user.getEmail());
        variables.put("senderEmail", senderEmail);
        variables.put("registrationDate", formattedRegistrationDate);
        variables.put("verificationUrl", link);

        return variables;
    }

    private Map<String, Object> createPasswordResetEmailVariables(User user, String link) {
        Map<String, Object> variables = new HashMap<>();
        String formattedRequestDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm"));

        variables.put("userName", capitalizeFullName(user.getUserName()));
        variables.put("userEmail", user.getEmail());
        variables.put("senderEmail", senderEmail);
        variables.put("requestDate", formattedRequestDate);
        variables.put("resetUrl", link);

        return variables;
    }

    private Map<String, Object> createOrderLifecycleEmailVariables(Order order) {
        Map<String, Object> variables = new HashMap<>();
        Payment paymentData = order.getPayment();
        Customer customer = order.getCustomer();
        String formattedOrderDate = order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm"));
        List<String> ticketNumbers = order.getOrderItems().stream()
                .map(OrderItem::getTicketNumber)
                .collect(Collectors.toList());

        variables.put("customer", customer);
        variables.put("paymentData", paymentData);
        variables.put("orderData", order);
        variables.put("customerName", capitalizeFullName(customer.getFullName()));
        variables.put("customerEmail", customer.getEmail());
        variables.put("customerPhoneNumber", customer.getPhoneNumber());
        variables.put("senderEmail", senderEmail);
        variables.put("paymentMethod", paymentData.getPaymentMethod());
        variables.put("paymentTotal", paymentData.getTotal());
        variables.put("orderReference", order.getOrderReference());
        variables.put("orderDate", formattedOrderDate);
        variables.put("ticketList", ticketNumbers);
        variables.put("ticketCount", ticketNumbers.size());
        variables.put("raffleName", order.getRaffle().getTitle());
        
        if (order.getCompletedAt() != null) {
            variables.put("completedDate", order.getCompletedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm")));
        }
        if (order.getCancelledAt() != null) {
            variables.put("cancelledDate", order.getCancelledAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm")));
        }
        if (order.getRefundedAt() != null) {
            variables.put("refundedDate", order.getRefundedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm")));
        }
        if (order.getUnpaidAt() != null) {
            variables.put("unpaidDate", order.getUnpaidAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm")));
        }

        return variables;
    }

    private Map<String, Object> createEmailUpdateVerificationEmailVariables(User user, String newEmail, String link) {
        Map<String, Object> variables = new HashMap<>();
        String formattedRequestDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm"));

        variables.put("customerName", capitalizeFullName(user.getUserName()));
        variables.put("customerEmail", user.getEmail());
        variables.put("newEmail", newEmail);
        variables.put("senderEmail", senderEmail);
        variables.put("requestDate", formattedRequestDate);
        variables.put("updateUrl", link);

        return variables;
    }

    private Map<String, Object> createUserCreationVerificationEmailVariables(User user, String associationName, String link) {
        Map<String, Object> variables = new HashMap<>();
        String formattedCreationDate = user.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm"));
        String fullName = user.getFirstName() + " " + user.getLastName();

        variables.put("customerName", capitalizeFullName(fullName));
        variables.put("customerUsername", user.getUserName());
        variables.put("customerEmail", user.getEmail());
        variables.put("associationName", associationName);
        variables.put("senderEmail", senderEmail);
        variables.put("creationDate", formattedCreationDate);
        variables.put("verificationUrl", link);

        return variables;
    }

    private String processTemplate(String templateName, Map<String, Object> variables) {
        try {
            Context context = new Context();
            context.setVariables(variables);
            return templateEngine.process(templateName, context);
        } catch (Exception ex) {
            log.error("Error processing email template {}: {}", templateName, ex.getMessage());
            throw new CustomMailException("Error processing email template: " + ex.getMessage());
        }
    }

    private void sendEmail(String to, String subject, String htmlContent) {
        try {
            MailRequest emailRequest = MailRequest.builder()
                    .sender(senderEmail)
                    .to(List.of(to))
                    .subject(subject)
                    .htmlBody(htmlContent)
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Smtp2go-Api-Key", apiKey);
            headers.set("accept", "application/json");

            HttpEntity<MailRequest> requestEntity = new HttpEntity<>(emailRequest, headers);

            // Construct the complete endpoint URL
            String emailEndpoint = apiUrl + "/email/send";

            ResponseEntity<MailResponse> response = restTemplate.exchange(
                    emailEndpoint,
                    HttpMethod.POST,
                    requestEntity,
                    MailResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                MailResponse responseBody = response.getBody();
                if (responseBody.getData() != null && responseBody.getData().getSucceeded() != null && responseBody.getData().getSucceeded() > 0) {
                    log.info("Email sent successfully to {} with email ID: {}", to, responseBody.getData().getEmailId());
                } else {
                    String errorMessage = responseBody.getData() != null ? responseBody.getData().getError() : "Unknown error";
                    throw new CustomMailException("Failed to send email: " + errorMessage);
                }
            } else {
                throw new CustomMailException("Failed to send email: HTTP " + response.getStatusCode());
            }

        } catch (RestClientException ex) {
            log.error("Error sending email to {}: {}", to, ex.getMessage());
            throw new CustomMailException("Error occurred while sending email: " + ex.getMessage());
        }
    }
}
