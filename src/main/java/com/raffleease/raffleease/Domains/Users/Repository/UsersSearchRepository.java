package com.raffleease.raffleease.Domains.Users.Repository;

import com.raffleease.raffleease.Domains.Users.DTOs.UserSearchFilters;
import com.raffleease.raffleease.Domains.Users.Model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UsersSearchRepository {
    Page<User> search(UserSearchFilters searchFilters, Long associationId, Pageable pageable);
} 