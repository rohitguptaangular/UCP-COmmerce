package com.sap.ucp.service;

import com.sap.ucp.config.UcpProperties;
import com.sap.ucp.mock.SapOccMockService;
import com.sap.ucp.model.sap.SapCart;
import com.sap.ucp.model.sap.SapCartEntry;
import com.sap.ucp.model.sap.SapOrder;
import com.sap.ucp.model.sap.SapProduct;
import com.sap.ucp.model.ucp.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Orchestrates UCP Checkout <-> SAP Commerce OCC mapping.
 * Maps UCP checkout session operations to SAP cart/order operations.
 */
@Service
public class CheckoutService {

    private final SapOccMockService sapService;
    private final UcpProperties properties;

    // Maps UCP session ID -> SAP cart code
    private final Map<String, String> sessionToCart = new ConcurrentHashMap<>();

    public CheckoutService(SapOccMockService sapService, UcpProperties properties) {
        this.sapService = sapService;
        this.properties = properties;
    }

    /**
     * Create a new checkout session.
     * UCP: POST /ucp/api/checkout/sessions
     * SAP:  POST /occ/v2/{site}/users/anonymous/carts
     */
    public CheckoutSession createSession() {
        SapCart cart = sapService.createCart();

        String sessionId = "ucp-session-" + UUID.randomUUID().toString().substring(0, 8);
        sessionToCart.put(sessionId, cart.getCode());

        return mapCartToSession(sessionId, cart, "incomplete");
    }

    /**
     * Update a checkout session (add items, set address, set delivery).
     * UCP: PATCH /ucp/api/checkout/sessions/{id}
     * SAP:  Multiple OCC cart endpoints
     */
    public CheckoutSession updateSession(String sessionId, CheckoutSession updates) {
        String cartCode = sessionToCart.get(sessionId);
        if (cartCode == null) throw new NoSuchElementException("Session not found: " + sessionId);

        // Add items if provided
        if (updates.getItems() != null) {
            for (CheckoutItem item : updates.getItems()) {
                sapService.addToCart(cartCode, item.getProductId(), item.getQuantity());
            }
        }

        // Set shipping address if provided
        if (updates.getShippingAddress() != null) {
            SapCart.SapAddress sapAddr = mapUcpAddressToSap(updates.getShippingAddress());
            sapService.setDeliveryAddress(cartCode, sapAddr);
        }

        // Set delivery method if provided
        if (updates.getDeliveryMethod() != null) {
            sapService.setDeliveryMode(cartCode, updates.getDeliveryMethod());
        }

        SapCart cart = sapService.getCart(cartCode)
                .orElseThrow(() -> new NoSuchElementException("Cart lost: " + cartCode));

        // Determine status: "ready" if has items + address, otherwise "incomplete"
        String status = (cart.getEntries().size() > 0 && cart.getDeliveryAddress() != null)
                ? "ready" : "incomplete";

        return mapCartToSession(sessionId, cart, status);
    }

    /**
     * Complete the checkout (place order).
     * UCP: POST /ucp/api/checkout/sessions/{id}/complete
     * SAP:  POST /occ/v2/{site}/users/{userId}/orders
     */
    public CheckoutSession completeSession(String sessionId) {
        String cartCode = sessionToCart.get(sessionId);
        if (cartCode == null) throw new NoSuchElementException("Session not found: " + sessionId);

        SapOrder order = sapService.placeOrder(cartCode);
        sessionToCart.remove(sessionId);

        CheckoutSession session = new CheckoutSession();
        session.setId(sessionId);
        session.setStatus("complete");
        session.setOrderId(order.getCode());
        session.setItems(order.getEntries().stream().map(this::mapSapEntryToItem).collect(Collectors.toList()));

        CheckoutSession.PriceSummary pricing = new CheckoutSession.PriceSummary();
        pricing.setSubtotal(order.getTotalPrice() - order.getDeliveryCost() - order.getTotalTax());
        pricing.setShipping(order.getDeliveryCost());
        pricing.setTax(order.getTotalTax());
        pricing.setTotal(order.getTotalPrice());
        pricing.setCurrency(order.getCurrency());
        session.setPricing(pricing);

        session.setUcp(buildUcpMeta());
        return session;
    }

    /**
     * Add a single item to the checkout session.
     */
    public CheckoutSession addItem(String sessionId, String productCode, int quantity) {
        String cartCode = sessionToCart.get(sessionId);
        if (cartCode == null) throw new NoSuchElementException("Session not found: " + sessionId);

        sapService.addToCart(cartCode, productCode, quantity);

        SapCart cart = sapService.getCart(cartCode)
                .orElseThrow(() -> new NoSuchElementException("Cart lost: " + cartCode));
        String status = (cart.getEntries().size() > 0 && cart.getDeliveryAddress() != null)
                ? "ready" : "incomplete";
        return mapCartToSession(sessionId, cart, status);
    }

    /**
     * Set the shipping address on a checkout session.
     */
    public CheckoutSession setAddress(String sessionId, Address addr) {
        String cartCode = sessionToCart.get(sessionId);
        if (cartCode == null) throw new NoSuchElementException("Session not found: " + sessionId);

        SapCart.SapAddress sapAddr = mapUcpAddressToSap(addr);
        sapService.setDeliveryAddress(cartCode, sapAddr);

        SapCart cart = sapService.getCart(cartCode)
                .orElseThrow(() -> new NoSuchElementException("Cart lost: " + cartCode));
        String status = (cart.getEntries().size() > 0 && cart.getDeliveryAddress() != null)
                ? "ready" : "incomplete";
        return mapCartToSession(sessionId, cart, status);
    }

    /**
     * Set the delivery method on a checkout session.
     */
    public CheckoutSession setDeliveryMethod(String sessionId, String method) {
        String cartCode = sessionToCart.get(sessionId);
        if (cartCode == null) throw new NoSuchElementException("Session not found: " + sessionId);

        sapService.setDeliveryMode(cartCode, method);

        SapCart cart = sapService.getCart(cartCode)
                .orElseThrow(() -> new NoSuchElementException("Cart lost: " + cartCode));
        String status = (cart.getEntries().size() > 0 && cart.getDeliveryAddress() != null)
                ? "ready" : "incomplete";
        return mapCartToSession(sessionId, cart, status);
    }

    public Optional<CheckoutSession> getSession(String sessionId) {
        String cartCode = sessionToCart.get(sessionId);
        if (cartCode == null) return Optional.empty();

        return sapService.getCart(cartCode)
                .map(cart -> {
                    String status = (cart.getEntries().size() > 0 && cart.getDeliveryAddress() != null)
                            ? "ready" : "incomplete";
                    return mapCartToSession(sessionId, cart, status);
                });
    }

    public List<SapProduct> getAvailableProducts() {
        return sapService.getAllProducts();
    }

    // --- Mapping helpers ---

    private CheckoutSession mapCartToSession(String sessionId, SapCart cart, String status) {
        CheckoutSession session = new CheckoutSession();
        session.setId(sessionId);
        session.setStatus(status);
        session.setItems(cart.getEntries().stream().map(this::mapSapEntryToItem).collect(Collectors.toList()));
        session.setDeliveryMethod(cart.getDeliveryMode());

        if (cart.getDeliveryAddress() != null) {
            session.setShippingAddress(mapSapAddressToUcp(cart.getDeliveryAddress()));
        }

        CheckoutSession.PriceSummary pricing = new CheckoutSession.PriceSummary();
        pricing.setSubtotal(cart.getSubTotal());
        pricing.setShipping(cart.getDeliveryCost());
        pricing.setTax(cart.getTotalTax());
        pricing.setTotal(cart.getTotalPrice());
        pricing.setCurrency(cart.getCurrency());
        session.setPricing(pricing);

        // Advertise payment handlers
        CheckoutSession.PaymentInfo paymentInfo = new CheckoutSession.PaymentInfo();
        paymentInfo.setHandlers(List.of(
                new PaymentHandler("google-pay", "Google Pay",
                        Map.of("merchant_id", properties.getMerchant().getId(), "gateway", "stripe"),
                        List.of("CARD", "TOKENIZED_CARD")),
                new PaymentHandler("credit-card", "Credit Card",
                        Map.of("gateway", "stripe"),
                        List.of("CARD"))
        ));
        session.setPayment(paymentInfo);

        session.setUcp(buildUcpMeta());
        return session;
    }

    private CheckoutItem mapSapEntryToItem(SapCartEntry entry) {
        CheckoutItem item = new CheckoutItem();
        item.setProductId(entry.getProductCode());
        item.setName(entry.getProductName());
        item.setQuantity(entry.getQuantity());
        item.setUnitPrice(entry.getBasePrice());
        item.setCurrency(entry.getCurrency());

        sapService.getProduct(entry.getProductCode())
                .ifPresent(p -> item.setImageUrl(p.getImageUrl()));
        return item;
    }

    private SapCart.SapAddress mapUcpAddressToSap(Address addr) {
        SapCart.SapAddress sapAddr = new SapCart.SapAddress();
        if (addr.getName() != null) {
            String[] parts = addr.getName().split(" ", 2);
            sapAddr.setFirstName(parts[0]);
            sapAddr.setLastName(parts.length > 1 ? parts[1] : "");
        }
        sapAddr.setLine1(addr.getStreetAddress());
        sapAddr.setTown(addr.getCity());
        sapAddr.setRegion(addr.getState());
        sapAddr.setPostalCode(addr.getPostalCode());
        sapAddr.setCountry(addr.getCountry());
        sapAddr.setPhone(addr.getPhone());
        return sapAddr;
    }

    private Address mapSapAddressToUcp(SapCart.SapAddress sapAddr) {
        Address addr = new Address();
        addr.setName((sapAddr.getFirstName() + " " + sapAddr.getLastName()).trim());
        addr.setStreetAddress(sapAddr.getLine1());
        addr.setCity(sapAddr.getTown());
        addr.setState(sapAddr.getRegion());
        addr.setPostalCode(sapAddr.getPostalCode());
        addr.setCountry(sapAddr.getCountry());
        addr.setPhone(sapAddr.getPhone());
        return addr;
    }

    private CheckoutSession.UcpMeta buildUcpMeta() {
        CheckoutSession.UcpMeta meta = new CheckoutSession.UcpMeta();
        meta.setVersion(properties.getMerchant().getVersion());
        meta.setCapabilities(List.of(
                new Capability("dev.ucp.shopping.checkout", properties.getMerchant().getVersion())
        ));
        return meta;
    }
}
