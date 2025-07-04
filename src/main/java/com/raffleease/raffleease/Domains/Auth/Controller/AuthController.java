package com.raffleease.raffleease.Domains.Auth.Controller;

import com.raffleease.raffleease.Domains.Auth.DTOs.Register.RegisterRequest;
import com.raffleease.raffleease.Domains.Auth.DTOs.LoginRequest;
import com.raffleease.raffleease.Domains.Auth.DTOs.RegisterEmailVerificationRequest;
import com.raffleease.raffleease.Domains.Auth.DTOs.ForgotPasswordRequest;
import com.raffleease.raffleease.Domains.Auth.DTOs.ResetPasswordRequest;
import com.raffleease.raffleease.Domains.Auth.Services.AuthValidationService;
import com.raffleease.raffleease.Domains.Auth.Services.LoginService;
import com.raffleease.raffleease.Domains.Auth.Services.PasswordResetService;
import com.raffleease.raffleease.Domains.Auth.Services.RegisterService;
import com.raffleease.raffleease.Domains.Auth.Services.VerificationService;
import com.raffleease.raffleease.Common.Responses.ApiResponse;
import com.raffleease.raffleease.Common.Responses.ResponseFactory;
import com.raffleease.raffleease.Common.RateLimiting.RateLimit;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.raffleease.raffleease.Common.RateLimiting.RateLimit.AccessLevel.PRIVATE;
import static com.raffleease.raffleease.Common.RateLimiting.RateLimit.AccessLevel.PUBLIC;

@RequiredArgsConstructor
@RequestMapping("/v1/auth")
@RestController
public class AuthController {
    private final RegisterService registerService;
    private final LoginService loginService;
    private final LogoutHandler logoutHandler;
    private final AuthValidationService authValidationService;
    private final VerificationService verificationService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    @RateLimit(operation = "create", accessLevel = PUBLIC)
    public ResponseEntity<ApiResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response
    ) {
        return ResponseEntity.ok().body(
                ResponseFactory.success(
                        registerService.register(request, response),
                        "New association account created successfully"
                )
        );
    }

    @PostMapping("/verify")
    @RateLimit(operation = "update", accessLevel = PUBLIC)
    public ResponseEntity<ApiResponse> verify(
            @RequestBody @Valid RegisterEmailVerificationRequest request
    ) {
        verificationService.verifyEmail(request.verificationToken());
        return ResponseEntity.ok().body(
                ResponseFactory.success(
                        null,
                        "Account verified successfully"
                )
        );
    }

    @PostMapping("/forgot-password")
    @RateLimit(operation = "create", accessLevel = PUBLIC)
    public ResponseEntity<ApiResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        passwordResetService.requestPasswordReset(request);
        return ResponseEntity.ok().body(
                ResponseFactory.success(
                        null,
                        "Password reset link has been sent successfully"
                )
        );
    }

    @PostMapping("/reset-password")
    @RateLimit(operation = "update", accessLevel = PUBLIC)
    public ResponseEntity<ApiResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {        
        passwordResetService.resetPassword(request);
        return ResponseEntity.ok().body(
                ResponseFactory.success(
                        null,
                        "Password has been reset successfully"
                )
        );
    }

    @PostMapping("/login")
    @RateLimit(operation = "read", accessLevel = PUBLIC)
    public ResponseEntity<ApiResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        return ResponseEntity.ok().body(
                ResponseFactory.success(
                        loginService.login(request, response),
                        "User authenticated successfully"
                )
        );
    }

    @PostMapping("/logout")
    @RateLimit(operation = "update", accessLevel = PRIVATE)
    public ResponseEntity<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) {
        logoutHandler.logout(request, response, authentication);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/validate")
    @RateLimit(operation = "read", accessLevel = PRIVATE)
    public ResponseEntity<ApiResponse> validate() {
        authValidationService.isUserAuthenticated();
        return ResponseEntity.ok().body(
                ResponseFactory.success("User authentication validated successfully")
        );
    }
}
