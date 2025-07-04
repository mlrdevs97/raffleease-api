package com.raffleease.raffleease.Domains.Raffles.Services;

import com.raffleease.raffleease.Domains.Raffles.Model.Raffle;

import java.util.List;

public interface RafflesPersistenceService {
    /**
     * Saves a raffle.
     * 
     * @param raffle the raffle to save
     * @return the saved raffle
     */
    Raffle save(Raffle raffle);

    /**
     * Deletes a raffle.
     * 
     * @param raffle the raffle to delete
     */
    void delete(Raffle raffle);

    /**
     * Finds a raffle by its ID.
     * 
     * @param id the ID of the raffle to find
     * @return the found raffle
     */
    Raffle findById(Long id);

    /**
     * Saves a list of raffles.
     * 
     * @param raffles the list of raffles to save
     */
    void saveAll(List<Raffle> raffles);
}
