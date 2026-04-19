#!/usr/bin/env python3
"""Generate a clean architecture diagram for Wipro Agentic Commerce Accelerator."""

import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
from matplotlib.patches import FancyBboxPatch, FancyArrowPatch

fig, ax = plt.subplots(1, 1, figsize=(20, 14))
ax.set_xlim(0, 20)
ax.set_ylim(0, 14)
ax.axis('off')
fig.patch.set_facecolor('white')

# ── Colors ──
WIPRO = '#5A2FC2'
WIPRO_LT = '#EDE7F6'
BLUE = '#1E88E5'
BLUE_LT = '#E3F2FD'
GREEN = '#2E7D32'
GREEN_LT = '#E8F5E9'
ORANGE = '#E67E00'
ORANGE_LT = '#FFF3E0'
RED = '#D3000F'
RED_LT = '#FFEBEE'
CYAN = '#0097A7'
CYAN_LT = '#E0F7FA'
GOLD = '#C49A00'
GOLD_LT = '#FFFDE7'
DARK = '#1A1A2E'
GRAY = '#4A4A5A'
MUTED = '#787888'
BORDER = '#D8D8E0'


def rounded_box(x, y, w, h, fill, edge, lw=2, alpha=1.0):
    box = FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.15",
                         facecolor=fill, edgecolor=edge, linewidth=lw, alpha=alpha)
    ax.add_patch(box)
    return box


def arrow(x1, y1, x2, y2, color=MUTED, style='->', lw=2):
    ax.annotate('', xy=(x2, y2), xytext=(x1, y1),
                arrowprops=dict(arrowstyle=style, color=color, lw=lw,
                                connectionstyle='arc3,rad=0'))


def arrow_curved(x1, y1, x2, y2, color=MUTED, rad=0.3, lw=2):
    ax.annotate('', xy=(x2, y2), xytext=(x1, y1),
                arrowprops=dict(arrowstyle='->', color=color, lw=lw,
                                connectionstyle=f'arc3,rad={rad}'))


# ══════════════════════════════════════════════════════════
#  TITLE
# ══════════════════════════════════════════════════════════
ax.text(10, 13.5, 'Wipro Agentic Commerce Accelerator — Architecture',
        fontsize=20, fontweight='bold', ha='center', color=DARK, fontfamily='sans-serif')
ax.text(10, 13.1, 'How AI Shopping Platforms Connect to Client Commerce via Wipro\'s Layer',
        fontsize=11, ha='center', color=MUTED, fontfamily='sans-serif')

# ══════════════════════════════════════════════════════════
#  LAYER 1: USER (top)
# ══════════════════════════════════════════════════════════
rounded_box(7.5, 12.0, 5.0, 0.8, '#F5F5F8', BORDER)
ax.text(10, 12.45, 'END USER', fontsize=13, fontweight='bold', ha='center', color=DARK)
ax.text(10, 12.15, '"Hey Gemini, buy me a Dyson V15"', fontsize=9, ha='center',
        color=MUTED, style='italic')

# Arrow: User → AI Platforms
arrow(10, 12.0, 10, 11.3, color=BLUE, lw=2.5)

# ══════════════════════════════════════════════════════════
#  LAYER 2: AI PLATFORMS (Google, OpenAI, Microsoft)
# ══════════════════════════════════════════════════════════
rounded_box(1.5, 10.0, 17.0, 1.2, BLUE_LT, BLUE)
ax.text(10, 10.95, 'AI SHOPPING PLATFORMS', fontsize=10, fontweight='bold',
        ha='center', color=BLUE, fontfamily='sans-serif')
ax.text(10, 10.65, '(Not our scope — these are Google / OpenAI / Microsoft products)',
        fontsize=8, ha='center', color=MUTED)

# Sub-boxes for each platform
for i, (name, protocol, clr) in enumerate([
    ('Google Gemini', 'UCP Protocol', BLUE),
    ('OpenAI ChatGPT', 'ACP Protocol', '#6B3FD4'),
    ('Microsoft Copilot', 'Copilot Checkout', '#0078D4'),
]):
    x = 2.5 + i * 5.5
    rounded_box(x, 10.1, 4.0, 0.45, 'white', clr, lw=1.5)
    ax.text(x + 2.0, 10.35, name, fontsize=9, fontweight='bold', ha='center', color=clr)

# Side box: What AI Platforms Hold
rounded_box(15.5, 8.2, 4.0, 1.6, BLUE_LT, BLUE, lw=1.5)
ax.text(17.5, 9.55, 'Google / AI Side', fontsize=9, fontweight='bold', ha='center', color=BLUE)
for j, item in enumerate(['User identity (Google account)', 'Google Pay / payment tokens',
                           'Shipping address', 'Shopping Graph (products)',
                           'OAuth tokens (if linked)']):
    ax.text(15.7, 9.2 - j * 0.25, f'• {item}', fontsize=7, color=GRAY)

# Arrow: AI Platforms → Wipro Layer
arrow(10, 10.0, 10, 9.0, color=WIPRO, lw=3)
ax.text(10.2, 9.5, 'HTTPS REST calls', fontsize=8, color=WIPRO, fontweight='bold',
        ha='left', rotation=0)

# ══════════════════════════════════════════════════════════
#  LAYER 3: WIPRO UCP INTEGRATOR (main layer)
# ══════════════════════════════════════════════════════════
rounded_box(1.5, 6.2, 13.5, 2.7, WIPRO_LT, WIPRO, lw=2.5)
ax.text(8.25, 8.65, 'WIPRO AGENTIC COMMERCE ACCELERATOR', fontsize=12,
        fontweight='bold', ha='center', color=WIPRO, fontfamily='sans-serif')
ax.text(8.25, 8.35, "(Deployed on client's cloud — Lambda / Container / Cloud Run)",
        fontsize=8, ha='center', color=MUTED)

# Sub-components of Wipro layer
components = [
    (1.8, 7.4, 3.2, 0.7, 'Discovery + Checkout', [
        '/.well-known/ucp',
        'POST/GET/PUT checkout-sessions',
        'POST /complete  |  POST /cancel',
    ], WIPRO),
    (5.2, 7.4, 3.0, 0.7, 'Field Translation', [
        'UCP ↔ SAP/SFCC mapping',
        'Cents → Dollars, GID → Cart ID',
        'Canonical Data Model',
    ], WIPRO),
    (8.4, 7.4, 3.2, 0.7, 'Identity & Auth', [
        'OAuth2 /authorize /token',
        'Bearer token pass-through',
        'Token refresh handling',
    ], WIPRO),
    (11.8, 7.4, 3.0, 0.7, 'Order Webhooks', [
        'Full order entity push',
        'JWT signed (UCP profile keys)',
        'Retry on failure',
    ], WIPRO),
]

for x, y, w, h, title, items, clr in components:
    rounded_box(x, y, w, h, 'white', clr, lw=1.5)
    ax.text(x + w/2, y + h - 0.12, title, fontsize=8, fontweight='bold',
            ha='center', color=clr)
    for j, item in enumerate(items):
        ax.text(x + 0.1, y + h - 0.32 - j * 0.17, item, fontsize=6.5, color=GRAY)

# Session mapping note
rounded_box(3.5, 6.4, 5.5, 0.45, 'white', BORDER, lw=1)
ax.text(6.25, 6.68, 'Session → Cart Mapping (Redis/DynamoDB)', fontsize=7,
        fontweight='bold', ha='center', color=WIPRO)
ax.text(6.25, 6.48, 'Stateless otherwise — no customer data stored',
        fontsize=6.5, ha='center', color=MUTED)

# Wipro builds label
rounded_box(9.3, 6.4, 5.5, 0.45, WIPRO_LT, WIPRO, lw=1)
ax.text(12.05, 6.68, 'Wipro Builds:', fontsize=7, fontweight='bold',
        ha='center', color=WIPRO)
ax.text(12.05, 6.48, 'A. UCP Integrator (endpoints + translation)   B. Platform Extension (if needed)',
        fontsize=6.5, ha='center', color=DARK)

# Arrow: Wipro → Extension
arrow(8.25, 6.2, 8.25, 5.5, color=ORANGE, lw=2.5)
ax.text(8.5, 5.85, 'Calls platform APIs', fontsize=8, color=ORANGE, fontweight='bold')

# ══════════════════════════════════════════════════════════
#  LAYER 4: WIPRO PLATFORM EXTENSION (optional)
# ══════════════════════════════════════════════════════════
rounded_box(1.5, 4.0, 13.5, 1.3, ORANGE_LT, ORANGE, lw=2)
ax.text(8.25, 5.05, 'WIPRO PLATFORM EXTENSION  (only if platform lacks required APIs)',
        fontsize=10, fontweight='bold', ha='center', color=ORANGE)

# Extension sub-boxes
for i, (platform, ext) in enumerate([
    ('SAP Commerce', 'Custom OCC Extension\n/ CPI iFlow'),
    ('Salesforce SFCC', 'Custom Cartridge\n/ SCAPI Hook'),
    ('Shopify', 'Custom Shopify\nApp'),
    ('Magento', 'Custom Magento\nModule'),
]):
    x = 1.8 + i * 3.35
    rounded_box(x, 4.15, 3.0, 0.7, 'white', ORANGE, lw=1.5)
    ax.text(x + 1.5, 4.65, platform, fontsize=8, fontweight='bold', ha='center', color=ORANGE)
    ax.text(x + 1.5, 4.35, ext, fontsize=6.5, ha='center', color=GRAY)

# Arrow: Extension → Client Platform
arrow(8.25, 4.0, 8.25, 3.3, color=GREEN, lw=2.5)
ax.text(8.5, 3.65, 'Native platform internals', fontsize=8, color=GREEN, fontweight='bold')

# ══════════════════════════════════════════════════════════
#  LAYER 5: CLIENT COMMERCE PLATFORM (untouched)
# ══════════════════════════════════════════════════════════
rounded_box(1.5, 1.5, 13.5, 1.7, GREEN_LT, GREEN, lw=2.5)
ax.text(8.25, 2.95, "CLIENT'S COMMERCE PLATFORM  (untouched — all business logic here)",
        fontsize=11, fontweight='bold', ha='center', color=GREEN)

# Sub-components
for i, (name, items) in enumerate([
    ('Product Catalog', 'Products, categories,\nimages, descriptions'),
    ('Pricing & Promotions', 'Prices, discounts,\ntax rules, coupons'),
    ('Inventory', 'Stock levels, warehouse\nallocation, backorders'),
    ('Order Management', 'Order processing,\nfulfillment, shipping'),
]):
    x = 1.8 + i * 3.35
    rounded_box(x, 1.65, 3.0, 0.65, 'white', GREEN, lw=1.5)
    ax.text(x + 1.5, 2.12, name, fontsize=8, fontweight='bold', ha='center', color=GREEN)
    ax.text(x + 1.5, 1.85, items, fontsize=6.5, ha='center', color=GRAY)

# ══════════════════════════════════════════════════════════
#  SIDE: Payment Flow
# ══════════════════════════════════════════════════════════
rounded_box(15.5, 4.0, 4.0, 3.9, GOLD_LT, GOLD, lw=2)
ax.text(17.5, 7.65, 'PAYMENT FLOW', fontsize=10, fontweight='bold',
        ha='center', color=GOLD)
ax.text(17.5, 7.35, '(Separate — never touches\nWipro layer)', fontsize=7,
        ha='center', color=MUTED)

for j, (name, desc) in enumerate([
    ('Google Pay', 'Tokenized card via Google'),
    ('Stripe', 'PSP processes payment'),
    ('Adyen', 'Alternative PSP'),
]):
    y = 6.6 - j * 0.65
    rounded_box(15.8, y, 3.4, 0.5, 'white', GOLD, lw=1)
    ax.text(17.5, y + 0.3, name, fontsize=8, fontweight='bold', ha='center', color=GOLD)
    ax.text(17.5, y + 0.1, desc, fontsize=6.5, ha='center', color=GRAY)

# Order webhook flow (side annotation)
rounded_box(15.5, 1.5, 4.0, 2.3, CYAN_LT, CYAN, lw=2)
ax.text(17.5, 3.55, 'ORDER UPDATES', fontsize=10, fontweight='bold',
        ha='center', color=CYAN)
ax.text(17.5, 3.25, '(Webhook: Merchant → Google)', fontsize=7,
        ha='center', color=MUTED)

for j, line in enumerate([
    'SAP fires status event',
    'Wipro builds full order object',
    'Signs with JWT (UCP profile keys)',
    'POST to Google webhook_url',
    'Statuses: processing → shipped',
    '→ in_transit → delivered',
    'User gets email + Gemini update',
]):
    ax.text(15.7, 2.95 - j * 0.2, f'{'→' if j > 0 and j < 5 else '•'} {line}',
            fontsize=6.5, color=GRAY if j > 0 else CYAN,
            fontweight='bold' if j == 0 else 'normal')

# ══════════════════════════════════════════════════════════
#  ARROWS: Webhook flow (SAP → Wipro → Google)
# ══════════════════════════════════════════════════════════
# SAP → Order Updates box
arrow_curved(15.0, 2.5, 15.5, 2.5, color=CYAN, rad=0, lw=1.5)
# Order Updates → AI Platforms
arrow_curved(17.5, 3.8, 17.5, 8.2, color=CYAN, rad=-0.15, lw=1.5)
ax.text(18.2, 6.0, 'Signed\nwebhooks', fontsize=7, color=CYAN, ha='center',
        fontweight='bold', rotation=90)

# Payment → AI Platforms
arrow_curved(17.5, 7.9, 17.5, 10.0, color=GOLD, rad=0.15, lw=1.5)
ax.text(16.8, 9.0, 'Payment\ntokens', fontsize=7, color=GOLD, ha='center',
        fontweight='bold', rotation=90)

# ══════════════════════════════════════════════════════════
#  LEGEND (bottom)
# ══════════════════════════════════════════════════════════
ax.text(1.5, 0.9, 'SCOPE:', fontsize=8, fontweight='bold', color=DARK)

legend_items = [
    (BLUE, 'Google / AI Platform (not our scope)'),
    (WIPRO, 'Wipro Builds & Maintains'),
    (ORANGE, 'Wipro Extension (if needed)'),
    (GREEN, "Client's Platform (untouched)"),
    (GOLD, 'Payment (separate flow)'),
    (CYAN, 'Order Webhooks (merchant → Google)'),
]
for i, (clr, label) in enumerate(legend_items):
    x = 3.0 + i * 2.8
    ax.add_patch(mpatches.Rectangle((x, 0.85), 0.3, 0.2, facecolor=clr, edgecolor=clr))
    ax.text(x + 0.4, 0.9, label, fontsize=6.5, color=GRAY, va='bottom')

# Source
ax.text(10, 0.4, 'Source: UCP Spec (ucp.dev/2026-01-23) | Google Merchant UCP Docs (developers.google.com/merchant/ucp)',
        fontsize=7, ha='center', color=MUTED, style='italic')

plt.tight_layout(pad=0.5)
out_path = '/Users/rohitgupta/Desktop/Claude/sap-ucp-poc/architecture-diagram.png'
plt.savefig(out_path, dpi=200, bbox_inches='tight', facecolor='white')
print(f"Saved: {out_path}")
plt.close()
