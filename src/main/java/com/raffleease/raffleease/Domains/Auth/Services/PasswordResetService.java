package com.raffleease.raffleease.Domains.Auth.Services;

import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.EmailVerificationException;
import com.raffleease.raffleease.Domains.Auth.DTOs.ForgotPasswordRequest;
import com.raffleease.raffleease.Domains.Auth.DTOs.ResetPasswordRequest;

public interface PasswordResetService {
    /*
     * Request a password reset for a non-authenticated user to be sent to their email.
     *
     * @param request The request containing the email
     */
    void requestPasswordReset(ForgotPasswordRequest request);

    /**
     * Reset the password for a non-authenticated user using a token sent to their email.
     *
     * @param request The request containing the token and new password
     * @throws EmailVerificationException If the token is invalid or expired
     */
    void resetPassword(ResetPasswordRequest request);
} 