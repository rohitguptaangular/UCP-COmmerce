package com.sap.ucp.service;

import com.sap.ucp.mock.SapOccMockService;
import com.sap.ucp.model.sap.SapOrder;
import com.sap.ucp.model.ucp.*;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Maps SAP Commerce order lifecycle to UCP OrderStatus.
 */
@Service
public class OrderService {

    private final SapOccMockService sapService;

    public OrderService(SapOccMockService sapService) {
        this.sapService = sapService;
    }

    public Optional<OrderStatus> getOrderStatus(String orderId) {
        return sapService.getOrder(orderId).map(this::mapSapOrderToUcp);
    }

    private OrderStatus mapSapOrderToUcp(SapOrder sapOrder) {
        OrderStatus status = new OrderStatus();
        status.setOrderId(sapOrder.getCode());
        status.setStatus(mapSapStatusToUcp(sapOrder.getStatus()));
        status.setCheckoutSessionId(sapOrder.getCartCode());
        status.setCreatedAt(sapOrder.getCreated());
        status.setUpdatedAt(sapOrder.getStatusUpdated());
        status.setTrackingNumber(sapOrder.getTrackingNumber());

        status.setItems(sapOrder.getEntries().stream().map(entry -> {
            CheckoutItem item = new CheckoutItem();
            item.setProductId(entry.getProductCode());
            item.setName(entry.getProductName());
            item.setQuantity(entry.getQuantity());
            item.setUnitPrice(entry.getBasePrice());
            item.setCurrency(entry.getCurrency());
            return item;
        }).collect(Collectors.toList()));

        CheckoutSession.PriceSummary pricing = new CheckoutSession.PriceSummary();
        pricing.setSubtotal(sapOrder.getTotalPrice() - sapOrder.getDeliveryCost() - sapOrder.getTotalTax());
        pricing.setShipping(sapOrder.getDeliveryCost());
        pricing.setTax(sapOrder.getTotalTax());
        pricing.setTotal(sapOrder.getTotalPrice());
        pricing.setCurrency(sapOrder.getCurrency());
        status.setPricing(pricing);

        if (sapOrder.getDeliveryAddress() != null) {
            Address addr = new Address();
            addr.setName((sapOrder.getDeliveryAddress().getFirstName() + " " + sapOrder.getDeliveryAddress().getLastName()).trim());
            addr.setStreetAddress(sapOrder.getDeliveryAddress().getLine1());
            addr.setCity(sapOrder.getDeliveryAddress().getTown());
            addr.setState(sapOrder.getDeliveryAddress().getRegion());
            addr.setPostalCode(sapOrder.getDeliveryAddress().getPostalCode());
            addr.setCountry(sapOrder.getDeliveryAddress().getCountry());
            status.setShippingAddress(addr);
        }

        return status;
    }

    private String mapSapStatusToUcp(String sapStatus) {
        return switch (sapStatus) {
            case "CREATED" -> "confirmed";
            case "PROCESSING" -> "processing";
            case "SHIPPED" -> "shipped";
            case "DELIVERED" -> "delivered";
            case "CANCELLED" -> "cancelled";
            default -> "confirmed";
        };
    }
}
