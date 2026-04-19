# SAP Commerce Cloud x Universal Commerce Protocol (UCP) — POC

This is a proof-of-concept that shows how AI agents can shop on any commerce platform using the Universal Commerce Protocol (UCP). Built a UCP facade over SAP Commerce Cloud OCC APIs and connected it with Google Gemini AI for a conversational shopping experience.

The idea is simple — a user types something like "Buy me some coffee and KitKat, ship to Bangalore" and the AI agent handles everything: discovers the store, finds products, builds a cart, sets shipping, and places the order. All through standardized UCP API calls.

## Tech Stack

- Java 17
- Spring Boot 3.2
- Google Gemini AI (function calling with 9 tools)
- SAP Commerce Cloud OCC (mocked)
- UCP Protocol

## What's in Here

```
src/main/java/com/sap/ucp/
├── controller/          # REST endpoints (Chat, Checkout, Orders, Identity, UCP Discovery)
├── service/             # Business logic (Gemini AI, Checkout, Orders, Identity)
├── model/               # UCP and SAP data models
├── mock/                # SAP OCC mock service (100 Nestle products)
└── config/              # App configuration

src/main/resources/
├── application.yml      # Server and UCP merchant config
├── mock-products.json   # 100 Nestle products across 13 categories
└── static/              # Chat UI (HTML/CSS/JS)
```

## How to Run

You need Java 17+ installed. Then:

```bash
chmod +x setup-and-run.sh
./setup-and-run.sh
```

Or manually:
```bash
./mvnw clean package -DskipTests
./mvnw spring-boot:run
```

App starts at http://localhost:8080

## API Endpoints

**UCP Discovery:**
```
GET /.well-known/ucp
```

**Chat (AI agent):**
```
POST /api/chat
Body: { "message": "show me coffee products" }
```

**Checkout:**
```
POST /ucp/api/checkout/sessions          # create session
GET  /ucp/api/checkout/products          # list products
POST /ucp/api/checkout/sessions/{id}/items  # add items
POST /ucp/api/checkout/sessions/{id}/complete  # place order
```

**Orders:**
```
GET /ucp/api/orders/{id}                 # order status
```

## How UCP Works

UCP is an open standard that lets AI agents talk to any commerce backend through one unified API. Instead of building separate integrations for SAP, Shopify, Magento etc., you implement UCP once and any AI agent can interact with your store.

The flow:
1. AI agent hits `/.well-known/ucp` to discover what the store can do
2. Creates a checkout session
3. Searches and adds products
4. Sets shipping details
5. Completes the order

In this POC, the actual SAP Commerce Cloud APIs are mocked since we didn't have a live SAP instance. The mock service has 100 Nestle products across 13 categories.

## Presentations

- `Wipro-Agentic-Commerce-Demo.pptx` — demo walkthrough
- `Wipro-Agentic-Commerce-Technical.pptx` — technical deep dive
- `Wipro-Agentic-Commerce-Leadership.pptx` — leadership deck
