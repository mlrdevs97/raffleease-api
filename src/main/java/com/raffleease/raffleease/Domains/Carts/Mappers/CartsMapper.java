package com.raffleease.raffleease.Domains.Carts.Mappers;

import com.raffleease.raffleease.Domains.Carts.DTO.CartDTO;
import com.raffleease.raffleease.Domains.Carts.Model.Cart;
import org.springframework.stereotype.Service;

@Service
public interface CartsMapper {
    CartDTO fromCart(Cart cart);
}
