# Wipro Agentic Commerce Accelerator — Leadership Deck

---

## SLIDE 1: The Opportunity — What Is UCP & Why It Matters

### What Happened
- **Jan 11, 2026** — Google launched **Universal Commerce Protocol (UCP)** at NRF Retail's Big Show
- **Feb 2026** — OpenAI launched **Agent Commerce Protocol (ACP)** with Stripe
- **Jan 2026** — Microsoft launched **Copilot Checkout** with Shopify

### What Is UCP
An **open standard** that lets AI agents (Gemini, ChatGPT, Copilot, Claude) **shop on behalf of users** — browse products, add to cart, checkout, track orders — all through conversation.

### Think of It This Way
| Era | How Users Shop | Standard |
|-----|---------------|----------|
| 2000s | Type URLs in browser | HTTP |
| 2010s | Tap apps on phone | REST APIs |
| **2026** | **Talk to AI agents** | **UCP / ACP** |

### The Business Threat
> If Dyson doesn't support these protocols, when a customer asks Gemini _"buy me a Dyson V15"_, the AI will redirect to a competitor who does.

### Who's Already In
Google, Shopify, Walmart, Target, Etsy, Wayfair, Visa, Mastercard, Stripe, Adyen, Best Buy, Flipkart, Macy's, The Home Depot, Zalando — 20+ global partners.

### Key Clarification for Leadership
| Question | Answer |
|----------|--------|
| Does Google provide the API? | **No.** Merchants expose APIs. Google's Gemini calls them. |
| Do we integrate with each AI? | **No.** We expose endpoints. AI platforms call us. Like building a website — you don't integrate with Chrome. |
| Does every AI use UCP? | **No.** Google uses UCP, OpenAI uses ACP, Microsoft uses Copilot Checkout. But the underlying work is 90% the same — just different field names. |
| Where does user context live? | Google side: identity, payment (Google Wallet). Merchant side: orders, loyalty. Bridged via OAuth2 identity linking. |
| What if user isn't logged in? | Can browse products. Can't checkout (no payment/identity). Degrades gracefully to merchant website. |

---

## SLIDE 2: What Wipro Builds — The Accelerator

### What It Is
A **thin API layer** between AI shopping platforms and the client's existing e-commerce backend (SAP / Salesforce / Shopify). No business logic. No touching the commerce platform.

### Architecture (Simple)
```
Google Gemini / ChatGPT / Claude
         │
         │  call our endpoints (HTTPS)
         ▼
┌─────────────────────────────┐
│   WIPRO ACCELERATOR         │
│                             │
│   • 5-6 REST endpoints      │
│   • Rename JSON fields      │
│   • Call backend APIs        │
│   • Track sessions (Redis)  │
│   • ~500 lines of code      │
│                             │
│   NO business logic.        │
│   NO pricing/tax/discounts. │
│   Just translate & forward. │
└──────────────┬──────────────┘
               │
               │  HTTP calls to existing APIs
               ▼
┌─────────────────────────────┐
│   CLIENT'S SAP COMMERCE     │
│   (untouched)               │
│                             │
│   All business rules here:  │
│   pricing, tax, stock,      │
│   promotions, fulfillment   │
└─────────────────────────────┘
```

### What the Accelerator Does vs Doesn't Do

| Accelerator Does | Accelerator Does NOT Do |
|-----------------|------------------------|
| Expose UCP/ACP/MCP endpoints | Calculate pricing |
| Translate field names between protocols | Apply discounts or promotions |
| Route requests to SAP OCC APIs | Validate inventory rules |
| Track session-to-cart mapping | Determine shipping costs |
| Handle OAuth2 identity linking flow | Store customer data |
| Return responses in correct protocol format | Make any business decisions |

### Why Not MuleSoft / Boomi / iPaaS?
| Factor | Custom API | MuleSoft |
|--------|-----------|----------|
| What we're building | 5 REST endpoints + field mapping | Same |
| License cost | $0 | $50-100K/year |
| Infrastructure cost | ~$1K/month | + MuleSoft license |
| Time to deliver | 3-4 weeks | 4-6 weeks |
| SAP connection | HTTP calls to OCC REST APIs | Same (OCC is already REST) |
| Verdict | **Right-sized for the job** | Overkill — using a truck to carry a backpack |

> **Use MuleSoft only if the client already has it.** Don't buy a $100K license for what is a simple API application.

### The Restaurant Analogy (for non-technical leadership)
> Uber Eats, DoorDash, Zomato each have different order formats. Does the restaurant build a different kitchen for each? **No.** One kitchen, one tablet that receives orders from all platforms, translates them to kitchen tickets. Our accelerator is that tablet.

---

## SLIDE 3: Wipro's Reusable IP — The Accelerator Kit

### What Wipro Delivers (Reusable Across All Clients)

| # | Deliverable | What It Is | Why It Matters |
|---|------------|-----------|----------------|
| 1 | **API Specifications** | OpenAPI definitions for UCP, ACP, MCP — maintained as Google/OpenAI update specs | Client doesn't read 200-page protocol specs |
| 2 | **Canonical Data Model** | Standardized field names that sit between all protocols and all backends | Add new protocol = 1 new mapping, not N |
| 3 | **Mapping Sheets** | Field-by-field transformation rules per protocol and per backend | The core IP — "city" vs "town" vs "locality" solved once |
| 4 | **Flow Templates** | Step-by-step orchestration blueprints (create cart → add items → set address → complete) | Implement on any platform in days |
| 5 | **Discovery Profile Template** | UCP `/.well-known/ucp` generator — client fills config, we generate | 1-day setup per client |
| 6 | **Test Suite + AI Agent Simulator** | Automated protocol compliance tests + simulated Gemini/ChatGPT agent | Validate in hours, not weeks |

### The Mapping Problem We Solve (Example: Address)

The same address field has **different names** in every system:

| Field | Google UCP | OpenAI ACP | SAP Commerce | Salesforce |
|-------|-----------|-----------|-------------|-----------|
| Name | `name` | `full_name` | `firstName` + `lastName` | `full_name` |
| Street | `street_address` | `address1` | `line1` | `address1` |
| City | `city` | `city` | `town` | `city` |
| State | `state` | `state` | `region.isocode` | `state_code` |
| Zip | `postal_code` | `zip` | `postalCode` | `postal_code` |
| Country | `country` | `country_code` | `country.isocode` | `country_code` |

**Wipro maps all of these once. Every new client reuses the same mapping sheets.**

### Reusability Model

```
Client 1 (Dyson / SAP):      Use SAP mapping sheet + UCP/ACP specs    → 3-4 weeks
Client 2 (Nike / Salesforce): Use SFCC mapping sheet + same specs      → 2-3 weeks
Client 3 (Unilever / SAP):   Use same SAP mapping + same specs        → 1-2 weeks
Client 4 (Any / Shopify):    Use Shopify mapping + same specs         → 1-2 weeks

Each new client is FASTER because the knowledge compounds.
```

### What Changes Per Client (Only Configuration)
```yaml
# dyson.yml — the ONLY client-specific file
client:
  business_name: "Dyson"
  business_id: "dyson-official"
  base_url: "https://agentic.dyson.com"
  backend: "sap"
  sap_occ_url: "https://sap-commerce.dyson.internal/occ/v2/dyson"
  google_pay_merchant_id: "BCR2DN4T7DYSON"
  payment_gateway: "stripe"
```

---

## SLIDE 4: Deployment, Commercials & Go-To-Market

### Where It Gets Deployed
Deployed on the **client's existing cloud**, same network as their SAP Commerce:

| If Client's SAP Is On | Deploy Accelerator On | Connection |
|----------------------|----------------------|------------|
| Azure | Azure (AKS / Container Apps) | Private VNet peering |
| AWS | AWS (ECS / EKS) | VPC private link |
| GCP | GCP (Cloud Run / GKE) | VPC peering |
| SAP BTP | SAP BTP (Cloud Foundry / Kyma) | SAP Destination Service |
| On-premise | Any cloud + VPN | Site-to-site VPN |

**Rule: Deploy as close to SAP as possible for lowest latency.**

### Infrastructure Cost
| Component | Monthly Cost |
|-----------|-------------|
| Containers (2-3 replicas) | $300-500 |
| Redis (session cache) | $100-200 |
| API Gateway | $100-200 |
| Monitoring | $50-100 |
| **Total** | **~$700-1,000/month** |

> Negligible vs. SAP Commerce license ($500K+/year) or MuleSoft ($100K/year).

### Engagement Model for Dyson (First Client)

| Phase | Duration | Deliverable | Wipro Effort |
|-------|----------|-------------|-------------|
| **Phase 1** — UCP (Google/Gemini) | 4 weeks | UCP endpoints live, Google Merchant Center registered | 2 developers |
| **Phase 2** — ACP (OpenAI/ChatGPT) | 2 weeks | ACP endpoints (reuse 90% of Phase 1) | 1 developer |
| **Phase 3** — MCP (Claude/Others) | 2 weeks | MCP server | 1 developer |
| **Phase 4** — Hardening & Go-Live | 2 weeks | Security, monitoring, load testing | 2 developers |
| **Ongoing** — Maintenance | Monthly | Spec updates, monitoring, support | 0.5 FTE |

**Total: ~10 weeks to full multi-platform agentic commerce.**

### Commercials (Indicative)

| Item | One-Time | Recurring |
|------|---------|-----------|
| Accelerator build (Dyson) | $80-120K | — |
| Infra (client pays) | — | ~$1K/month |
| Maintenance & support | — | $5-8K/month |
| **Each subsequent client** | **$30-50K** | **$5-8K/month** |

> Subsequent clients are 60-70% cheaper because all specs, mappings, and templates are already built.

### Wipro's Strategic Play

```
SHORT TERM (2026):
  Land Dyson as first client.
  Build the accelerator kit.
  Prove it works end-to-end.

MEDIUM TERM (2026-27):
  Sell to 5-10 more retail/CPG clients.
  Each deployment is faster and cheaper.
  Build connector library (SAP, SFCC, Shopify, Magento).

LONG TERM (2027+):
  "Wipro Agentic Commerce Practice"
  — Dedicated team, reusable IP, industry recognition.
  — Every Wipro retail client is a potential buyer.
  — Recurring maintenance revenue from all clients.
```

### The One-Liner Pitch

> _"We make your products buyable through every AI assistant — Gemini, ChatGPT, Copilot — in 10 weeks, without changing your SAP Commerce setup. We've already built the prototype."_

---

## APPENDIX: Anticipated Leadership Questions

| Question | Answer |
|----------|--------|
| Is UCP a Google-only thing? | Open standard, but Google drives it. OpenAI has ACP (similar). Both are emerging. |
| Will one protocol win? | Too early. Like VHS vs Betamax. Wipro's kit supports all — we don't bet on one. |
| What if Dyson already has Shopify? | Shopify is building native UCP support. But many SAP/SFCC clients need custom integration — that's our market. |
| What about security? | Payment never touches our layer (Google Pay / Stripe handle it). We're just a pass-through for product/cart data. |
| What if protocols change? | That's the maintenance contract. We update mapping sheets and endpoints. ~0.5 FTE ongoing. |
| Can't SAP build this themselves? | They will eventually. But clients need it NOW. We bridge the gap. First-mover advantage. |
| Why not just use SAP CX tools? | SAP doesn't have UCP/ACP adapters yet. Their roadmap is 12-18 months out. We deliver in 10 weeks. |
| What's Wipro's IP here? | The mapping sheets, canonical model, flow templates, and accelerator code. Platform-agnostic, reusable across clients. |
| ROI for Dyson? | New sales channel (AI commerce projected $385B by 2028). Cost: ~$150K one-time + $6K/month. One Dyson V15 sale through AI covers a month of maintenance. |
