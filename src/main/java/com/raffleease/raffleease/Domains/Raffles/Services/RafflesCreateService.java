package com.raffleease.raffleease.Domains.Raffles.Services;

import com.raffleease.raffleease.Domains.Raffles.DTOs.RaffleCreate;
import com.raffleease.raffleease.Domains.Raffles.DTOs.RaffleDTO;

public interface RafflesCreateService {
    /**
     * Creates a new raffle.
     * Only ADMIN and MEMBER users can create raffles.
     * 
     * @param associationId the ID of the association
     * @param raffleData the raffle data
     * @return the created raffle
     */ 
    RaffleDTO create(Long associationId, RaffleCreate raffleData);
}
