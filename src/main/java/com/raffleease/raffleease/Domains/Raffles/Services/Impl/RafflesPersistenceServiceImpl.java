package com.raffleease.raffleease.Domains.Raffles.Services.Impl;

import com.raffleease.raffleease.Domains.Raffles.Model.Raffle;
import com.raffleease.raffleease.Domains.Raffles.Repository.RafflesRepository;
import com.raffleease.raffleease.Domains.Raffles.Services.RafflesPersistenceService;
import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.ConflictException;
import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.DatabaseException;
import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class RafflesPersistenceServiceImpl implements RafflesPersistenceService {
    private final RafflesRepository repository;

    @Override
    public Raffle findById(Long id) {
        try {
            return repository.findById(id).orElseThrow(() -> new NotFoundException("Raffle not found for id <" + id + ">"));
        } catch (DataAccessException ex) {
            throw new DatabaseException("Database error occurred while fetching raffle with ID <" + id + ">: " + ex.getMessage());
        }
    }

    @Override
    public void saveAll(List<Raffle> raffles) {
        try {
            repository.saveAll(raffles);
        } catch (DataAccessException ex) {
            throw new DatabaseException("Database error occurred while saving raffles: " + ex.getMessage());
        }
    }

    @Override
    public Raffle save(Raffle raffles) {
        try {
            return repository.save(raffles);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("Failed to save raffle due to unique constraint violation: " + ex.getMessage());
        } catch (DataAccessException ex) {
            throw new DatabaseException("Database error occurred while saving raffle: " + ex.getMessage());
        }
    }

    @Override
    public void delete(Raffle raffle) {
        try {
            repository.delete(raffle);
        } catch (DataAccessException ex) {
            throw new DatabaseException("Database error occurred while deleting raffle: " + ex.getMessage());
        }
    }
}
