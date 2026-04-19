package com.sap.ucp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sap.ucp.model.sap.SapProduct;
import com.sap.ucp.model.ucp.Address;
import com.sap.ucp.model.ucp.CheckoutSession;
import com.sap.ucp.model.ucp.OrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class GeminiChatService {

    private static final Logger log = LoggerFactory.getLogger(GeminiChatService.class);
    private static final int MAX_TOOL_ROUNDS = 10;

    private final ObjectMapper objectMapper;
    private final CheckoutService checkoutService;
    private final OrderService orderService;
    private final WebClient webClient;

    @Value("${GEMINI_API_KEY:#{null}}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.5-flash}")
    private String model;

    // conversation history per chat session
    private final Map<String, List<Map<String, Object>>> conversations = new ConcurrentHashMap<>();

    public GeminiChatService(ObjectMapper objectMapper, CheckoutService checkoutService,
                             OrderService orderService) {
        this.objectMapper = objectMapper;
        this.checkoutService = checkoutService;
        this.orderService = orderService;
        this.webClient = WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public ChatResponse chat(String sessionId, String userMessage) {
        if (!isConfigured()) {
            return new ChatResponse("Gemini API key not configured. Set GEMINI_API_KEY environment variable.", List.of());
        }

        List<Map<String, Object>> history = conversations.computeIfAbsent(sessionId, k -> new ArrayList<>());

        // Add user message
        history.add(Map.of("role", "user", "parts", List.of(Map.of("text", userMessage))));

        List<ToolCallLog> toolLogs = new ArrayList<>();

        try {
            // Loop for function calling rounds
            for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
                JsonNode response = callGemini(history);

                JsonNode candidate = response.path("candidates").path(0);
                JsonNode content = candidate.path("content");
                JsonNode parts = content.path("parts");

                // Add model response to history
                Map<String, Object> modelMessage = objectMapper.convertValue(content, new TypeReference<>() {});
                history.add(modelMessage);

                // Check if there are function calls
                List<JsonNode> functionCalls = new ArrayList<>();
                String textResponse = null;

                for (JsonNode part : parts) {
                    if (part.has("functionCall")) {
                        functionCalls.add(part.get("functionCall"));
                    }
                    if (part.has("text")) {
                        textResponse = part.get("text").asText();
                    }
                }

                if (functionCalls.isEmpty()) {
                    // No function calls — return text response
                    return new ChatResponse(textResponse != null ? textResponse : "", toolLogs);
                }

                // Execute function calls and build response parts
                List<Map<String, Object>> functionResponseParts = new ArrayList<>();

                for (JsonNode fc : functionCalls) {
                    String funcName = fc.get("name").asText();
                    JsonNode args = fc.get("args");

                    log.info("Executing function: {} with args: {}", funcName, args);

                    Object result = executeFunction(funcName, args);
                    Map<String, Object> resultMap;
                    if (result instanceof Map) {
                        resultMap = (Map<String, Object>) result;
                    } else {
                        resultMap = objectMapper.convertValue(result, new TypeReference<>() {});
                    }

                    toolLogs.add(new ToolCallLog(funcName,
                            objectMapper.convertValue(args, new TypeReference<>() {}),
                            resultMap));

                    functionResponseParts.add(Map.of(
                            "functionResponse", Map.of(
                                    "name", funcName,
                                    "response", resultMap
                            )
                    ));
                }

                // Add function responses to history
                history.add(Map.of("role", "user", "parts", functionResponseParts));
            }

            return new ChatResponse("I've completed the maximum number of actions. Please continue with a new message.", toolLogs);

        } catch (Exception e) {
            log.error("Gemini API error", e);
            return new ChatResponse("Error communicating with AI: " + e.getMessage(), toolLogs);
        }
    }

    private JsonNode callGemini(List<Map<String, Object>> history) {
        ObjectNode requestBody = objectMapper.createObjectNode();

        // System instruction
        ObjectNode systemInstruction = objectMapper.createObjectNode();
        ArrayNode systemParts = objectMapper.createArrayNode();
        systemParts.add(objectMapper.createObjectNode().put("text",
                "You are a helpful AI shopping assistant for Nestle Online Store, powered by the Universal Commerce Protocol (UCP) — " +
                "an open standard that enables AI agents to interact with any commerce platform through a unified API. " +
                "You help customers browse products, add items to cart, set shipping details, and complete purchases. " +
                "Be friendly, concise, and proactive. When showing products, format them nicely. " +
                "Always use the available tools to interact with the store. " +
                "When listing products, organize them by category and show prices. " +
                "After adding items or completing actions, confirm what you did with a brief summary."));
        systemInstruction.set("parts", systemParts);
        requestBody.set("systemInstruction", systemInstruction);

        // Contents (conversation history)
        ArrayNode contents = objectMapper.valueToTree(history);
        requestBody.set("contents", contents);

        // Tools (function declarations)
        requestBody.set("tools", buildToolDeclarations());

        // Generation config
        ObjectNode genConfig = objectMapper.createObjectNode();
        genConfig.put("temperature", 0.7);
        genConfig.put("maxOutputTokens", 4096);
        requestBody.set("generationConfig", genConfig);

        String url = "/v1beta/models/" + model + ":generateContent?key=" + apiKey;

        String responseStr = webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        try {
            return objectMapper.readTree(responseStr);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Gemini response", e);
        }
    }

    private ArrayNode buildToolDeclarations() {
        ArrayNode tools = objectMapper.createArrayNode();
        ObjectNode toolObj = objectMapper.createObjectNode();
        ArrayNode functionDeclarations = objectMapper.createArrayNode();

        // discover_merchant
        functionDeclarations.add(buildFunction("discover_merchant",
                "Discover the merchant's UCP profile including capabilities, payment handlers, and identity configuration.",
                objectMapper.createObjectNode()));

        // list_products
        functionDeclarations.add(buildFunction("list_products",
                "List all available products from the Nestle Online Store catalog with prices, categories, and stock info.",
                objectMapper.createObjectNode()));

        // create_checkout_session
        functionDeclarations.add(buildFunction("create_checkout_session",
                "Create a new checkout session (shopping cart) to start adding items.",
                objectMapper.createObjectNode()));

        // add_item_to_cart
        ObjectNode addItemParams = objectMapper.createObjectNode();
        addItemParams.put("type", "OBJECT");
        ObjectNode addItemProps = objectMapper.createObjectNode();
        addItemProps.set("session_id", objectMapper.createObjectNode().put("type", "STRING").put("description", "The checkout session ID"));
        addItemProps.set("product_code", objectMapper.createObjectNode().put("type", "STRING").put("description", "The product code (e.g., NEST-011)"));
        addItemProps.set("quantity", objectMapper.createObjectNode().put("type", "INTEGER").put("description", "Quantity to add"));
        addItemParams.set("properties", addItemProps);
        addItemParams.set("required", objectMapper.createArrayNode().add("session_id").add("product_code").add("quantity"));
        functionDeclarations.add(buildFunction("add_item_to_cart",
                "Add a product to the shopping cart by product code and quantity.",
                addItemParams));

        // set_shipping_address
        ObjectNode addrParams = objectMapper.createObjectNode();
        addrParams.put("type", "OBJECT");
        ObjectNode addrProps = objectMapper.createObjectNode();
        addrProps.set("session_id", objectMapper.createObjectNode().put("type", "STRING").put("description", "The checkout session ID"));
        addrProps.set("name", objectMapper.createObjectNode().put("type", "STRING").put("description", "Recipient full name"));
        addrProps.set("street", objectMapper.createObjectNode().put("type", "STRING").put("description", "Street address"));
        addrProps.set("city", objectMapper.createObjectNode().put("type", "STRING").put("description", "City"));
        addrProps.set("state", objectMapper.createObjectNode().put("type", "STRING").put("description", "State or region"));
        addrProps.set("postal_code", objectMapper.createObjectNode().put("type", "STRING").put("description", "Postal/ZIP code"));
        addrProps.set("country", objectMapper.createObjectNode().put("type", "STRING").put("description", "Country code (e.g., US, IN)"));
        addrParams.set("properties", addrProps);
        addrParams.set("required", objectMapper.createArrayNode().add("session_id").add("name").add("street").add("city").add("state").add("postal_code").add("country"));
        functionDeclarations.add(buildFunction("set_shipping_address",
                "Set the shipping/delivery address for the checkout session.",
                addrParams));

        // set_delivery_method
        ObjectNode deliveryParams = objectMapper.createObjectNode();
        deliveryParams.put("type", "OBJECT");
        ObjectNode deliveryProps = objectMapper.createObjectNode();
        deliveryProps.set("session_id", objectMapper.createObjectNode().put("type", "STRING").put("description", "The checkout session ID"));
        deliveryProps.set("delivery_method", objectMapper.createObjectNode().put("type", "STRING").put("description", "Delivery method: standard ($5.99), express ($14.99), or overnight ($24.99)"));
        deliveryParams.set("properties", deliveryProps);
        deliveryParams.set("required", objectMapper.createArrayNode().add("session_id").add("delivery_method"));
        functionDeclarations.add(buildFunction("set_delivery_method",
                "Set the delivery/shipping method for the checkout session.",
                deliveryParams));

        // get_checkout_session
        ObjectNode getSessionParams = objectMapper.createObjectNode();
        getSessionParams.put("type", "OBJECT");
        ObjectNode getSessionProps = objectMapper.createObjectNode();
        getSessionProps.set("session_id", objectMapper.createObjectNode().put("type", "STRING").put("description", "The checkout session ID"));
        getSessionParams.set("properties", getSessionProps);
        getSessionParams.set("required", objectMapper.createArrayNode().add("session_id"));
        functionDeclarations.add(buildFunction("get_checkout_session",
                "Get the current state of a checkout session including items, pricing, and status.",
                getSessionParams));

        // complete_checkout
        ObjectNode completeParams = objectMapper.createObjectNode();
        completeParams.put("type", "OBJECT");
        ObjectNode completeProps = objectMapper.createObjectNode();
        completeProps.set("session_id", objectMapper.createObjectNode().put("type", "STRING").put("description", "The checkout session ID to complete"));
        completeParams.set("properties", completeProps);
        completeParams.set("required", objectMapper.createArrayNode().add("session_id"));
        functionDeclarations.add(buildFunction("complete_checkout",
                "Complete the checkout and place the order. Requires items in cart and a shipping address.",
                completeParams));

        // get_order_status
        ObjectNode orderParams = objectMapper.createObjectNode();
        orderParams.put("type", "OBJECT");
        ObjectNode orderProps = objectMapper.createObjectNode();
        orderProps.set("order_id", objectMapper.createObjectNode().put("type", "STRING").put("description", "The order ID (e.g., SAP-ORD-5001)"));
        orderParams.set("properties", orderProps);
        orderParams.set("required", objectMapper.createArrayNode().add("order_id"));
        functionDeclarations.add(buildFunction("get_order_status",
                "Get the status of a placed order including tracking information.",
                orderParams));

        toolObj.set("functionDeclarations", functionDeclarations);
        tools.add(toolObj);
        return tools;
    }

    private ObjectNode buildFunction(String name, String description, ObjectNode parameters) {
        ObjectNode func = objectMapper.createObjectNode();
        func.put("name", name);
        func.put("description", description);
        if (parameters.size() > 0) {
            func.set("parameters", parameters);
        }
        return func;
    }

    private Object executeFunction(String name, JsonNode args) {
        try {
            return switch (name) {
                case "discover_merchant" -> {
                    yield Map.of(
                            "store_name", "Nestle Online Store",
                            "protocol", "Universal Commerce Protocol (UCP)",
                            "capabilities", List.of("checkout", "order tracking", "identity linking"),
                            "payment_methods", List.of("Google Pay", "Credit Card"),
                            "delivery_options", Map.of("standard", "$5.99", "express", "$14.99", "overnight", "$24.99")
                    );
                }
                case "list_products" -> {
                    List<SapProduct> products = checkoutService.getAvailableProducts();
                    List<Map<String, Object>> productList = products.stream().map(p -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("code", p.getCode());
                        m.put("name", p.getName());
                        m.put("price", p.getPrice());
                        m.put("currency", p.getCurrency());
                        m.put("category", p.getCategory());
                        m.put("stock", p.getStock());
                        return m;
                    }).collect(Collectors.toList());
                    yield Map.of("products", productList, "total_count", productList.size());
                }
                case "create_checkout_session" -> {
                    CheckoutSession session = checkoutService.createSession();
                    yield objectMapper.convertValue(session, new TypeReference<Map<String, Object>>() {});
                }
                case "add_item_to_cart" -> {
                    String sessionId = args.get("session_id").asText();
                    String productCode = args.get("product_code").asText();
                    int quantity = args.get("quantity").asInt(1);
                    CheckoutSession session = checkoutService.addItem(sessionId, productCode, quantity);
                    yield objectMapper.convertValue(session, new TypeReference<Map<String, Object>>() {});
                }
                case "set_shipping_address" -> {
                    String sessionId = args.get("session_id").asText();
                    Address addr = new Address();
                    addr.setName(args.get("name").asText());
                    addr.setStreetAddress(args.get("street").asText());
                    addr.setCity(args.get("city").asText());
                    addr.setState(args.get("state").asText());
                    addr.setPostalCode(args.get("postal_code").asText());
                    addr.setCountry(args.get("country").asText());
                    CheckoutSession session = checkoutService.setAddress(sessionId, addr);
                    yield objectMapper.convertValue(session, new TypeReference<Map<String, Object>>() {});
                }
                case "set_delivery_method" -> {
                    String sessionId = args.get("session_id").asText();
                    String method = args.get("delivery_method").asText();
                    CheckoutSession session = checkoutService.setDeliveryMethod(sessionId, method);
                    yield objectMapper.convertValue(session, new TypeReference<Map<String, Object>>() {});
                }
                case "get_checkout_session" -> {
                    String sessionId = args.get("session_id").asText();
                    CheckoutSession session = checkoutService.getSession(sessionId)
                            .orElseThrow(() -> new NoSuchElementException("Session not found"));
                    yield objectMapper.convertValue(session, new TypeReference<Map<String, Object>>() {});
                }
                case "complete_checkout" -> {
                    String sessionId = args.get("session_id").asText();
                    CheckoutSession session = checkoutService.completeSession(sessionId);
                    yield objectMapper.convertValue(session, new TypeReference<Map<String, Object>>() {});
                }
                case "get_order_status" -> {
                    String orderId = args.get("order_id").asText();
                    OrderStatus status = orderService.getOrderStatus(orderId)
                            .orElseThrow(() -> new NoSuchElementException("Order not found"));
                    yield objectMapper.convertValue(status, new TypeReference<Map<String, Object>>() {});
                }
                default -> Map.of("error", "Unknown function: " + name);
            };
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }

    public void clearSession(String sessionId) {
        conversations.remove(sessionId);
    }

    // Response DTOs
    public record ChatResponse(String reply, List<ToolCallLog> toolCalls) {}
    public record ToolCallLog(String function, Map<String, Object> args, Map<String, Object> result) {}
}
