package com.raffleease.raffleease.Domains.Orders.Services;

import com.raffleease.raffleease.Domains.Orders.DTOs.OrderDTO;
import com.raffleease.raffleease.Domains.Orders.DTOs.OrderSearchFilters;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrdersQueryService {
    /**
     * Finds an order by its ID.
     * 
     * @param id the ID of the order to find
     * @return the found order
     */
    OrderDTO get(Long id);
    
    /**
     * Searches for orders based on the provided filters.
     * 
     * @param filters the filters to apply to the search
     * @param associationId the ID of the association to search for orders
     * @param pageable the pagination information
     * @return a page of orders matching the search criteria
     */
    Page<OrderDTO> search(OrderSearchFilters filters, Long associationId, Pageable pageable);
}
