package com.raffleease.raffleease.Domains.Raffles.Services.Impl;

import com.raffleease.raffleease.Domains.Associations.Model.Association;
import com.raffleease.raffleease.Domains.Associations.Services.AssociationsService;
import com.raffleease.raffleease.Domains.Raffles.DTOs.RaffleDTO;
import com.raffleease.raffleease.Domains.Raffles.DTOs.RaffleSearchFilters;
import com.raffleease.raffleease.Domains.Raffles.Mappers.RafflesMapper;
import com.raffleease.raffleease.Domains.Raffles.Model.Raffle;
import com.raffleease.raffleease.Domains.Raffles.Repository.RafflesRepository;
import com.raffleease.raffleease.Domains.Raffles.Services.RafflesPersistenceService;
import com.raffleease.raffleease.Domains.Raffles.Services.RafflesQueryService;
import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.DatabaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class RafflesQueryServiceImpl implements RafflesQueryService {
    private final RafflesPersistenceService rafflesPersistence;
    private final RafflesRepository rafflesRepository;
    private final RafflesMapper mapper;
    private final AssociationsService associationsService;

    @Override
    public RaffleDTO get(Long id) {
        return mapper.fromRaffle(rafflesPersistence.findById(id));
    }

    @Override
    public List<RaffleDTO> search(Long associationId) {
        Association association = associationsService.findById(associationId);
        return mapper.fromRaffleList(findByAssociation(association));
    }

    @Override
    public List<Raffle> findAllByAssociation(Association association) {
        try {
            return rafflesRepository.findAllByAssociation(association);
        } catch (DataAccessException exp) {
            throw new DatabaseException("Database error occurred while retrieving raffles by association: " + exp.getMessage());
        }
    }

    private List<Raffle> findByAssociation(Association association) {
        try {
            return rafflesRepository.findByAssociation(association);
        } catch (DataAccessException exp) {
            throw new DatabaseException("Database error occurred while retrieving raffle by association: " + exp.getMessage());
        }
    }

    @Override
    public Page<RaffleDTO> search(Long associationId, RaffleSearchFilters filters, Pageable pageable) {
        Page<Raffle> page = rafflesRepository.search(filters, associationId, pageable);
        return page.map(mapper::fromRaffle);
    }
}
