package com.raffleease.raffleease.Domains.Raffles.Services;

import com.raffleease.raffleease.Domains.Raffles.DTOs.RaffleDTO;
import com.raffleease.raffleease.Domains.Raffles.DTOs.StatusUpdate;
import com.raffleease.raffleease.Domains.Raffles.Model.Raffle;

public interface RafflesStatusService {
    /**
     * Updates the status of a raffle.
     * Only ADMINS and MEMBERS of the association can update the status of a raffle.
     * 
     * @param id the ID of the raffle to update
     * @param request the status update request
     * @return the updated raffle
     */
    RaffleDTO updateStatus(Long id, StatusUpdate request);

    /**
     * Deletes a raffle.
     * Only ADMINS and MEMBERS of the association can delete a raffle.
     * AN association can only delete a raffle if it is in PENDING status.
     * 
     * @param id the ID of the raffle to delete
     */
    void delete(Long id);

    /**
     * Completes a raffle if all tickets are sold.
     * 
     * @param raffle the raffle to complete
     */
    void completeRaffleIfAllTicketsSold(Raffle raffle);

    /**
     * Updates the status of a raffle after the available tickets are increased.
     * It can reactivate a raffle that was previously completed with reason "All tickets sold".
     * 
     * @param raffle the raffle to update
     */
    void updateStatusAfterAvailableTicketsIncrease(Raffle raffle);

    /**
     * Reactivates raffles that were completed due to END_DATE_REACHED.
     * Used when the end date of a raffle is edited.
     * 
     * @param raffle the raffle to potentially reactivate
     */
    void reactivateRaffleAfterEndDateChange(Raffle raffle);
}
