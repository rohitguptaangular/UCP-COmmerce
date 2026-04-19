#!/usr/bin/env python3
"""
Nestle UCP POC - Google Gemini AI Shopping Agent Test
=====================================================
This script uses Google Gemini's function calling to simulate an AI shopping agent
that interacts with the Nestle UCP (Universal Commerce Protocol) endpoints.

Prerequisites:
  pip install google-genai requests

Usage:
  1. Start the Spring Boot app: mvn spring-boot:run
  2. Set your Gemini API key: export GEMINI_API_KEY=your_key_here
  3. Run: python3 test-gemini-agent.py

Get your API key at: https://aistudio.google.com/
"""

import os
import sys
import json
import time
import requests
from google import genai
from google.genai import types

# ─── Configuration ───────────────────────────────────────────────────────────

BASE_URL = os.getenv("UCP_BASE_URL", "http://localhost:8080")
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY", "")
GEMINI_MODEL = os.getenv("GEMINI_MODEL", "gemini-2.5-flash")
MAX_TURNS = 15  # safety limit to prevent infinite loops
MAX_RETRIES = 3
RETRY_WAIT = 60  # seconds to wait on rate limit

# Available free models (try next one if current is rate-limited)
FALLBACK_MODELS = [
    "gemini-2.0-flash",
    "gemini-2.0-flash-lite",
    "gemini-1.5-flash",
]


# ─── UCP Tool Definitions for Gemini ─────────────────────────────────────────

ucp_tools = [
    types.Tool(function_declarations=[
        types.FunctionDeclaration(
            name="discover_merchant",
            description="Discover the merchant's UCP profile including capabilities, payment handlers, and identity linking configuration. This is always the first step.",
        ),
        types.FunctionDeclaration(
            name="list_products",
            description="List all available products from the merchant's catalog with prices, stock, and descriptions.",
        ),
        types.FunctionDeclaration(
            name="create_checkout_session",
            description="Create a new empty checkout session to start the shopping cart flow.",
        ),
        types.FunctionDeclaration(
            name="add_item_to_cart",
            description="Add a product to an existing checkout session by product code and quantity.",
            parameters=types.Schema(
                type="OBJECT",
                properties={
                    "session_id": types.Schema(type="STRING", description="The checkout session ID"),
                    "product_code": types.Schema(type="STRING", description="The product code (e.g. NEST-011 for KitKat Variety Pack)"),
                    "quantity": types.Schema(type="INTEGER", description="Number of items to add"),
                },
                required=["session_id", "product_code", "quantity"],
            ),
        ),
        types.FunctionDeclaration(
            name="set_shipping_address",
            description="Set the shipping address on a checkout session.",
            parameters=types.Schema(
                type="OBJECT",
                properties={
                    "session_id": types.Schema(type="STRING", description="The checkout session ID"),
                    "name": types.Schema(type="STRING", description="Full name"),
                    "street": types.Schema(type="STRING", description="Street address"),
                    "city": types.Schema(type="STRING", description="City"),
                    "state": types.Schema(type="STRING", description="State/Region"),
                    "postal_code": types.Schema(type="STRING", description="Postal/ZIP code"),
                    "country": types.Schema(type="STRING", description="Country code (e.g. US)"),
                },
                required=["session_id", "name", "street", "city", "state", "postal_code", "country"],
            ),
        ),
        types.FunctionDeclaration(
            name="set_delivery_method",
            description="Set the delivery method on a checkout session. Options: standard, express, overnight.",
            parameters=types.Schema(
                type="OBJECT",
                properties={
                    "session_id": types.Schema(type="STRING", description="The checkout session ID"),
                    "delivery_method": types.Schema(type="STRING", description="Delivery method: standard, express, or overnight"),
                },
                required=["session_id", "delivery_method"],
            ),
        ),
        types.FunctionDeclaration(
            name="get_checkout_session",
            description="Get the current state of a checkout session including items, pricing, and status.",
            parameters=types.Schema(
                type="OBJECT",
                properties={
                    "session_id": types.Schema(type="STRING", description="The checkout session ID"),
                },
                required=["session_id"],
            ),
        ),
        types.FunctionDeclaration(
            name="complete_checkout",
            description="Complete the checkout session and place the order. Requires items, address, and delivery method to be set.",
            parameters=types.Schema(
                type="OBJECT",
                properties={
                    "session_id": types.Schema(type="STRING", description="The checkout session ID"),
                },
                required=["session_id"],
            ),
        ),
        types.FunctionDeclaration(
            name="get_order_status",
            description="Get the status and details of a placed order by order ID.",
            parameters=types.Schema(
                type="OBJECT",
                properties={
                    "order_id": types.Schema(type="STRING", description="The order ID returned after checkout completion"),
                },
                required=["order_id"],
            ),
        ),
    ])
]


# ─── UCP API Client ──────────────────────────────────────────────────────────

def call_ucp(fn_name, args=None):
    """Execute a UCP API call based on the function name and arguments."""
    if args is None:
        args = {}

    try:
        if fn_name == "discover_merchant":
            resp = requests.get(f"{BASE_URL}/.well-known/ucp")

        elif fn_name == "list_products":
            resp = requests.get(f"{BASE_URL}/ucp/api/checkout/products")

        elif fn_name == "create_checkout_session":
            resp = requests.post(f"{BASE_URL}/ucp/api/checkout/sessions",
                                 json={}, headers={"Content-Type": "application/json"})

        elif fn_name == "add_item_to_cart":
            resp = requests.patch(
                f"{BASE_URL}/ucp/api/checkout/sessions/{args['session_id']}",
                json={
                    "action": "add_item",
                    "productCode": args["product_code"],
                    "quantity": int(args.get("quantity", 1))
                },
                headers={"Content-Type": "application/json"}
            )

        elif fn_name == "set_shipping_address":
            resp = requests.patch(
                f"{BASE_URL}/ucp/api/checkout/sessions/{args['session_id']}",
                json={
                    "action": "set_address",
                    "address": {
                        "name": args["name"],
                        "street": args["street"],
                        "city": args["city"],
                        "state": args["state"],
                        "postalCode": args["postal_code"],
                        "country": args["country"]
                    }
                },
                headers={"Content-Type": "application/json"}
            )

        elif fn_name == "set_delivery_method":
            resp = requests.patch(
                f"{BASE_URL}/ucp/api/checkout/sessions/{args['session_id']}",
                json={
                    "action": "set_delivery",
                    "deliveryMethod": args["delivery_method"]
                },
                headers={"Content-Type": "application/json"}
            )

        elif fn_name == "get_checkout_session":
            resp = requests.get(
                f"{BASE_URL}/ucp/api/checkout/sessions/{args['session_id']}")

        elif fn_name == "complete_checkout":
            resp = requests.post(
                f"{BASE_URL}/ucp/api/checkout/sessions/{args['session_id']}/complete",
                json={}, headers={"Content-Type": "application/json"})

        elif fn_name == "get_order_status":
            resp = requests.get(
                f"{BASE_URL}/ucp/api/orders/{args['order_id']}")

        else:
            return {"error": f"Unknown function: {fn_name}"}

        resp.raise_for_status()
        return resp.json()

    except requests.ConnectionError:
        return {"error": f"Cannot connect to {BASE_URL}. Is the Spring Boot app running?"}
    except requests.HTTPError as e:
        return {"error": f"HTTP {e.response.status_code}: {e.response.text[:300]}"}
    except Exception as e:
        return {"error": str(e)}


# ─── Gemini API Call with Retry & Model Fallback ────────────────────────────

def gemini_generate(client, model, contents, system_instruction, tools):
    """Call Gemini API with automatic retry on rate limits and model fallback."""
    models_to_try = [model] + [m for m in FALLBACK_MODELS if m != model]

    for current_model in models_to_try:
        for attempt in range(MAX_RETRIES):
            try:
                response = client.models.generate_content(
                    model=current_model,
                    contents=contents,
                    config=types.GenerateContentConfig(
                        system_instruction=system_instruction,
                        tools=tools,
                    ),
                )
                return response, current_model
            except Exception as e:
                error_msg = str(e)
                if "RESOURCE_EXHAUSTED" in error_msg or "429" in error_msg:
                    if attempt < MAX_RETRIES - 1:
                        wait = RETRY_WAIT * (attempt + 1)
                        print(f"    [Rate limited on {current_model}, retrying in {wait}s... (attempt {attempt+2}/{MAX_RETRIES})]")
                        time.sleep(wait)
                    else:
                        print(f"    [Rate limit exhausted on {current_model}, trying next model...]")
                        break  # try next model
                else:
                    raise e

    print("\n  All models rate-limited. Options:")
    print("    1. Wait a few minutes and try again")
    print("    2. Use a different API key")
    print("    3. Enable billing at https://aistudio.google.com/")
    sys.exit(1)


# ─── Gemini Agent Loop ───────────────────────────────────────────────────────

def run_agent(user_prompt):
    """Run the Gemini AI shopping agent with the given prompt."""

    system_instruction = (
        "You are an AI shopping assistant for the Nestle Online Store using the Universal Commerce Protocol (UCP). "
        "You help users browse and buy Nestle products like KitKat, Maggi, Nescafe, Nesquik, Gerber, Purina, and more. "
        "Always start by discovering the merchant's capabilities. "
        "When the user wants to buy something, follow this flow: "
        "1) Discover merchant, 2) List products, 3) Create checkout session, "
        "4) Add items, 5) Set shipping address (use a sample address if not provided), "
        "6) Set delivery method, 7) Complete checkout, 8) Check order status. "
        "Be helpful and provide clear summaries of what you're doing at each step. "
        "This is a Nestle store - only suggest Nestle products (food, beverages, pet care, baby nutrition, etc)."
    )

    client = genai.Client(api_key=GEMINI_API_KEY)

    print(f"\n{'='*60}")
    print(f"  User: {user_prompt}")
    print(f"{'='*60}\n")

    # Build conversation history
    history = []
    current_message = user_prompt
    active_model = GEMINI_MODEL

    turns = 0
    while turns < MAX_TURNS:
        turns += 1

        contents = history + [types.Content(role="user", parts=[types.Part(text=current_message)])] if turns == 1 else history
        response, active_model = gemini_generate(client, active_model, contents, system_instruction, ucp_tools)

        if active_model != GEMINI_MODEL and turns == 1:
            print(f"  [Switched to model: {active_model}]\n")

        # Add the user message to history on first turn
        if turns == 1:
            history.append(types.Content(role="user", parts=[types.Part(text=current_message)]))

        # Add model response to history
        history.append(response.candidates[0].content)

        # Process response parts
        function_calls = []
        text_parts = []

        for part in response.candidates[0].content.parts:
            if part.function_call:
                function_calls.append(part.function_call)
            elif part.text:
                text_parts.append(part.text)

        # Print any text from the model
        for text in text_parts:
            print(f"  Gemini: {text}\n")

        # If no function calls, we're done
        if not function_calls:
            break

        # Execute all function calls and collect results
        function_response_parts = []
        for fc in function_calls:
            args = dict(fc.args) if fc.args else {}
            print(f"  >> Calling: {fc.name}({json.dumps(args, indent=2) if args else ''})")

            result = call_ucp(fc.name, args)
            result_str = json.dumps(result, indent=2)
            print(f"  << Result:  {result_str[:500]}\n")

            if isinstance(result, dict) and "error" in result:
                print(f"  !! Error: {result['error']}\n")

            function_response_parts.append(
                types.Part(function_response=types.FunctionResponse(
                    name=fc.name,
                    response={"result": result}
                ))
            )

        # Add function responses to history as user turn
        history.append(types.Content(role="user", parts=function_response_parts))

    if turns >= MAX_TURNS:
        print(f"\n  [Stopped after {MAX_TURNS} turns]")

    print(f"\n{'='*60}")
    print(f"  Completed in {turns} turn(s)")
    print(f"{'='*60}\n")

    return history


# ─── Interactive Chat Mode ───────────────────────────────────────────────────

def run_chat():
    """Run an interactive natural language chat with the Gemini shopping agent."""

    system_instruction = (
        "You are an AI shopping assistant for the Nestle Online Store using the Universal Commerce Protocol (UCP). "
        "You help users browse and buy Nestle products like KitKat, Maggi, Nescafe, Nesquik, Perrier, Gerber, Purina, and more. "
        "When the user first asks about products or shopping, start by discovering the merchant's capabilities. "
        "When the user wants to buy something, follow this flow: "
        "1) Discover merchant, 2) List products, 3) Create checkout session, "
        "4) Add items, 5) Set shipping address (ask the user or use a sample if they say so), "
        "6) Set delivery method, 7) Complete checkout, 8) Check order status. "
        "Be conversational and friendly. Ask clarifying questions when needed. "
        "Summarize prices, items, and order details in a readable way. "
        "Remember the context of the conversation across messages. "
        "This is a Nestle store - only suggest Nestle products (food, beverages, pet care, baby nutrition, etc)."
    )

    client = genai.Client(api_key=GEMINI_API_KEY)
    history = []
    active_model = GEMINI_MODEL

    print(f"\n{'='*60}")
    print("  Nestle UCP POC - Interactive Shopping Chat")
    print(f"{'='*60}")
    print(f"  Server: {BASE_URL}  |  Model: {GEMINI_MODEL}")
    print(f"  Type naturally! e.g.:")
    print(f"    'What Nestle products do you have?'")
    print(f"    'I want KitKat and Maggi noodles'")
    print(f"    'Ship to 42 MG Road, Bangalore'")
    print(f"    'Place the order'")
    print(f"  Type 'quit' or 'exit' to stop.")
    print(f"{'='*60}\n")

    while True:
        try:
            user_input = input("  You: ").strip()
        except (EOFError, KeyboardInterrupt):
            print("\n  Goodbye!")
            break

        if not user_input:
            continue
        if user_input.lower() in ("quit", "exit", "q", "bye"):
            print("  Goodbye!")
            break

        # Add user message to history
        history.append(types.Content(role="user", parts=[types.Part(text=user_input)]))

        # Agent loop for this user message
        turns = 0
        while turns < MAX_TURNS:
            turns += 1

            response, active_model = gemini_generate(client, active_model, history, system_instruction, ucp_tools)

            # Add model response to history
            history.append(response.candidates[0].content)

            # Process response parts
            function_calls = []
            text_parts = []

            for part in response.candidates[0].content.parts:
                if part.function_call:
                    function_calls.append(part.function_call)
                elif part.text:
                    text_parts.append(part.text)

            # Print any text from the model
            for text in text_parts:
                print(f"\n  Gemini: {text}\n")

            # If no function calls, wait for next user input
            if not function_calls:
                break

            # Execute function calls
            function_response_parts = []
            for fc in function_calls:
                args = dict(fc.args) if fc.args else {}
                fn_display = f"{fc.name}({', '.join(f'{k}={v}' for k,v in args.items())})" if args else fc.name
                print(f"    [calling {fn_display}...]")

                result = call_ucp(fc.name, args)

                if isinstance(result, dict) and "error" in result:
                    print(f"    [error: {result['error']}]")

                function_response_parts.append(
                    types.Part(function_response=types.FunctionResponse(
                        name=fc.name,
                        response={"result": result}
                    ))
                )

            # Add function responses to history
            history.append(types.Content(role="user", parts=function_response_parts))

        if turns >= MAX_TURNS:
            print(f"\n  [Reached max turns, waiting for your input]\n")


# ─── Main ─────────────────────────────────────────────────────────────────────

def check_prerequisites():
    """Check API key and server connectivity."""
    if not GEMINI_API_KEY:
        print("Error: GEMINI_API_KEY environment variable is not set.")
        print("")
        print("Get your free API key at: https://aistudio.google.com/")
        print("Then run:")
        print("  export GEMINI_API_KEY=your_key_here")
        print("  python3 test-gemini-agent.py")
        sys.exit(1)

    try:
        requests.get(f"{BASE_URL}/.well-known/ucp", timeout=3)
    except requests.ConnectionError:
        print(f"Error: Cannot connect to UCP server at {BASE_URL}")
        print("")
        print("Start the Spring Boot app first:")
        print("  cd sap-ucp-poc")
        print("  export JAVA_HOME=$(/usr/libexec/java_home -v 17)")
        print("  mvn spring-boot:run")
        sys.exit(1)


def main():
    check_prerequisites()

    # Choose test scenario
    scenarios = {
        "1": "Discover the merchant and show me all Nestle product categories available.",
        "2": "I want to buy KitKat Variety Pack, Maggi Masala Noodles 12pk, and Nescafe Gold Blend. Add them to cart, ship to 42 MG Road, Bangalore, Karnataka 560001 India, use standard delivery, and complete checkout.",
        "3": "Buy the most expensive product available. Use express delivery to 123 Main St, San Francisco, CA 94105.",
    }

    print("\n" + "="*60)
    print("  Nestle UCP POC - Gemini AI Shopping Agent")
    print("="*60)
    print(f"\n  Server:  {BASE_URL}")
    print(f"  Model:   {GEMINI_MODEL}")
    print("\n  Modes:")
    print("    [1] Quick test - Browse Nestle products")
    print("    [2] Quick test - Full checkout (KitKat + Maggi + Nescafe)")
    print("    [3] Quick test - Buy most expensive item")
    print("    [4] Single prompt - Type one instruction")
    print("    [5] Interactive chat - Natural conversation (Recommended)")

    choice = input("\n  Select mode (1-5): ").strip()

    if choice == "5":
        run_chat()
    elif choice == "4":
        prompt = input("  Enter your prompt: ").strip()
        if not prompt:
            print("  Empty prompt. Exiting.")
            sys.exit(0)
        run_agent(prompt)
    elif choice in scenarios:
        run_agent(scenarios[choice])
    else:
        print("  Invalid choice. Starting interactive chat.")
        run_chat()


if __name__ == "__main__":
    main()
