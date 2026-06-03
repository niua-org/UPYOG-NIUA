import re

with open('index.html', 'r', encoding='utf-8') as f:
    content = f.read()

# Inject the ambient background HTML layers right after <body>
if '<div class="ambient-layer">' not in content:
    ambient_html = """
    <div class="ambient-layer"></div>
    <div class="organic-shape-1"></div>
    <div class="organic-shape-2"></div>
    <div class="organic-shape-3"></div>
    <div class="organic-shape-4"></div>
    <div class="grain-overlay"></div>
"""
    content = content.replace('<body>', '<body>' + ambient_html)

new_style = """<style>
        :root {
            /* ADVANCED SUNRISE CINEMATIC BACKGROUND */
            --bg-primary: #f1f5f9; /* Base soft cool canvas */
            --glass-bg: rgba(255, 255, 255, 0.5);
            --glass-border: rgba(255, 255, 255, 0.9);
            --glass-shadow: 0 20px 40px rgba(0, 0, 0, 0.08);
            --glass-blur: blur(48px);

            /* TEXT COLORS */
            --text-primary: #0f172a; /* Deep slate */
            --text-secondary: #475569; /* Slate */
            --text-muted: #94a3b8;
            
            /* BRAND ACCENTS */
            --accent-brand: #ea580c;
            --accent-glow: #f59e0b; 
            --accent-saffron: #f97316;
            --accent-cyan: #38bdf8; 
            --accent-green: #10b981;
            --accent-lavender: #a855f7;
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
            font-family: 'Outfit', 'Noto Sans Devanagari', sans-serif;
            -webkit-font-smoothing: antialiased;
        }

        body {
            background-color: var(--bg-primary);
            color: var(--text-primary);
            height: 100vh;
            display: flex;
            flex-direction: column;
            overflow: hidden;
            position: relative;
        }
        
        /* ===== AMBIENT VOLUMETRIC BACKGROUND ===== */
        .ambient-layer {
            position: absolute;
            top: 0; left: 0; width: 100%; height: 100%;
            z-index: -5;
            background-image: 
                radial-gradient(ellipse at 15% 15%, rgba(251, 146, 60, 0.5), transparent 60%), /* Golden amber top left */
                radial-gradient(ellipse at 85% 10%, rgba(244, 114, 182, 0.4), transparent 50%), /* Pink-orange clouds top right */
                radial-gradient(ellipse at 50% 85%, rgba(249, 115, 22, 0.45), transparent 60%), /* Saffron core bottom center */
                radial-gradient(ellipse at 85% 90%, rgba(167, 139, 250, 0.35), transparent 60%), /* Lavender mist bottom right */
                radial-gradient(ellipse at 15% 85%, rgba(14, 165, 233, 0.3), transparent 60%), /* Airy sky blue bottom left */
                radial-gradient(ellipse at 50% 50%, rgba(255, 255, 255, 0.8), transparent 70%); /* Calm central light */
            filter: blur(60px);
            opacity: 1;
        }

        .organic-shape-1 {
            position: absolute;
            top: -15%; left: -10%; width: 65vw; height: 65vh;
            background: radial-gradient(circle, rgba(253, 186, 116, 0.7) 0%, transparent 65%);
            filter: blur(90px);
            z-index: -4;
            animation: float 25s infinite alternate ease-in-out;
            opacity: 0.8;
        }

        .organic-shape-2 {
            position: absolute;
            bottom: -20%; right: -15%; width: 75vw; height: 75vh;
            background: radial-gradient(circle, rgba(249, 115, 22, 0.6) 0%, transparent 70%);
            filter: blur(100px);
            z-index: -4;
            animation: float-reverse 28s infinite alternate ease-in-out;
            opacity: 0.9;
        }

        .organic-shape-3 {
            position: absolute;
            top: 25%; right: 10%; width: 55vw; height: 55vh;
            background: radial-gradient(circle, rgba(244, 114, 182, 0.5) 0%, transparent 65%);
            filter: blur(85px);
            z-index: -4;
            animation: float-vertical 30s infinite alternate ease-in-out;
            opacity: 0.8;
        }

        .organic-shape-4 {
            position: absolute;
            bottom: 10%; left: 10%; width: 50vw; height: 50vh;
            background: radial-gradient(circle, rgba(167, 139, 250, 0.45) 0%, transparent 60%);
            filter: blur(80px);
            z-index: -4;
            animation: float-diagonal 35s infinite alternate ease-in-out;
            opacity: 0.8;
        }

        .grain-overlay {
            position: absolute;
            top: 0; left: 0; right: 0; bottom: 0;
            background: url('data:image/svg+xml;utf8,<svg viewBox="0 0 200 200" xmlns="http://www.w3.org/2000/svg"><filter id="noiseFilter"><feTurbulence type="fractalNoise" baseFrequency="0.75" numOctaves="3" stitchTiles="stitch"/></filter><rect width="100%" height="100%" filter="url(%23noiseFilter)" opacity="0.055"/></svg>');
            pointer-events: none;
            z-index: -2;
            mix-blend-mode: overlay;
        }

        @keyframes float {
            0% { transform: translate(0, 0) scale(1); }
            100% { transform: translate(8%, 12%) scale(1.1); }
        }
        @keyframes float-reverse {
            0% { transform: translate(0, 0) scale(1); }
            100% { transform: translate(-10%, -8%) scale(1.15); }
        }
        @keyframes float-vertical {
            0% { transform: translate(0, 0) scale(1); }
            100% { transform: translate(2%, 15%) scale(1.05); }
        }
        @keyframes float-diagonal {
            0% { transform: translate(0, 0) scale(1); }
            100% { transform: translate(12%, -10%) scale(1.1); }
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
            box-shadow: 0 8px 16px rgba(249, 115, 22, 0.3);
        }

        .lang-toggle {
            display: flex;
            background: rgba(255, 255, 255, 0.4);
            backdrop-filter: blur(20px) saturate(150%);
            -webkit-backdrop-filter: blur(20px) saturate(150%);
            border-radius: 100px;
            padding: 4px;
            border: 1px solid rgba(255, 255, 255, 0.8);
            box-shadow: 0 4px 15px rgba(0,0,0,0.05);
        }

        .lang-btn {
            padding: 6px 14px;
            border-radius: 100px;
            border: none;
            background: transparent;
            color: var(--text-secondary);
            cursor: pointer;
            font-size: 12px;
            font-weight: 600;
            transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
        }

        .lang-btn.active {
            background: white;
            color: var(--text-primary);
            box-shadow: 0 2px 8px rgba(0,0,0,0.08);
            border: 1px solid rgba(0,0,0,0.05);
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
            background: linear-gradient(135deg, rgba(249,115,22,0.8), rgba(251,146,60,0.7), rgba(244,114,182,0.6));
            margin-bottom: 32px;
            filter: blur(16px);
            animation: orbPulse 5s ease-in-out infinite alternate;
            opacity: 0.95;
            position: relative;
            box-shadow: 0 0 60px rgba(249, 115, 22, 0.4);
        }
        
        .hero-orb::after {
            content: '';
            position: absolute;
            top: 20px; left: 20px; right: 20px; bottom: 20px;
            background: white;
            border-radius: 50%;
            filter: blur(10px);
        }

        @keyframes orbPulse {
            0% { transform: scale(0.9) translateY(0); opacity: 0.8; }
            100% { transform: scale(1.15) translateY(-10px); opacity: 1; }
        }

        .hero-title {
            font-size: 42px;
            font-weight: 700;
            letter-spacing: -1.5px;
            color: var(--text-primary);
            margin-bottom: 12px;
            line-height: 1.1;
        }

        .hero-subtitle {
            font-size: 16px;
            color: var(--text-secondary);
            font-weight: 500;
            line-height: 1.5;
            max-width: 400px;
        }

        .hero-chip {
            background: rgba(255,255,255,0.5);
            border: 1px solid rgba(255,255,255,1);
            padding: 6px 14px;
            border-radius: 100px;
            font-size: 12px;
            font-weight: 600;
            color: var(--text-secondary);
            margin-bottom: 28px;
            backdrop-filter: blur(16px) saturate(150%);
            box-shadow: 0 8px 16px rgba(0,0,0,0.04);
        }

        /* ===== CHAT CONTAINER ===== */
        #chat-container {
            flex: 1;
            padding: 24px;
            overflow-y: auto;
            display: flex;
            flex-direction: column;
            gap: 24px; /* Increased gap for atmospheric feeling */
            scroll-behavior: smooth;
            max-width: 760px;
            margin: 0 auto;
            width: 100%;
            padding-bottom: 140px;
            scrollbar-width: none;
        }
        
        #chat-container::-webkit-scrollbar {
            display: none;
        }

        .message {
            position: relative;
            animation: slideUpFade 0.5s cubic-bezier(0.16, 1, 0.3, 1) forwards;
            font-size: 16.5px;
            line-height: 1.6;
            opacity: 0;
            transform: translateY(20px);
            font-weight: 500;
        }

        @keyframes slideUpFade {
            to { opacity: 1; transform: translateY(0); }
        }
        
        .message-row {
            display: flex;
            align-items: flex-end;
            gap: 12px;
            width: 100%;
        }
        
        .message-row.user-row {
            justify-content: flex-end;
        }
        
        .user-avatar {
            width: 32px;
            height: 32px;
            border-radius: 50%;
            background: linear-gradient(135deg, rgba(251, 146, 60, 0.9), rgba(244, 114, 182, 0.9));
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 16px;
            box-shadow: 0 8px 16px rgba(251, 146, 60, 0.3);
            border: 2px solid white;
            flex-shrink: 0;
        }

        /* Bot message: Premium Glossy Frosted Glass */
        .message.bot, .bot-message {
            background: linear-gradient(135deg, rgba(255, 255, 255, 0.85) 0%, rgba(255, 255, 255, 0.5) 100%);
            backdrop-filter: blur(32px) saturate(180%);
            -webkit-backdrop-filter: blur(32px) saturate(180%);
            border: 1px solid rgba(255, 255, 255, 1);
            border-bottom: 1px solid rgba(255, 255, 255, 0.4);
            border-right: 1px solid rgba(255, 255, 255, 0.4);
            border-radius: 24px 24px 24px 8px; /* Modern rounded corners */
            padding: 18px 24px;
            color: var(--text-primary);
            font-weight: 500;
            box-shadow: 0 24px 48px rgba(0, 0, 0, 0.06), inset 0 2px 15px rgba(255,255,255,0.9), inset 0 -2px 12px rgba(251, 146, 60, 0.15); /* Subtle sunrise reflection */
            max-width: 85%;
            align-self: flex-start;
        }

        /* User message: Vibrant, glowing sunrise */
        .message.user, .user-message {
            background: linear-gradient(135deg, rgba(251, 146, 60, 0.95) 0%, rgba(249, 115, 22, 0.95) 50%, rgba(167, 139, 250, 0.95) 100%);
            backdrop-filter: blur(32px) saturate(150%);
            -webkit-backdrop-filter: blur(32px) saturate(150%);
            border: 1px solid rgba(255, 255, 255, 0.6);
            border-bottom: 1px solid rgba(255, 255, 255, 0.2);
            border-right: 1px solid rgba(255, 255, 255, 0.2);
            border-radius: 24px 24px 8px 24px;
            padding: 18px 24px;
            color: white; 
            box-shadow: 0 24px 48px rgba(249, 115, 22, 0.25), inset 0 2px 12px rgba(255,255,255,0.4);
            max-width: 75%;
            align-self: flex-end;
        }
        
        .message.bot.grievance-offered, .bot-message.grievance-offered,
        .message.bot.grievance-collecting, .bot-message.grievance-collecting {
            background: linear-gradient(135deg, rgba(255, 240, 235, 0.9) 0%, rgba(255, 230, 240, 0.8) 100%);
            box-shadow: 0 24px 48px rgba(249, 115, 22, 0.1), inset 0 2px 15px rgba(255,255,255,1);
            position: relative;
            overflow: hidden;
            border-left: 4px solid var(--accent-saffron);
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
        
        .interim-text-wrapper {
            position: absolute;
            bottom: 100%;
            left: 24px;
            margin-bottom: 16px;
        }

        #interim-display {
            font-size: 13px;
            color: var(--text-secondary);
            background: rgba(255, 255, 255, 0.8);
            backdrop-filter: blur(16px);
            padding: 8px 18px;
            border-radius: 100px;
            border: 1px solid rgba(255,255,255,1);
            box-shadow: 0 12px 24px rgba(0,0,0,0.05);
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

        /* Floating glass dock style */
        .input-area {
            display: flex;
            gap: 12px;
            align-items: center;
            background: rgba(255, 255, 255, 0.5); /* Premium translucency */
            backdrop-filter: blur(48px) saturate(150%);
            -webkit-backdrop-filter: blur(48px) saturate(150%);
            border: 1px solid rgba(255, 255, 255, 0.8);
            border-bottom: 1px solid rgba(255, 255, 255, 0.3);
            border-radius: 100px;
            padding: 10px 10px 10px 24px;
            box-shadow: 0 32px 64px rgba(0, 0, 0, 0.08), 0 16px 32px rgba(249, 115, 22, 0.12), inset 0 2px 10px rgba(255,255,255,0.9); /* Warm glow underneath + deep dimensional shadow */
            transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
        }
        
        .input-area:focus-within {
            box-shadow: 0 0 0 3px rgba(251, 146, 60, 0.3), 0 32px 64px rgba(249, 115, 22, 0.15), inset 0 2px 10px rgba(255,255,255,1);
            border-color: rgba(255, 255, 255, 1);
            background: rgba(255, 255, 255, 0.65);
        }

        .input-area input[type="text"] {
            flex: 1;
            border: none;
            background: transparent;
            color: var(--text-primary);
            font-size: 16px;
            font-weight: 500;
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
            width: 48px;
            height: 48px;
            border-radius: 50%;
            border: none;
            display: flex;
            align-items: center;
            justify-content: center;
            cursor: pointer;
            transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
        }

        .send-btn {
            background: linear-gradient(135deg, #f97316, #d946ef);
            color: white;
            box-shadow: 0 8px 16px rgba(249, 115, 22, 0.3);
            border: 1px solid rgba(255,255,255,0.5);
        }

        .send-btn:hover {
            transform: scale(1.05);
            box-shadow: 0 12px 24px rgba(249, 115, 22, 0.4);
        }
        
        .session-btn.start {
            background: rgba(255,255,255,0.8);
            color: var(--accent-brand);
            border: 1px solid rgba(255,255,255,1);
            backdrop-filter: blur(10px);
            box-shadow: 0 4px 12px rgba(0,0,0,0.05);
        }
        
        .session-btn.start:hover {
            background: white;
            transform: scale(1.05);
            box-shadow: 0 8px 16px rgba(0,0,0,0.08);
        }

        .session-btn.end {
            background: #ef4444;
            color: white;
            box-shadow: 0 8px 16px rgba(239, 68, 68, 0.3);
            animation: pulse-red 2s infinite;
        }

        @keyframes pulse-red {
            0% { box-shadow: 0 0 0 0 rgba(239,68,68,0.4); }
            70% { box-shadow: 0 0 0 12px rgba(239,68,68,0); }
            100% { box-shadow: 0 0 0 0 rgba(239,68,68,0); }
        }

        /* ===== SETTINGS ===== */
        .settings-toggle {
            position: fixed;
            top: 24px;
            right: 24px;
            width: 44px;
            height: 44px;
            border-radius: 50%;
            background: rgba(255, 255, 255, 0.5);
            border: 1px solid rgba(255,255,255,0.9);
            color: var(--text-secondary);
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 1.2rem;
            z-index: 100;
            transition: all 0.3s;
            backdrop-filter: blur(20px) saturate(150%);
            box-shadow: 0 4px 12px rgba(0,0,0,0.05);
        }

        .settings-toggle:hover {
            background: white;
            color: var(--text-primary);
            box-shadow: 0 8px 16px rgba(0,0,0,0.08);
        }
        
        .header-controls {
            display: flex;
            align-items: center;
            gap: 16px;
        }

        .settings-panel {
            position: fixed;
            top: 84px;
            right: 24px;
            background: rgba(255, 255, 255, 0.85);
            border: 1px solid rgba(255, 255, 255, 1);
            border-radius: 20px;
            padding: 24px;
            width: 300px;
            z-index: 100;
            display: none;
            backdrop-filter: blur(32px) saturate(150%);
            box-shadow: 0 24px 64px rgba(0,0,0,0.1);
            color: var(--text-primary);
        }

        .settings-panel.show {
            display: block;
            animation: fadeIn 0.2s cubic-bezier(0.16, 1, 0.3, 1);
        }

        .settings-panel h3 {
            font-size: 13px;
            text-transform: uppercase;
            letter-spacing: 1.5px;
            margin-bottom: 20px;
            color: var(--text-secondary);
            font-weight: 700;
        }

        .setting-item {
            margin-bottom: 20px;
        }

        .setting-item label {
            display: block;
            font-size: 14px;
            color: var(--text-primary);
            margin-bottom: 10px;
            font-weight: 600;
        }

        .setting-item input[type="range"] {
            width: 100%;
            accent-color: var(--accent-brand);
        }

        .setting-item select {
            width: 100%;
            padding: 10px 14px;
            border-radius: 12px;
            background: rgba(255,255,255,0.7);
            border: 1px solid rgba(0, 0, 0, 0.08);
            color: var(--text-primary);
            font-size: 14px;
            font-weight: 500;
            outline: none;
            box-shadow: inset 0 2px 4px rgba(0,0,0,0.02);
        }
        
        .hint-text {
            font-size: 11.5px;
            color: var(--text-muted);
            margin-top: 6px;
        }
        
        /* STATUS PILL */
        .status-pill {
            display: flex;
            align-items: center;
            gap: 10px;
            background: rgba(255, 255, 255, 0.7);
            backdrop-filter: blur(24px) saturate(150%);
            border: 1px solid rgba(255, 255, 255, 1);
            padding: 8px 18px;
            border-radius: 100px;
            font-size: 14px;
            color: var(--text-primary);
            font-weight: 600;
            box-shadow: 0 8px 24px rgba(0,0,0,0.06);
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
            width: 10px;
            height: 10px;
            border-radius: 50%;
        }
        
        .status-dot.listening {
            background: var(--accent-green);
            box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.4);
            animation: pulse-green 1.5s infinite;
        }
        @keyframes pulse-green {
            0% { box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.5); }
            70% { box-shadow: 0 0 0 10px rgba(16, 185, 129, 0); }
            100% { box-shadow: 0 0 0 0 rgba(16, 185, 129, 0); }
        }
        
        .status-dot.processing {
            background: transparent;
            border: 2px solid rgba(0, 0, 0, 0.1);
            border-top-color: var(--accent-brand);
            animation: spin 0.8s linear infinite;
        }
        
        .status-dot.speaking {
            background: var(--accent-glow);
            animation: pulse-purple 1.5s infinite;
        }
        @keyframes pulse-purple {
            0% { box-shadow: 0 0 0 0 rgba(245, 158, 11, 0.4); }
            70% { box-shadow: 0 0 0 10px rgba(245, 158, 11, 0); }
            100% { box-shadow: 0 0 0 0 rgba(245, 158, 11, 0); }
        }
</style>"""

new_content = re.sub(r'<style>.*?</style>', new_style, content, flags=re.DOTALL)

with open('index.html', 'w', encoding='utf-8') as f:
    f.write(new_content)
