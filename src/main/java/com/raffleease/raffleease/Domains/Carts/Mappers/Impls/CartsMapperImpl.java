package com.raffleease.raffleease.Domains.Carts.Mappers.Impls;

import com.raffleease.raffleease.Domains.Carts.DTO.CartDTO;
import com.raffleease.raffleease.Domains.Carts.Mappers.CartsMapper;
import com.raffleease.raffleease.Domains.Carts.Model.Cart;
import com.raffleease.raffleease.Domains.Tickets.Mappers.TicketsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CartsMapperImpl implements CartsMapper {
    private final TicketsMapper ticketsMapper;

    @Override
    public CartDTO fromCart(Cart cart) {
        return CartDTO.builder()
                .id(cart.getId())
                .userId(cart.getUser().getId())
                .tickets(ticketsMapper.fromTicketList(cart.getTickets()))
                .status(cart.getStatus())
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .build();
    }
}
