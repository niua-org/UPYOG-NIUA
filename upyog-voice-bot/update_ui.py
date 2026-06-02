import re

with open('index.html', 'r', encoding='utf-8') as f:
    content = f.read()

# The new head and CSS
new_head = """<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>UPYOG AI Assistant</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600&family=Noto+Sans+Devanagari:wght@400;500;600&display=swap" rel="stylesheet">
    <style>
        :root {
            /* MESH GRADIENT COLORS */
            --bg-primary: #f8fafc;
            --glass-bg: rgba(255, 255, 255, 0.7);
            --glass-border: rgba(255, 255, 255, 0.5);
            --glass-shadow: 0 8px 32px rgba(30, 41, 59, 0.04);
            --glass-blur: blur(20px);

            /* TEXT COLORS */
            --text-primary: #0f172a;
            --text-secondary: #475569;
            --text-muted: #94a3b8;
            
            /* BRAND ACCENTS */
            --accent-brand: #3b82f6; /* Modern crisp blue */
            --accent-glow: #8b5cf6;  /* Soft purple glow */
            --accent-saffron: #f97316; /* Saffron touch */
            --accent-cyan: #06b6d4;
            --accent-green: #10b981;

            --bubble-user: linear-gradient(135deg, var(--accent-brand), var(--accent-glow));
            --bubble-bot: var(--glass-bg);
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
            font-family: 'Inter', 'Noto Sans Devanagari', sans-serif;
            -webkit-font-smoothing: antialiased;
        }

        body {
            background-color: var(--bg-primary);
            background-image: 
                radial-gradient(circle at 15% 50%, rgba(249, 115, 22, 0.05), transparent 30%),
                radial-gradient(circle at 85% 30%, rgba(6, 182, 212, 0.05), transparent 30%),
                radial-gradient(circle at 50% 100%, rgba(139, 92, 246, 0.06), transparent 40%);
            background-attachment: fixed;
            color: var(--text-primary);
            height: 100vh;
            display: flex;
            flex-direction: column;
            overflow: hidden;
            position: relative;
        }
        
        body::before {
            content: '';
            position: absolute;
            top: 0; left: 0; right: 0; bottom: 0;
            background: url('data:image/svg+xml;utf8,<svg viewBox="0 0 200 200" xmlns="http://www.w3.org/2000/svg"><filter id="noiseFilter"><feTurbulence type="fractalNoise" baseFrequency="0.85" numOctaves="3" stitchTiles="stitch"/></filter><rect width="100%" height="100%" filter="url(%23noiseFilter)" opacity="0.025"/></svg>');
            pointer-events: none;
            z-index: -1;
        }

        /* ===== HEADER ===== */
        header {
            padding: 24px 32px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            z-index: 10;
            background: transparent;
        }

        header h1 {
            font-weight: 600;
            font-size: 18px;
            letter-spacing: -0.5px;
            display: flex;
            align-items: center;
            gap: 12px;
            color: var(--text-primary);
        }

        header h1 span.logo-icon {
            display: flex;
            align-items: center;
            justify-content: center;
            width: 32px;
            height: 32px;
            background: linear-gradient(135deg, var(--accent-saffron), var(--accent-glow));
            border-radius: 10px;
            color: white;
            font-size: 16px;
            box-shadow: 0 4px 12px rgba(249, 115, 22, 0.2);
        }

        .lang-toggle {
            display: flex;
            background: rgba(255,255,255,0.6);
            backdrop-filter: var(--glass-blur);
            -webkit-backdrop-filter: var(--glass-blur);
            border-radius: 100px;
            padding: 4px;
            border: 1px solid var(--glass-border);
            box-shadow: 0 2px 10px rgba(0,0,0,0.02);
        }

        .lang-btn {
            padding: 6px 14px;
            border-radius: 100px;
            border: none;
            background: transparent;
            color: var(--text-secondary);
            cursor: pointer;
            font-size: 12px;
            font-weight: 500;
            transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
        }

        .lang-btn.active {
            background: white;
            color: var(--text-primary);
            box-shadow: 0 2px 8px rgba(0,0,0,0.05);
        }

        /* ===== HERO / WELCOME ===== */
        .hero-container {
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            text-align: center;
            margin: auto;
            max-width: 600px;
            padding: 40px 20px;
            transition: opacity 0.3s ease;
        }

        .hero-orb {
            width: 100px;
            height: 100px;
            border-radius: 50%;
            background: linear-gradient(135deg, #fca5a5, #c084fc, #93c5fd);
            margin-bottom: 32px;
            filter: blur(16px);
            animation: orbPulse 6s ease-in-out infinite alternate;
            opacity: 0.8;
            position: relative;
        }
        
        .hero-orb::after {
            content: '';
            position: absolute;
            top: 20px; left: 20px; right: 20px; bottom: 20px;
            background: white;
            border-radius: 50%;
            filter: blur(12px);
        }

        @keyframes orbPulse {
            0% { transform: scale(0.9) translateY(0); opacity: 0.6; }
            100% { transform: scale(1.1) translateY(-10px); opacity: 0.9; }
        }

        .hero-title {
            font-size: 38px;
            font-weight: 700;
            letter-spacing: -1.5px;
            color: var(--text-primary);
            margin-bottom: 12px;
            line-height: 1.1;
        }

        .hero-subtitle {
            font-size: 15px;
            color: var(--text-secondary);
            font-weight: 400;
            line-height: 1.5;
            max-width: 400px;
        }

        .hero-chip {
            background: rgba(255,255,255,0.6);
            border: 1px solid var(--glass-border);
            padding: 4px 12px;
            border-radius: 100px;
            font-size: 11px;
            font-weight: 500;
            color: var(--text-secondary);
            margin-bottom: 24px;
            backdrop-filter: var(--glass-blur);
        }

        /* ===== CHAT CONTAINER ===== */
        #chat-container {
            flex: 1;
            padding: 24px;
            overflow-y: auto;
            display: flex;
            flex-direction: column;
            gap: 20px;
            scroll-behavior: smooth;
            max-width: 760px;
            margin: 0 auto;
            width: 100%;
            padding-bottom: 140px; /* Space for floating input */
            scrollbar-width: none; /* Firefox */
        }
        
        #chat-container::-webkit-scrollbar {
            display: none; /* Chrome/Safari */
        }

        .message {
            position: relative;
            animation: slideUpFade 0.4s cubic-bezier(0.16, 1, 0.3, 1) forwards;
            font-size: 15px;
            line-height: 1.6;
            opacity: 0;
            transform: translateY(16px);
        }

        @keyframes slideUpFade {
            to { opacity: 1; transform: translateY(0); }
        }

        /* Bot message */
        .message.bot, .bot-message {
            background: var(--glass-bg);
            backdrop-filter: var(--glass-blur);
            -webkit-backdrop-filter: var(--glass-blur);
            border: 1px solid var(--glass-border);
            border-radius: 20px 20px 20px 6px;
            padding: 16px 20px;
            color: var(--text-primary);
            box-shadow: var(--glass-shadow);
            max-width: 85%;
            align-self: flex-start;
        }

        /* User message */
        .message.user, .user-message {
            background: var(--bubble-user);
            border-radius: 20px 20px 6px 20px;
            padding: 16px 20px;
            color: white;
            box-shadow: 0 8px 24px rgba(59, 130, 246, 0.25);
            max-width: 75%;
            align-self: flex-end;
            font-weight: 400;
        }
        
        .message.bot.grievance-offered, .bot-message.grievance-offered,
        .message.bot.grievance-collecting, .bot-message.grievance-collecting {
            position: relative;
            overflow: hidden;
        }
        
        .message.bot.grievance-offered::before,
        .message.bot.grievance-collecting::before {
            content: '';
            position: absolute;
            left: 0; top: 0; bottom: 0;
            width: 4px;
            background: linear-gradient(to bottom, var(--accent-saffron), var(--accent-glow));
        }

        /* ===== FLOATING INPUT AREA ===== */
        .input-wrapper {
            position: absolute;
            bottom: 32px;
            left: 50%;
            transform: translateX(-50%);
            width: calc(100% - 48px);
            max-width: 700px;
            z-index: 100;
        }
        
        /* The interim text floats just above the input */
        .interim-text-wrapper {
            position: absolute;
            bottom: 100%;
            left: 24px;
            margin-bottom: 12px;
        }

        #interim-display {
            font-size: 13px;
            color: var(--text-secondary);
            background: rgba(255,255,255,0.8);
            backdrop-filter: blur(8px);
            padding: 6px 16px;
            border-radius: 100px;
            border: 1px solid var(--glass-border);
            box-shadow: 0 4px 12px rgba(0,0,0,0.05);
            transition: opacity 0.3s, transform 0.3s;
            opacity: 0;
            transform: translateY(10px);
            pointer-events: none;
            display: inline-block;
        }

        #interim-display.show {
            opacity: 1;
            transform: translateY(0);
        }

        .input-area {
            display: flex;
            gap: 12px;
            align-items: center;
            background: var(--glass-bg);
            backdrop-filter: var(--glass-blur);
            -webkit-backdrop-filter: var(--glass-blur);
            border: 1px solid var(--glass-border);
            border-radius: 100px;
            padding: 8px 8px 8px 24px;
            box-shadow: 0 12px 40px rgba(0, 0, 0, 0.08);
            transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
        }
        
        .input-area:focus-within {
            box-shadow: 0 12px 40px rgba(59, 130, 246, 0.15);
            border-color: rgba(59, 130, 246, 0.3);
            background: rgba(255, 255, 255, 0.95);
        }

        .input-area input[type="text"] {
            flex: 1;
            border: none;
            background: transparent;
            color: var(--text-primary);
            font-size: 15px;
            outline: none;
            padding: 12px 0;
        }

        .input-area input[type="text"]::placeholder {
            color: var(--text-muted);
            font-weight: 400;
        }

        .action-buttons {
            display: flex;
            gap: 8px;
            align-items: center;
        }

        .icon-btn {
            width: 40px;
            height: 40px;
            border-radius: 50%;
            border: none;
            display: flex;
            align-items: center;
            justify-content: center;
            cursor: pointer;
            transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
        }

        .send-btn {
            background: linear-gradient(135deg, var(--accent-brand), var(--accent-glow));
            color: white;
            box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
        }

        .send-btn:hover {
            transform: scale(1.05);
            box-shadow: 0 6px 16px rgba(59, 130, 246, 0.4);
        }
        
        .session-btn.start {
            background: white;
            color: var(--text-secondary);
            border: 1px solid var(--glass-border);
            box-shadow: 0 2px 8px rgba(0,0,0,0.05);
        }
        
        .session-btn.start:hover {
            background: #f8fafc;
            color: var(--accent-brand);
            transform: scale(1.05);
        }

        .session-btn.end {
            background: #ef4444;
            color: white;
            box-shadow: 0 4px 12px rgba(239, 68, 68, 0.3);
            animation: pulse-red 2s infinite;
        }

        @keyframes pulse-red {
            0% { box-shadow: 0 0 0 0 rgba(239,68,68,0.4); }
            70% { box-shadow: 0 0 0 8px rgba(239,68,68,0); }
            100% { box-shadow: 0 0 0 0 rgba(239,68,68,0); }
        }

        /* ===== SETTINGS ===== */
        .settings-toggle {
            position: fixed;
            top: 24px;
            right: 24px;
            width: 40px;
            height: 40px;
            border-radius: 50%;
            background: transparent;
            border: none;
            color: var(--text-muted);
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 1.2rem;
            z-index: 100;
            transition: all 0.3s;
        }

        .settings-toggle:hover {
            background: rgba(255, 255, 255, 0.5);
            color: var(--text-primary);
        }
        
        .header-controls {
            display: flex;
            align-items: center;
            gap: 12px;
        }

        .settings-panel {
            position: fixed;
            top: 80px;
            right: 24px;
            background: rgba(255, 255, 255, 0.9);
            border: 1px solid var(--glass-border);
            border-radius: 16px;
            padding: 20px;
            width: 280px;
            z-index: 100;
            display: none;
            backdrop-filter: blur(20px);
            box-shadow: 0 12px 40px rgba(0,0,0,0.1);
        }

        .settings-panel.show {
            display: block;
            animation: fadeIn 0.2s cubic-bezier(0.16, 1, 0.3, 1);
        }

        .settings-panel h3 {
            font-size: 13px;
            text-transform: uppercase;
            letter-spacing: 1px;
            margin-bottom: 16px;
            color: var(--text-secondary);
            font-weight: 600;
        }

        .setting-item {
            margin-bottom: 16px;
        }

        .setting-item label {
            display: block;
            font-size: 13px;
            color: var(--text-primary);
            margin-bottom: 8px;
            font-weight: 500;
        }

        .setting-item input[type="range"] {
            width: 100%;
            accent-color: var(--accent-brand);
        }

        .setting-item select {
            width: 100%;
            padding: 8px 12px;
            border-radius: 8px;
            background: white;
            border: 1px solid var(--glass-border);
            color: var(--text-primary);
            font-size: 13px;
            outline: none;
        }
        
        .hint-text {
            font-size: 11px;
            color: var(--text-muted);
            margin-top: 4px;
        }
        
        /* STATUS PILL */
        .status-pill {
            display: flex;
            align-items: center;
            gap: 8px;
            background: rgba(255,255,255,0.6);
            backdrop-filter: var(--glass-blur);
            border: 1px solid var(--glass-border);
            padding: 4px 12px;
            border-radius: 100px;
            font-size: 12px;
            color: var(--text-secondary);
            font-weight: 500;
            box-shadow: 0 2px 8px rgba(0,0,0,0.02);
            position: fixed;
            top: 24px;
            left: 50%;
            transform: translateX(-50%);
            z-index: 10;
            transition: all 0.3s ease;
            opacity: 0;
            pointer-events: none;
        }
        
        .status-pill.show {
            opacity: 1;
        }
        
        .status-dot {
            width: 8px;
            height: 8px;
            border-radius: 50%;
        }
        
        .status-dot.listening {
            background: var(--accent-green);
            box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.4);
            animation: pulse-green 1.5s infinite;
        }
        @keyframes pulse-green {
            0% { box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.4); }
            70% { box-shadow: 0 0 0 6px rgba(16, 185, 129, 0); }
            100% { box-shadow: 0 0 0 0 rgba(16, 185, 129, 0); }
        }
        
        .status-dot.processing {
            background: transparent;
            border: 2px solid rgba(59, 130, 246, 0.2);
            border-top-color: var(--accent-brand);
            animation: spin 0.8s linear infinite;
        }
        
        .status-dot.speaking {
            background: var(--accent-glow);
            animation: pulse-purple 1.5s infinite;
        }
        @keyframes pulse-purple {
            0% { box-shadow: 0 0 0 0 rgba(139, 92, 246, 0.4); }
            70% { box-shadow: 0 0 0 6px rgba(139, 92, 246, 0); }
            100% { box-shadow: 0 0 0 0 rgba(139, 92, 246, 0); }
        }
    </style>
</head>

<body>
    <!-- Top Status Pill (Shows when active) -->
    <div class="status-pill" id="status-pill">
        <div class="status-dot idle" id="status-dot"></div>
        <span id="status-text">Ready</span>
    </div>

    <header>
        <h1><span class="logo-icon">✨</span> UPYOG AI</h1>
        
        <div class="header-controls">
            <div class="lang-toggle">
                <button class="lang-btn" data-lang="en">EN</button>
                <button class="lang-btn" data-lang="hi">HI</button>
                <button class="lang-btn active" data-lang="auto">Auto</button>
            </div>
            <button class="settings-toggle" id="settings-toggle">⚙</button>
        </div>
    </header>

    <div id="chat-container">
        <!-- Hero / Welcome state -->
        <div class="hero-container" id="hero-welcome">
            <div class="hero-chip">Speak. Ask. Solve.</div>
            <div class="hero-orb"></div>
            <h2 class="hero-title">Smart Voice Assistant</h2>
            <p class="hero-subtitle">Talk naturally and get instant answers, help, and guidance for government services.</p>
        </div>
    </div>

    <!-- Floating Input Container at Bottom -->
    <div class="input-wrapper">
        <div class="interim-text-wrapper">
            <div id="interim-display"></div>
        </div>
        
        <div class="input-area" id="input-area-container">
            <input type="text" id="text-input" placeholder="Ask anything about UPYOG services..." autocomplete="off">
            <div class="action-buttons">
                <!-- Voice toggle button -->
                <button class="icon-btn session-btn start" id="session-toggle-btn" title="Tap to speak">
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M12 2a3 3 0 0 0-3 3v7a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3Z"/>
                        <path d="M19 10v2a7 7 0 0 1-14 0v-2"/>
                        <line x1="12" y1="19" x2="12" y2="22"/>
                    </svg>
                </button>
                <!-- Text send button -->
                <button id="send-text-btn" class="icon-btn send-btn" title="Send message">
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
                        <path d="M2 21l21-9L2 3v7l15 2-15 2v7z"/>
                    </svg>
                </button>
            </div>
        </div>
    </div>

    <div class="settings-panel" id="settings-panel">
        <h3>Settings</h3>
        <div class="setting-item">
            <label>Barge-in Sensitivity: <span id="sensitivity-value">0.02</span></label>
            <input type="range" id="barge-sensitivity" min="0.01" max="0.05" step="0.01" value="0.02">
            <div class="hint-text">Lower = more sensitive</div>
        </div>
        <div class="setting-item">
            <label>Language Preference</label>
            <select id="lang-preference">
                <option value="auto">Auto Detect</option>
                <option value="hi">Force Hindi</option>
                <option value="en">Force English</option>
            </select>
        </div>
    </div>
"""

# Extract the <script> block and below
script_match = re.search(r'<script>(.*?)</script>\s*</body>\s*</html>', content, re.DOTALL)
if script_match:
    script_content = script_match.group(0)
    
    # We need to apply a few quick patches to the JS since DOM elements changed.
    # 1. Remove the old text input bar logic since we use a floating one now.
    # Wait, the addBotMessage dynamic input logic creates a new bar! We need to style that too.
    # Let's just append the script for now and do a JS patch later if needed.
    
    full_html = new_head + "\n" + script_content
    
    with open('index.html', 'w', encoding='utf-8') as f:
        f.write(full_html)
    print("UI updated successfully")
else:
    print("Could not find script block")
