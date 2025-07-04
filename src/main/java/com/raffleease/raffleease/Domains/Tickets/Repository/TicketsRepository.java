package com.raffleease.raffleease.Domains.Tickets.Repository;

import com.raffleease.raffleease.Domains.Carts.Model.Cart;
import com.raffleease.raffleease.Domains.Raffles.Model.Raffle;
import com.raffleease.raffleease.Domains.Tickets.Model.Ticket;
import com.raffleease.raffleease.Domains.Tickets.Model.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketsRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByRaffleAndStatus(Raffle raffle, TicketStatus status);
    List<Ticket> findAllByRaffle(Raffle raffle);
    List<Ticket> findAllByCart(Cart cart);
}
