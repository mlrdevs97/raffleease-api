package com.raffleease.raffleease.Domains.Users.Services;

import com.raffleease.raffleease.Domains.Users.DTOs.UpdateEmailRequest;
import com.raffleease.raffleease.Domains.Users.DTOs.VerifyEmailUpdateRequest;

public interface EmailUpdateService {
    void requestEmailUpdate(Long userId, UpdateEmailRequest request);
    void verifyEmailUpdate(VerifyEmailUpdateRequest request);
} 