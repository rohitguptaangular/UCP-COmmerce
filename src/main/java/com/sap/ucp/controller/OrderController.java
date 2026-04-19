package com.sap.ucp.controller;

import com.sap.ucp.model.ucp.OrderStatus;
import com.sap.ucp.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * UCP Order Management Capability.
 *
 * Provides order status lookups mapped from SAP Commerce order lifecycle.
 *
 * SAP Commerce OCC equivalent:
 *   GET /occ/v2/{site}/users/{userId}/orders/{code}
 *
 * In a full implementation, this would also push webhook events to the
 * UCP platform when order status changes (shipped, delivered, returned).
 */
@RestController
@RequestMapping("/ucp/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Get order status by order ID.
     * UCP: GET /ucp/api/orders/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderStatus> getOrder(@PathVariable String id) {
        return orderService.getOrderStatus(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
