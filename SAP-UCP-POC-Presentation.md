# SAP Commerce Cloud x Universal Commerce Protocol (UCP)
## AI-Powered Shopping Experience — Proof of Concept

---

**Date:** March 2026
**Stack:** Spring Boot 3.2 | Java 17 | Gemini AI | UCP Protocol
**Merchant:** Nestle Online Store

---

## 1. Executive Summary

This POC demonstrates how **AI agents can autonomously shop on any commerce platform** using the **Universal Commerce Protocol (UCP)** — an open standard that provides a unified API layer across all commerce backends.

We built a **UCP facade over SAP Commerce Cloud (OCC APIs)** and connected it to **Google Gemini AI** to create an end-to-end AI shopping experience where customers simply chat in natural language to browse, cart, and purchase products.

**Key Outcome:** A customer types *"Buy me some coffee and KitKat, ship to Bangalore"* — and the AI agent discovers the store, finds products, builds a cart, sets shipping, and places the order — all through standardized UCP API calls.

---

## 2. What is UCP (Universal Commerce Protocol)?

UCP is an **open standard** that enables AI agents and platforms to interact with **any** commerce backend through a **unified, standardized API** — regardless of whether the backend is SAP, Shopify, Magento, or a custom solution.

### The Problem UCP Solves
| Without UCP | With UCP |
|-------------|----------|
| Every AI agent needs custom integration per commerce platform | One standard API works across all platforms |
| SAP OCC, Shopify API, Magento REST — all different | Single protocol: discovery, checkout, orders, identity |
| N platforms x M agents = N*M integrations | N + M integrations (each implements UCP once) |
| No standard way for AI to discover store capabilities | `/.well-known/ucp` — universal discovery endpoint |

### UCP Core Capabilities
- **Discovery** — AI agents find store capabilities via `/.well-known/ucp`
- **Checkout** — Create sessions, add items, set shipping, complete orders
- **Order Management** — Track order status, get tracking info
- **Identity Linking** — OAuth2-based authentication flow
- **Payment Handlers** — Standardized payment method advertisement

---

## 3. Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                    BROWSER (Chat UI)                     │
│  ┌───────────────────────┐  ┌────────────────────────┐  │
│  │   AI Chat Interface   │  │  UCP Protocol Inspector │  │
│  │  (Natural Language)   │  │  (Live API Call Viewer) │  │
│  └───────────┬───────────┘  └────────────────────────┘  │
└──────────────┼──────────────────────────────────────────┘
               │ POST /api/chat
               ▼
┌─────────────────────────────────────────────────────────┐
│               SPRING BOOT APPLICATION                    │
│                                                          │
│  ┌──────────────┐    ┌─────────────────────────────┐    │
│  │ ChatController│───▶│    GeminiChatService         │    │
│  │ /api/chat     │    │  • Calls Gemini API          │    │
│  └──────────────┘    │  • Function calling (9 tools) │    │
│                       │  • Conversation memory        │    │
│                       └──────────┬──────────────────┘    │
│                                  │ executes tools         │
│                                  ▼                        │
│  ┌──────────────────────────────────────────────────┐    │
│  │              UCP FACADE LAYER                     │    │
│  │                                                    │    │
│  │  UcpDiscovery     Checkout       Order    Identity │    │
│  │  Controller       Service        Service  Service  │    │
│  │  /.well-known/ucp /sessions      /orders  /identity│    │
│  └──────────────────────┬───────────────────────────┘    │
│                          │ maps UCP ↔ SAP                 │
│                          ▼                                │
│  ┌──────────────────────────────────────────────────┐    │
│  │           SAP OCC MOCK SERVICE                    │    │
│  │  • 100 Nestle products (13 categories)            │    │
│  │  • In-memory carts, orders                        │    │
│  │  • Simulates SAP Commerce Cloud OCC APIs          │    │
│  └──────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────┘
               │
               │ (In production, replaces mock with real HTTP calls)
               ▼
┌─────────────────────────────────────────────────────────┐
│          SAP COMMERCE CLOUD (OCC REST APIs)              │
│  GET  /occ/v2/{site}/products                            │
│  POST /occ/v2/{site}/users/anonymous/carts               │
│  POST /occ/v2/{site}/users/{id}/carts/{id}/entries       │
│  PUT  /occ/v2/{site}/users/{id}/carts/{id}/deliverymode  │
│  POST /occ/v2/{site}/users/{id}/orders                   │
└─────────────────────────────────────────────────────────┘
```

---

## 4. What We Built — Functional Overview

### 4.1 UCP Discovery Endpoint
**`GET /.well-known/ucp`**

Any AI agent hitting this URL instantly learns:
- Store name, ID, and version
- Supported capabilities (checkout, orders, identity)
- Available payment methods (Google Pay, Credit Card)
- OAuth2 endpoints for user authentication
- API base URL and spec location

This is the **entry point for all AI agents** — no prior configuration needed.

### 4.2 Product Catalog
**`GET /ucp/api/checkout/products`**

- **100 real Nestle products** across 13 categories
- Categories: Coffee (11), Chocolate (12), Noodles (9), Dairy (7), Ice Cream (6), Frozen Meals (8), Baking (5), Water (7), Baby Food (8), Cereals (7), Pet Care (8), Health (7), Beverages (5)
- Each product has: code, name, description, price (USD), stock count, image URL, category

### 4.3 Checkout Session Flow

| Step | UCP API Call | What Happens |
|------|-------------|--------------|
| 1. Create Cart | `POST /sessions` | Empty session created with unique ID |
| 2. Add Items | `PATCH /sessions/{id}` | Products added, pricing recalculated |
| 3. Set Address | `PATCH /sessions/{id}` | Shipping address attached |
| 4. Set Delivery | `PATCH /sessions/{id}` | Standard ($5.99) / Express ($14.99) / Overnight ($24.99) |
| 5. Place Order | `POST /sessions/{id}/complete` | Order created with ID, tracking number |

- Tax auto-calculated at 8.25%
- Session status transitions: `incomplete` → `ready` → `complete`
- Payment handlers advertised at every step (Google Pay, Credit Card)

### 4.4 Order Management
**`GET /ucp/api/orders/{id}`**

- Order status tracking: confirmed → processing → shipped → delivered
- Tracking number and URL provided
- Full order details: items, pricing, shipping address, timestamps

### 4.5 Identity Linking (OAuth2)
- **Authorization endpoint** — `GET /ucp/api/identity/authorize`
- **Token endpoint** — `POST /ucp/api/identity/token`
- **UserInfo endpoint** — `GET /ucp/api/identity/userinfo`
- Supports standard OAuth2 authorization code flow
- Auto-approves in POC (no login UI needed for demo)

### 4.6 AI Shopping Chat (New)
**`POST /api/chat`**

- Natural language shopping via Gemini AI
- AI autonomously calls UCP tools using function calling
- 9 tool functions: discover, list products, create session, add item, set address, set delivery, get session, complete checkout, get order status
- Conversation memory maintained per session
- Multi-turn function calling (up to 10 rounds per message)

---

## 5. What We Built — Technical Details

### 5.1 Technology Stack

| Layer | Technology | Purpose |
|-------|-----------|---------|
| Runtime | Java 17 (SapMachine) | Application runtime |
| Framework | Spring Boot 3.2.5 | REST API framework |
| HTTP Client | Spring WebFlux / WebClient | Calling Gemini API (non-blocking) |
| AI Engine | Google Gemini 2.5 Flash | Natural language + function calling |
| Serialization | Jackson (with JSR310) | JSON processing + date/time |
| Data Store | ConcurrentHashMap (in-memory) | Products, carts, orders |
| Frontend | Vanilla HTML/CSS/JS | Zero-dependency chat UI |
| Build | Maven | Dependency management + build |

### 5.2 Project Structure

```
sap-ucp-poc/
├── pom.xml
├── test-gemini-agent.py              # Terminal-based AI agent (Python)
├── SAP-UCP-POC-Presentation.md       # This document
│
├── src/main/java/com/sap/ucp/
│   ├── SapUcpApplication.java        # Spring Boot entry point
│   ├── config/
│   │   └── UcpProperties.java        # YAML config binding
│   ├── controller/
│   │   ├── UcpDiscoveryController.java    # /.well-known/ucp
│   │   ├── CheckoutController.java        # /ucp/api/checkout/*
│   │   ├── OrderController.java           # /ucp/api/orders/*
│   │   ├── IdentityLinkingController.java # /ucp/api/identity/*
│   │   └── ChatController.java           # /api/chat (NEW)
│   ├── service/
│   │   ├── CheckoutService.java       # UCP ↔ SAP checkout mapping
│   │   ├── OrderService.java          # UCP ↔ SAP order mapping
│   │   ├── IdentityService.java       # OAuth2 token management
│   │   └── GeminiChatService.java     # Gemini AI integration (NEW)
│   ├── mock/
│   │   └── SapOccMockService.java     # In-memory SAP OCC simulation
│   └── model/
│       ├── ucp/                       # UCP standard models
│       │   ├── CheckoutSession.java
│       │   ├── CheckoutItem.java
│       │   ├── OrderStatus.java
│       │   ├── Address.java
│       │   ├── UcpProfile.java
│       │   └── ...
│       └── sap/                       # SAP Commerce models
│           ├── SapProduct.java
│           ├── SapCart.java
│           ├── SapOrder.java
│           └── ...
│
├── src/main/resources/
│   ├── application.yml                # Server + merchant config
│   ├── mock-products.json             # 100 Nestle products
│   └── static/                        # Frontend (NEW)
│       ├── index.html                 # Chat UI page
│       ├── css/styles.css             # Dark theme styling
│       └── js/app.js                  # Chat logic + inspector
```

### 5.3 Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| **In-memory mock** instead of real SAP | Enables self-contained demo without SAP Commerce instance |
| **Function calling** instead of prompt-based | Gemini decides which APIs to call — true autonomous agent behavior |
| **Internal tool execution** (no HTTP loopback) | GeminiChatService calls CheckoutService directly — faster, no network overhead |
| **Vanilla JS frontend** | Zero build tools, no npm — just open browser. Keeps focus on UCP, not frontend framework |
| **Session-based conversations** | Each browser tab gets its own AI conversation and shopping cart |
| **Protocol Inspector sidebar** | Shows actual UCP API calls in real-time — proves the protocol works |

### 5.4 UCP-to-SAP Mapping Layer

The core value proposition — UCP provides a **standard interface**, our facade **translates** to SAP-specific APIs:

| UCP Concept | SAP OCC Equivalent |
|-------------|-------------------|
| `CheckoutSession` | `Cart` (anonymous user cart) |
| `session.items[]` | `cart.entries[]` (cart entries) |
| `session.shipping_address` | `cart.deliveryAddress` |
| `session.delivery_method` | `cart.deliveryMode` |
| `session.pricing.tax` | Calculated at 8.25% (configurable) |
| `completeSession()` | `placeOrder()` — converts cart to order |
| `OrderStatus.status: "confirmed"` | `SapOrder.status: "CREATED"` |
| `UcpProfile` | No direct SAP equivalent — UCP adds this |

### 5.5 AI Function Calling Flow

```
User: "Buy 2 KitKats and ship to Bangalore with express delivery"
                    │
                    ▼
            ┌── Gemini AI ──┐
            │  Understands   │
            │  intent, plans │
            │  tool calls    │
            └───────┬────────┘
                    │
        ┌───────────┼───────────────────┐
        ▼           ▼                   ▼
  create_session  add_item         set_address
  → session-abc   → NEST-011 x2   → Bangalore
        │           │                   │
        ▼           ▼                   ▼
  set_delivery   complete_checkout
  → express      → SAP-ORD-5001
        │           │
        ▼           ▼
  AI summarizes: "Done! Order SAP-ORD-5001 placed.
  2x KitKat ($29.98) + express shipping ($14.99)
  + tax ($2.47) = $47.44. Shipping to Bangalore."
```

Each step is a real UCP API call — visible in the Protocol Inspector.

---

## 6. API Endpoint Reference

### Discovery
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/.well-known/ucp` | Merchant UCP profile |

### Checkout
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/ucp/api/checkout/sessions` | Create checkout session |
| `GET` | `/ucp/api/checkout/sessions/{id}` | Get session state |
| `PATCH` | `/ucp/api/checkout/sessions/{id}` | Update session (add items, set address, set delivery) |
| `POST` | `/ucp/api/checkout/sessions/{id}/complete` | Place order |
| `GET` | `/ucp/api/checkout/products` | List all products |

### Orders
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/ucp/api/orders/{id}` | Get order status + tracking |

### Identity (OAuth2)
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/ucp/api/identity/authorize` | OAuth2 authorization |
| `POST` | `/ucp/api/identity/token` | Exchange code for token |
| `GET` | `/ucp/api/identity/userinfo` | Get authenticated user info |

### AI Chat
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/chat` | Send message, get AI response |
| `GET` | `/api/chat/status` | Check Gemini connection status |
| `POST` | `/api/chat/reset` | Clear conversation history |

---

## 7. Demo Script (Recommended Flow)

### Opening (30 seconds)
> "We've built a proof of concept showing how AI agents can shop on SAP Commerce Cloud using the Universal Commerce Protocol — an open standard for AI-driven commerce."

### Step 1: Show the Discovery Endpoint (30 seconds)
Open browser to `http://localhost:8080/.well-known/ucp`
> "Any AI agent can hit this single URL to discover everything about the store — capabilities, payment methods, API endpoints. No prior configuration needed."

### Step 2: Open the Chat UI (2 minutes)
Open `http://localhost:8080`
> "Now let's see a customer shopping through natural conversation."

**Type:** `What products do you have in coffee?`
- AI calls `list_products` → shows coffee products
- Point to Protocol Inspector: "Notice the UCP API call on the right"

**Type:** `Add Nescafe Gold Blend and 2 KitKat Variety Packs to my cart`
- AI calls `create_checkout_session`, then `add_item_to_cart` twice
- Inspector shows 3 API calls

**Type:** `Ship to 42 MG Road, Bangalore, Karnataka 560001, India`
- AI calls `set_shipping_address`

**Type:** `Use express delivery and place the order`
- AI calls `set_delivery_method` then `complete_checkout`
- Order confirmed with ID and tracking number

### Step 3: Key Takeaway (30 seconds)
> "The customer never saw a single API call. The AI agent autonomously used 7 UCP endpoints to complete this purchase. The same AI agent could work with Shopify, Magento, or any platform that implements UCP — zero code changes."

---

## 8. How to Run

### Prerequisites
- Java 17 (SapMachine or any JDK 17)
- Maven 3.x
- Google Gemini API key (for AI chat)

### Start the Application
```bash
cd sap-ucp-poc

# Set your Gemini API key
export GEMINI_API_KEY=your-api-key-here

# Set Java 17 (macOS)
export JAVA_HOME=$(/usr/libexec/java_home -v 17)

# Build and run
mvn spring-boot:run
```

### Access Points
| URL | What |
|-----|------|
| `http://localhost:8080` | AI Chat UI |
| `http://localhost:8080/.well-known/ucp` | UCP Discovery Profile |
| `http://localhost:8080/ucp/api/checkout/products` | Product Catalog (JSON) |

### Terminal-Based Agent (Alternative)
```bash
export GEMINI_API_KEY=your-api-key-here
python3 test-gemini-agent.py
# Select option 5 for interactive chat
```

---

## 9. Future Roadmap

| Phase | What | Impact |
|-------|------|--------|
| **Phase 2** | Replace mock with real SAP Commerce Cloud OCC calls | Production-ready backend |
| **Phase 3** | Multi-merchant support (Nestle + others on same UCP) | Platform-level value |
| **Phase 4** | Real payment processing (Stripe/Google Pay integration) | End-to-end transactions |
| **Phase 5** | Multi-agent support (Claude, GPT, Gemini — all work via UCP) | AI-agnostic commerce |
| **Phase 6** | Analytics dashboard — track AI agent shopping patterns | Business intelligence |

---

## 10. Key Metrics

| Metric | Value |
|--------|-------|
| UCP API Endpoints | 10 |
| Product Catalog | 100 products, 13 categories |
| AI Tool Functions | 9 |
| Lines of Java Code | ~1,200 |
| External Dependencies | 3 (Spring Web, WebFlux, Jackson) |
| Build Time | < 3 seconds |
| Startup Time | < 2 seconds |
| Frontend Dependencies | 0 (vanilla JS) |

---

*Built with Spring Boot 3.2.5 | Java 17 | Google Gemini 2.5 Flash | Universal Commerce Protocol*
