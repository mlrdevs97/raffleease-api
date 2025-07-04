package com.raffleease.raffleease.Domains.Raffles.Services;

import com.raffleease.raffleease.Domains.Raffles.DTOs.RaffleDTO;
import com.raffleease.raffleease.Domains.Raffles.DTOs.RaffleEdit;

public interface RafflesEditService {
    /**
     * Edits a raffle.
     * Only ADMIN and MEMBER users can edit raffles.
     * 
     * @param id the ID of the raffle to edit
     * @param raffleEdit the raffle edit data
     * @return the edited raffle
     */
    RaffleDTO edit(Long id, RaffleEdit raffleEdit);
}
