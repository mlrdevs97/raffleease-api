package com.raffleease.raffleease.Domains.Raffles.Mappers;

import com.raffleease.raffleease.Domains.Raffles.DTOs.RaffleDTO;
import com.raffleease.raffleease.Domains.Raffles.Model.Raffle;

import java.util.List;

public interface RafflesMapper {
    RaffleDTO fromRaffle(Raffle raffle);
    List<RaffleDTO> fromRaffleList(List<Raffle> raffles);
}
