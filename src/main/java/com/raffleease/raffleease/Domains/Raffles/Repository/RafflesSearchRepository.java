package com.raffleease.raffleease.Domains.Raffles.Repository;

import com.raffleease.raffleease.Domains.Raffles.DTOs.RaffleSearchFilters;
import com.raffleease.raffleease.Domains.Raffles.Model.Raffle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RafflesSearchRepository {
    Page<Raffle> search(RaffleSearchFilters searchFilters, Long associationId, Pageable pageable);
} 