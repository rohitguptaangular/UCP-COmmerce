package com.sap.ucp.controller;

import com.sap.ucp.model.sap.SapProduct;
import com.sap.ucp.model.ucp.Address;
import com.sap.ucp.model.ucp.CheckoutItem;
import com.sap.ucp.model.ucp.CheckoutSession;
import com.sap.ucp.service.CheckoutService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * UCP Checkout Capability endpoints.
 *
 * Implements the three core checkout operations:
 * 1. Create Session  -> maps to SAP Cart creation
 * 2. Update Session  -> maps to SAP Cart modifications (add items, set address, delivery mode)
 * 3. Complete Session -> maps to SAP Order placement
 *
 * SAP Commerce OCC equivalents:
 *   POST   /occ/v2/{site}/users/anonymous/carts
 *   PATCH  /occ/v2/{site}/users/{userId}/carts/{cartId}/entries
 *   POST   /occ/v2/{site}/users/{userId}/orders
 */
@RestController
@RequestMapping("/ucp/api/checkout")
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    /**
     * Create a new checkout session.
     * UCP: POST /ucp/api/checkout/sessions
     */
    @PostMapping("/sessions")
    public ResponseEntity<CheckoutSession> createSession() {
        CheckoutSession session = checkoutService.createSession();
        return ResponseEntity.status(HttpStatus.CREATED).body(session);
    }

    /**
     * Get an existing checkout session.
     * UCP: GET /ucp/api/checkout/sessions/{id}
     */
    @GetMapping("/sessions/{id}")
    public ResponseEntity<CheckoutSession> getSession(@PathVariable String id) {
        return checkoutService.getSession(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update a checkout session (add items, set address, set delivery method).
     * UCP: PATCH /ucp/api/checkout/sessions/{id}
     *
     * Supports action-based requests:
     *   {"action": "add_item", "productCode": "NEST-011", "quantity": 1}
     *   {"action": "set_address", "address": {"name": "...", "street": "...", ...}}
     *   {"action": "set_delivery", "deliveryMethod": "standard"}
     *
     * Also supports batch updates:
     *   {"items": [{"product_id": "NEST-011", "quantity": 1}], "shipping_address": {...}, "delivery_method": "standard"}
     */
    @SuppressWarnings("unchecked")
    @PatchMapping("/sessions/{id}")
    public ResponseEntity<?> updateSession(@PathVariable String id, @RequestBody Map<String, Object> body) {
        try {
            String action = (String) body.get("action");

            if (action != null) {
                // Action-based format from AI agents
                switch (action) {
                    case "add_item": {
                        String productCode = (String) body.get("productCode");
                        int quantity = body.get("quantity") instanceof Number
                                ? ((Number) body.get("quantity")).intValue() : 1;
                        CheckoutSession session = checkoutService.addItem(id, productCode, quantity);
                        return ResponseEntity.ok(session);
                    }
                    case "set_address": {
                        Map<String, String> addrMap = (Map<String, String>) body.get("address");
                        Address addr = new Address();
                        addr.setName(addrMap.get("name"));
                        addr.setStreetAddress(addrMap.getOrDefault("street", addrMap.get("streetAddress")));
                        addr.setCity(addrMap.get("city"));
                        addr.setState(addrMap.get("state"));
                        addr.setPostalCode(addrMap.getOrDefault("postalCode", addrMap.get("postal_code")));
                        addr.setCountry(addrMap.get("country"));
                        addr.setPhone(addrMap.get("phone"));
                        CheckoutSession session = checkoutService.setAddress(id, addr);
                        return ResponseEntity.ok(session);
                    }
                    case "set_delivery": {
                        String method = (String) body.get("deliveryMethod");
                        CheckoutSession session = checkoutService.setDeliveryMethod(id, method);
                        return ResponseEntity.ok(session);
                    }
                    default:
                        return ResponseEntity.badRequest().body(Map.of("error", "Unknown action: " + action));
                }
            } else {
                // Batch update format
                CheckoutSession updates = new CheckoutSession();
                if (body.containsKey("items")) {
                    List<Map<String, Object>> itemsList = (List<Map<String, Object>>) body.get("items");
                    List<CheckoutItem> items = new java.util.ArrayList<>();
                    for (Map<String, Object> itemMap : itemsList) {
                        CheckoutItem item = new CheckoutItem();
                        item.setProductId((String) itemMap.getOrDefault("product_id", itemMap.get("productId")));
                        item.setQuantity(itemMap.get("quantity") instanceof Number
                                ? ((Number) itemMap.get("quantity")).intValue() : 1);
                        items.add(item);
                    }
                    updates.setItems(items);
                }
                if (body.containsKey("shipping_address")) {
                    Map<String, String> addrMap = (Map<String, String>) body.get("shipping_address");
                    Address addr = new Address();
                    addr.setName(addrMap.get("name"));
                    addr.setStreetAddress(addrMap.getOrDefault("street", addrMap.get("street_address")));
                    addr.setCity(addrMap.get("city"));
                    addr.setState(addrMap.get("state"));
                    addr.setPostalCode(addrMap.getOrDefault("postal_code", addrMap.get("postalCode")));
                    addr.setCountry(addrMap.get("country"));
                    addr.setPhone(addrMap.get("phone"));
                    updates.setShippingAddress(addr);
                }
                if (body.containsKey("delivery_method")) {
                    updates.setDeliveryMethod((String) body.get("delivery_method"));
                }
                CheckoutSession session = checkoutService.updateSession(id, updates);
                return ResponseEntity.ok(session);
            }
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Complete the checkout session (place order).
     * UCP: POST /ucp/api/checkout/sessions/{id}/complete
     */
    @PostMapping("/sessions/{id}/complete")
    public ResponseEntity<?> completeSession(@PathVariable String id) {
        try {
            CheckoutSession session = checkoutService.completeSession(id);
            return ResponseEntity.ok(session);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * List available products (helper endpoint for demo/testing).
     * Not part of UCP spec — maps to SAP OCC product search.
     */
    @GetMapping("/products")
    public List<SapProduct> listProducts() {
        return checkoutService.getAvailableProducts();
    }
}
