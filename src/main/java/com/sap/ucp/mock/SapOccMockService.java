package com.sap.ucp.mock;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.ucp.model.sap.*;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simulates SAP Commerce Cloud OCC REST APIs in-memory.
 * In production, this would be replaced with actual HTTP calls to SAP Commerce OCC endpoints.
 */
@Service
public class SapOccMockService {

    private final ObjectMapper objectMapper;
    private final Map<String, SapProduct> productCatalog = new ConcurrentHashMap<>();
    private final Map<String, SapCart> carts = new ConcurrentHashMap<>();
    private final Map<String, SapOrder> orders = new ConcurrentHashMap<>();
    private final AtomicInteger cartCounter = new AtomicInteger(1000);
    private final AtomicInteger orderCounter = new AtomicInteger(5000);

    public SapOccMockService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        try (InputStream is = getClass().getResourceAsStream("/mock-products.json")) {
            List<SapProduct> products = objectMapper.readValue(is, new TypeReference<>() {});
            products.forEach(p -> productCatalog.put(p.getCode(), p));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load mock products", e);
        }
    }

    // --- Product APIs (simulates GET /occ/v2/{site}/products/{code}) ---

    public Optional<SapProduct> getProduct(String code) {
        return Optional.ofNullable(productCatalog.get(code));
    }

    public List<SapProduct> getAllProducts() {
        return new ArrayList<>(productCatalog.values());
    }

    // --- Cart APIs (simulates OCC cart endpoints) ---

    /** POST /occ/v2/{site}/users/anonymous/carts */
    public SapCart createCart() {
        SapCart cart = new SapCart();
        cart.setCode("cart-" + cartCounter.incrementAndGet());
        cart.setGuid(UUID.randomUUID().toString());
        cart.setCurrency("USD");
        cart.setSubTotal(0);
        cart.setDeliveryCost(0);
        cart.setTotalTax(0);
        cart.setTotalPrice(0);
        carts.put(cart.getCode(), cart);
        return cart;
    }

    /** POST /occ/v2/{site}/users/{userId}/carts/{cartId}/entries */
    public SapCart addToCart(String cartCode, String productCode, int quantity) {
        SapCart cart = carts.get(cartCode);
        if (cart == null) throw new NoSuchElementException("Cart not found: " + cartCode);

        SapProduct product = productCatalog.get(productCode);
        if (product == null) throw new NoSuchElementException("Product not found: " + productCode);

        // Check if product already in cart
        Optional<SapCartEntry> existing = cart.getEntries().stream()
                .filter(e -> e.getProductCode().equals(productCode))
                .findFirst();

        if (existing.isPresent()) {
            SapCartEntry entry = existing.get();
            entry.setQuantity(entry.getQuantity() + quantity);
            entry.setTotalPrice(entry.getBasePrice() * entry.getQuantity());
        } else {
            SapCartEntry entry = new SapCartEntry();
            entry.setEntryNumber(cart.getEntries().size());
            entry.setProductCode(productCode);
            entry.setProductName(product.getName());
            entry.setQuantity(quantity);
            entry.setBasePrice(product.getPrice());
            entry.setTotalPrice(product.getPrice() * quantity);
            entry.setCurrency(product.getCurrency());
            cart.getEntries().add(entry);
        }

        recalculateCart(cart);
        return cart;
    }

    /** PUT /occ/v2/{site}/users/{userId}/carts/{cartId}/addresses/delivery */
    public SapCart setDeliveryAddress(String cartCode, SapCart.SapAddress address) {
        SapCart cart = carts.get(cartCode);
        if (cart == null) throw new NoSuchElementException("Cart not found: " + cartCode);
        cart.setDeliveryAddress(address);
        return cart;
    }

    /** PUT /occ/v2/{site}/users/{userId}/carts/{cartId}/deliverymode */
    public SapCart setDeliveryMode(String cartCode, String deliveryMode) {
        SapCart cart = carts.get(cartCode);
        if (cart == null) throw new NoSuchElementException("Cart not found: " + cartCode);
        cart.setDeliveryMode(deliveryMode);

        // Simulate delivery cost based on mode
        double cost = switch (deliveryMode) {
            case "standard" -> 5.99;
            case "express" -> 14.99;
            case "overnight" -> 24.99;
            default -> 9.99;
        };
        cart.setDeliveryCost(cost);
        recalculateCart(cart);
        return cart;
    }

    public Optional<SapCart> getCart(String cartCode) {
        return Optional.ofNullable(carts.get(cartCode));
    }

    // --- Order APIs (simulates POST /occ/v2/{site}/users/{userId}/orders) ---

    /** Place order from cart */
    public SapOrder placeOrder(String cartCode) {
        SapCart cart = carts.get(cartCode);
        if (cart == null) throw new NoSuchElementException("Cart not found: " + cartCode);
        if (cart.getEntries().isEmpty()) throw new IllegalStateException("Cart is empty");

        SapOrder order = new SapOrder();
        order.setCode("SAP-ORD-" + orderCounter.incrementAndGet());
        order.setStatus("CREATED");
        order.setCartCode(cartCode);
        order.setEntries(new ArrayList<>(cart.getEntries()));
        order.setTotalPrice(cart.getTotalPrice());
        order.setDeliveryCost(cart.getDeliveryCost());
        order.setTotalTax(cart.getTotalTax());
        order.setCurrency(cart.getCurrency());
        order.setDeliveryAddress(cart.getDeliveryAddress());
        order.setCreated(Instant.now());
        order.setStatusUpdated(Instant.now());

        orders.put(order.getCode(), order);

        // Remove cart after order placed
        carts.remove(cartCode);

        return order;
    }

    /** GET /occ/v2/{site}/users/{userId}/orders/{code} */
    public Optional<SapOrder> getOrder(String orderCode) {
        return Optional.ofNullable(orders.get(orderCode));
    }

    public List<SapOrder> getAllOrders() {
        return new ArrayList<>(orders.values());
    }

    // --- Internal helpers ---

    private void recalculateCart(SapCart cart) {
        double subtotal = cart.getEntries().stream()
                .mapToDouble(SapCartEntry::getTotalPrice)
                .sum();
        cart.setSubTotal(subtotal);

        // Simulate 8.25% tax (Texas rate as example)
        double tax = Math.round(subtotal * 0.0825 * 100.0) / 100.0;
        cart.setTotalTax(tax);

        cart.setTotalPrice(subtotal + cart.getDeliveryCost() + tax);
    }
}
