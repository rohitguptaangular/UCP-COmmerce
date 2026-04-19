// ==============================
// SAP UCP POC — AI Chat Frontend
// ==============================

let sessionId = crypto.randomUUID();
let isLoading = false;
let totalApiCalls = 0;
let inspectorVisible = true;

const messagesEl = document.getElementById('messages');
const inputEl = document.getElementById('messageInput');
const sendBtnEl = document.getElementById('sendBtn');
const inspectorPanel = document.getElementById('inspectorPanel');
const inspectorBody = document.getElementById('inspectorBody');
const callCountEl = document.getElementById('callCount');
const aiStatusEl = document.getElementById('aiStatus');
const welcomeScreen = document.getElementById('welcomeScreen');

// ===== Init =====
document.addEventListener('DOMContentLoaded', () => {
    checkAiStatus();
    setupTextarea();
});

// Toggle inspector
document.getElementById('toggleInspector').addEventListener('click', () => {
    inspectorVisible = !inspectorVisible;
    inspectorPanel.classList.toggle('hidden', !inspectorVisible);
    document.getElementById('toggleInspector').classList.toggle('active', inspectorVisible);
});

// Reset chat
document.getElementById('resetChat').addEventListener('click', async () => {
    try {
        await fetch('/api/chat/reset', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ sessionId })
        });
    } catch (e) { /* ignore */ }

    sessionId = crypto.randomUUID();
    messagesEl.innerHTML = '';
    inspectorBody.innerHTML = `
        <div class="inspector-empty">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <circle cx="12" cy="12" r="10"></circle>
                <line x1="12" y1="16" x2="12" y2="12"></line>
                <line x1="12" y1="8" x2="12.01" y2="8"></line>
            </svg>
            <p>API calls will appear here as the AI interacts with UCP endpoints</p>
        </div>`;
    totalApiCalls = 0;
    callCountEl.textContent = '0 calls';

    // Re-add welcome screen
    messagesEl.innerHTML = buildWelcomeHTML();
});

function setupTextarea() {
    inputEl.addEventListener('input', () => {
        inputEl.style.height = 'auto';
        inputEl.style.height = Math.min(inputEl.scrollHeight, 120) + 'px';
    });

    inputEl.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            sendMessage();
        }
    });
}

async function checkAiStatus() {
    try {
        const res = await fetch('/api/chat/status');
        const data = await res.json();
        const dot = aiStatusEl.querySelector('.status-dot');
        if (data.configured) {
            dot.className = 'status-dot ready';
            aiStatusEl.querySelector('.status-dot').nextSibling.textContent = ' Gemini AI Connected';
        } else {
            dot.className = 'status-dot error';
            aiStatusEl.querySelector('.status-dot').nextSibling.textContent = ' ' + data.message;
        }
    } catch (e) {
        const dot = aiStatusEl.querySelector('.status-dot');
        dot.className = 'status-dot error';
    }
}

// ===== Send Message =====
function sendSuggestion(btn) {
    inputEl.value = btn.textContent;
    sendMessage();
}

async function sendMessage() {
    const message = inputEl.value.trim();
    if (!message || isLoading) return;

    // Hide welcome screen
    if (welcomeScreen) {
        welcomeScreen.remove();
    }

    // Add user message
    addMessage('user', message);
    inputEl.value = '';
    inputEl.style.height = 'auto';

    // Show typing indicator
    isLoading = true;
    sendBtnEl.disabled = true;
    const typingEl = showTyping();

    try {
        const res = await fetch('/api/chat', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ message, sessionId })
        });

        const data = await res.json();

        // Remove typing indicator
        typingEl.remove();

        if (data.error) {
            addMessage('assistant', 'Sorry, something went wrong: ' + data.error);
        } else {
            // Log tool calls to inspector
            if (data.toolCalls && data.toolCalls.length > 0) {
                data.toolCalls.forEach(tc => addApiCall(tc));
            }

            // Add AI response
            addMessage('assistant', data.reply, data.toolCalls);
        }
    } catch (e) {
        typingEl.remove();
        addMessage('assistant', 'Failed to reach the server. Make sure the Spring Boot app is running.');
    }

    isLoading = false;
    sendBtnEl.disabled = false;
    inputEl.focus();
}

// ===== Message Rendering =====
function addMessage(role, text, toolCalls) {
    const div = document.createElement('div');
    div.className = `message ${role}`;

    const avatarLabel = role === 'user' ? 'You' : 'AI';
    const avatarChar = role === 'user' ? 'U' : 'N';

    let toolBadgesHTML = '';
    if (toolCalls && toolCalls.length > 0) {
        const badges = toolCalls.map(tc =>
            `<span class="tool-badge">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="20 6 9 17 4 12"></polyline>
                </svg>
                ${tc.function}
            </span>`
        ).join('');
        toolBadgesHTML = `<div class="tool-badges">${badges}</div>`;
    }

    const now = new Date();
    const time = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

    div.innerHTML = `
        <div class="message-avatar">${avatarChar}</div>
        <div class="message-content">
            <div class="message-bubble">${renderMarkdown(text)}</div>
            ${toolBadgesHTML}
            <div class="message-time">${time}</div>
        </div>
    `;

    messagesEl.appendChild(div);
    scrollToBottom();
}

function showTyping() {
    const div = document.createElement('div');
    div.className = 'typing-indicator';
    div.innerHTML = `
        <div class="message-avatar" style="background: linear-gradient(135deg, var(--accent), #ff4444); color: white;">N</div>
        <div class="typing-dots">
            <span></span><span></span><span></span>
        </div>
    `;
    messagesEl.appendChild(div);
    scrollToBottom();
    return div;
}

function scrollToBottom() {
    messagesEl.scrollTop = messagesEl.scrollHeight;
}

// ===== Markdown Renderer (lightweight) =====
function renderMarkdown(text) {
    if (!text) return '';

    let html = text
        // Escape HTML
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        // Bold
        .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
        // Italic
        .replace(/\*(.+?)\*/g, '<em>$1</em>')
        // Inline code
        .replace(/`([^`]+)`/g, '<code>$1</code>')
        // Headers
        .replace(/^### (.+)$/gm, '<strong style="font-size: 14px;">$1</strong>')
        .replace(/^## (.+)$/gm, '<strong style="font-size: 15px;">$1</strong>')
        .replace(/^# (.+)$/gm, '<strong style="font-size: 16px;">$1</strong>')
        // Unordered lists
        .replace(/^[*-] (.+)$/gm, '<li>$1</li>')
        // Numbered lists
        .replace(/^\d+\. (.+)$/gm, '<li>$1</li>')
        // Horizontal rule
        .replace(/^---$/gm, '<hr style="border: none; border-top: 1px solid var(--border); margin: 12px 0;">')
        // Line breaks -> paragraphs
        .replace(/\n\n/g, '</p><p>')
        .replace(/\n/g, '<br>');

    // Wrap consecutive <li> in <ul>
    html = html.replace(/((?:<li>.*?<\/li>\s*)+)/g, '<ul>$1</ul>');

    return `<p>${html}</p>`;
}

// ===== Inspector =====
function addApiCall(toolCall) {
    // Remove empty state
    const empty = inspectorBody.querySelector('.inspector-empty');
    if (empty) empty.remove();

    totalApiCalls++;
    callCountEl.textContent = `${totalApiCalls} call${totalApiCalls !== 1 ? 's' : ''}`;

    const methodMap = {
        'discover_merchant': { method: 'GET', endpoint: '/.well-known/ucp' },
        'list_products': { method: 'GET', endpoint: '/ucp/api/checkout/products' },
        'create_checkout_session': { method: 'POST', endpoint: '/ucp/api/checkout/sessions' },
        'add_item_to_cart': { method: 'PATCH', endpoint: '/ucp/api/checkout/sessions/{id}' },
        'set_shipping_address': { method: 'PATCH', endpoint: '/ucp/api/checkout/sessions/{id}' },
        'set_delivery_method': { method: 'PATCH', endpoint: '/ucp/api/checkout/sessions/{id}' },
        'get_checkout_session': { method: 'GET', endpoint: '/ucp/api/checkout/sessions/{id}' },
        'complete_checkout': { method: 'POST', endpoint: '/ucp/api/checkout/sessions/{id}/complete' },
        'get_order_status': { method: 'GET', endpoint: '/ucp/api/orders/{id}' }
    };

    const info = methodMap[toolCall.function] || { method: 'POST', endpoint: '/ucp/api/' + toolCall.function };
    const now = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });

    const div = document.createElement('div');
    div.className = 'api-call';

    const argsStr = toolCall.args ? JSON.stringify(toolCall.args, null, 2) : '{}';
    const resultStr = toolCall.result ? JSON.stringify(toolCall.result, null, 2) : '{}';

    // Truncate long results for display
    const displayResult = resultStr.length > 2000
        ? resultStr.substring(0, 2000) + '\n... (truncated)'
        : resultStr;

    div.innerHTML = `
        <div class="api-call-header" onclick="this.parentElement.classList.toggle('expanded')">
            <span class="api-method ${info.method.toLowerCase()}">${info.method}</span>
            <span class="api-call-name">${info.endpoint}</span>
            <span class="api-call-time">${now}</span>
            <span class="api-call-chevron">&#9654;</span>
        </div>
        <div class="api-call-body">
            <div class="api-call-section">
                <div class="api-call-section-label">Function</div>
                <pre>${toolCall.function}</pre>
            </div>
            <div class="api-call-section">
                <div class="api-call-section-label">Request Args</div>
                <pre>${escapeHtml(argsStr)}</pre>
            </div>
            <div class="api-call-section">
                <div class="api-call-section-label">Response</div>
                <pre>${escapeHtml(displayResult)}</pre>
            </div>
        </div>
    `;

    inspectorBody.appendChild(div);
    inspectorBody.scrollTop = inspectorBody.scrollHeight;
}

function escapeHtml(text) {
    return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

function buildWelcomeHTML() {
    return `
    <div class="welcome-screen" id="welcomeScreen">
        <div class="welcome-icon">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
            </svg>
        </div>
        <h2>Welcome to Nestle AI Shopping</h2>
        <p>I'm your AI shopping assistant powered by the Universal Commerce Protocol. I can help you browse products, build your cart, and complete your purchase — all through natural conversation.</p>
        <div class="architecture-badge">
            <span class="arch-flow">Your Message</span>
            <span class="arch-arrow">&rarr;</span>
            <span class="arch-flow">Gemini AI</span>
            <span class="arch-arrow">&rarr;</span>
            <span class="arch-flow">UCP Protocol</span>
            <span class="arch-arrow">&rarr;</span>
            <span class="arch-flow">SAP Commerce</span>
        </div>
        <div class="suggestions">
            <p class="suggestions-label">Try asking:</p>
            <button class="suggestion" onclick="sendSuggestion(this)">What products do you have?</button>
            <button class="suggestion" onclick="sendSuggestion(this)">Show me coffee products</button>
            <button class="suggestion" onclick="sendSuggestion(this)">I want to buy KitKat and Maggi noodles</button>
            <button class="suggestion" onclick="sendSuggestion(this)">What's the most expensive item?</button>
        </div>
    </div>`;
}
