package com.raffleease.raffleease.Domains.Raffles.Services;

import com.raffleease.raffleease.Domains.Associations.Model.Association;
import com.raffleease.raffleease.Domains.Raffles.DTOs.RaffleDTO;
import com.raffleease.raffleease.Domains.Raffles.DTOs.RaffleSearchFilters;
import com.raffleease.raffleease.Domains.Raffles.Model.Raffle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RafflesQueryService {
    /**
     * Fetches the information of the raffle with the provided ID.
     *
     * @param id the ID of the raffle to find
     * @return the found raffle
     */
    RaffleDTO get(Long id);

    /**
     * Searches for raffles based on the provided filters.
     *
     * @param associationId the ID of the association to search for raffles
     * @return a list of raffles of the provided association
     */
    List<RaffleDTO> search(Long associationId);

    /**
     * Fetches all raffles of the provided association.
     *
     * @param association the association to find raffles for
     * @return a list of raffles of the provided association
     */
    List<Raffle> findAllByAssociation(Association association);

    /**
     * Searches for raffles based on the provided filters.
     * 
     * @param associationId the ID of the association to search for raffles
     * @param filters the filters to apply to the search
     * @param pageable the pagination information
     * @return a page of raffles matching the search criteria
     */
    Page<RaffleDTO> search(Long associationId, RaffleSearchFilters filters, Pageable pageable);
}
