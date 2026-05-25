import re

with open('index.html', 'r', encoding='utf-8') as f:
    content = f.read()

new_style = """<style>
        :root {
            /* WARM CINEMATIC BACKGROUND (Sunlight through glass) */
            --bg-primary: #fcf9f6; /* Warm champagne base */
            --glass-bg: rgba(255, 255, 255, 0.65);
            --glass-border: rgba(255, 255, 255, 0.8);
            --glass-shadow: 0 16px 40px rgba(234, 88, 12, 0.06); /* Warm subtle shadow */
            --glass-blur: blur(24px);

            /* TEXT COLORS */
            --text-primary: #1c1917; /* Very dark warm slate */
            --text-secondary: #57534e; /* Warm stone */
            --text-muted: #a8a29e;
            
            /* BRAND ACCENTS */
            --accent-brand: #ea580c;
            --accent-glow: #9333ea;
            --accent-saffron: #f97316;
            --accent-cyan: #06b6d4;
            --accent-green: #10b981;
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
            background-image: 
                radial-gradient(ellipse at 10% -10%, rgba(249, 115, 22, 0.15), transparent 60%), /* Top Saffron glow */
                radial-gradient(ellipse at 90% 110%, rgba(249, 115, 22, 0.12), transparent 60%), /* Bottom Saffron glow */
                radial-gradient(ellipse at 85% 15%, rgba(147, 51, 234, 0.06), transparent 50%), /* Soft Lavender touch */
                radial-gradient(circle at 50% 50%, rgba(255, 255, 255, 0.8), transparent 80%); /* Bright calm center */
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
            background: url('data:image/svg+xml;utf8,<svg viewBox="0 0 200 200" xmlns="http://www.w3.org/2000/svg"><filter id="noiseFilter"><feTurbulence type="fractalNoise" baseFrequency="0.85" numOctaves="3" stitchTiles="stitch"/></filter><rect width="100%" height="100%" filter="url(%23noiseFilter)" opacity="0.03"/></svg>');
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
            background: rgba(255, 255, 255, 0.6);
            backdrop-filter: var(--glass-blur);
            -webkit-backdrop-filter: var(--glass-blur);
            border-radius: 100px;
            padding: 4px;
            border: 1px solid rgba(255, 255, 255, 0.5);
            box-shadow: 0 2px 10px rgba(0,0,0,0.03);
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
            border: 1px solid rgba(0,0,0,0.03);
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
            background: linear-gradient(135deg, rgba(249,115,22,0.6), rgba(147,51,234,0.4), rgba(253,186,116,0.6));
            margin-bottom: 32px;
            filter: blur(20px);
            animation: orbPulse 6s ease-in-out infinite alternate;
            opacity: 0.9;
            position: relative;
        }
        
        .hero-orb::after {
            content: '';
            position: absolute;
            top: 25px; left: 25px; right: 25px; bottom: 25px;
            background: white;
            border-radius: 50%;
            filter: blur(12px);
        }

        @keyframes orbPulse {
            0% { transform: scale(0.9) translateY(0); opacity: 0.7; }
            100% { transform: scale(1.1) translateY(-10px); opacity: 1; }
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
            font-weight: 400;
            line-height: 1.5;
            max-width: 400px;
        }

        .hero-chip {
            background: rgba(255,255,255,0.7);
            border: 1px solid rgba(255,255,255,0.8);
            padding: 6px 14px;
            border-radius: 100px;
            font-size: 12px;
            font-weight: 500;
            color: var(--text-secondary);
            margin-bottom: 28px;
            backdrop-filter: var(--glass-blur);
            box-shadow: 0 4px 12px rgba(0,0,0,0.03);
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
            padding-bottom: 140px;
            scrollbar-width: none;
        }
        
        #chat-container::-webkit-scrollbar {
            display: none;
        }

        .message {
            position: relative;
            animation: slideUpFade 0.4s cubic-bezier(0.16, 1, 0.3, 1) forwards;
            font-size: 16.5px;
            line-height: 1.6;
            opacity: 0;
            transform: translateY(16px);
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
            background: linear-gradient(135deg, rgba(234, 88, 12, 0.85), rgba(147, 51, 234, 0.85));
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 16px;
            box-shadow: 0 4px 10px rgba(234, 88, 12, 0.2);
            border: 2px solid white;
            flex-shrink: 0;
        }

        /* Bot message: Warm, official, readable gradient */
        .message.bot, .bot-message {
            background: linear-gradient(135deg, rgba(255, 241, 230, 0.95) 0%, rgba(255, 235, 245, 0.95) 50%, rgba(248, 240, 255, 0.95) 100%);
            backdrop-filter: blur(24px);
            -webkit-backdrop-filter: blur(24px);
            border: 1px solid rgba(255, 255, 255, 0.8);
            border-radius: 20px 20px 20px 6px;
            padding: 16px 20px;
            color: var(--text-primary);
            box-shadow: 0 12px 32px rgba(249, 115, 22, 0.08), inset 0 2px 12px rgba(255,255,255,1);
            max-width: 85%;
            align-self: flex-start;
        }

        /* User message: Darker saffron/purple */
        .message.user, .user-message {
            background: linear-gradient(135deg, #ea580c, #9333ea);
            backdrop-filter: blur(24px);
            -webkit-backdrop-filter: blur(24px);
            border: 1px solid rgba(255, 255, 255, 0.4);
            border-radius: 20px 20px 6px 20px;
            padding: 16px 20px;
            color: white;
            box-shadow: 0 12px 32px rgba(234, 88, 12, 0.2), inset 0 2px 8px rgba(255,255,255,0.3);
            max-width: 75%;
            align-self: flex-end;
        }
        
        .message.bot.grievance-offered, .bot-message.grievance-offered,
        .message.bot.grievance-collecting, .bot-message.grievance-collecting {
            background: linear-gradient(135deg, rgba(255, 232, 224, 0.95) 0%, rgba(255, 236, 246, 0.95) 100%);
            box-shadow: 0 12px 32px rgba(249, 115, 22, 0.12), inset 0 2px 12px rgba(255,255,255,1);
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
            margin-bottom: 12px;
        }

        #interim-display {
            font-size: 13px;
            color: var(--text-secondary);
            background: rgba(255, 255, 255, 0.9);
            backdrop-filter: blur(12px);
            padding: 6px 16px;
            border-radius: 100px;
            border: 1px solid rgba(255,255,255,1);
            box-shadow: 0 8px 24px rgba(0,0,0,0.05);
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
            background: rgba(255, 255, 255, 0.7);
            backdrop-filter: blur(40px);
            -webkit-backdrop-filter: blur(40px);
            border: 1px solid rgba(255, 255, 255, 0.8);
            border-radius: 100px;
            padding: 8px 8px 8px 24px;
            box-shadow: 0 16px 48px rgba(234, 88, 12, 0.08), inset 0 2px 4px rgba(255,255,255,0.6);
            transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
        }
        
        .input-area:focus-within {
            box-shadow: 0 0 0 2px rgba(249, 115, 22, 0.2), 0 16px 48px rgba(249, 115, 22, 0.12);
            border-color: rgba(249, 115, 22, 0.4);
            background: rgba(255, 255, 255, 0.9);
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
            width: 44px;
            height: 44px;
            border-radius: 50%;
            border: none;
            display: flex;
            align-items: center;
            justify-content: center;
            cursor: pointer;
            transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
        }

        .send-btn {
            background: linear-gradient(135deg, #f97316, #a855f7);
            color: white;
            box-shadow: 0 4px 12px rgba(249, 115, 22, 0.3);
            border: 1px solid rgba(255,255,255,0.3);
        }

        .send-btn:hover {
            transform: scale(1.05);
            box-shadow: 0 6px 20px rgba(249, 115, 22, 0.5);
        }
        
        .session-btn.start {
            background: white;
            color: var(--accent-brand);
            border: 1px solid rgba(255,255,255,0.8);
            box-shadow: 0 2px 10px rgba(0,0,0,0.05);
        }
        
        .session-btn.start:hover {
            background: #fff8f5;
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
            background: rgba(255,255,255,0.6);
            border: 1px solid rgba(255,255,255,0.8);
            color: var(--text-secondary);
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 1.2rem;
            z-index: 100;
            transition: all 0.3s;
            backdrop-filter: blur(12px);
            box-shadow: 0 2px 10px rgba(0,0,0,0.03);
        }

        .settings-toggle:hover {
            background: white;
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
            background: rgba(255, 255, 255, 0.85);
            border: 1px solid rgba(255, 255, 255, 1);
            border-radius: 16px;
            padding: 20px;
            width: 280px;
            z-index: 100;
            display: none;
            backdrop-filter: blur(24px);
            box-shadow: 0 16px 48px rgba(0,0,0,0.1);
            color: var(--text-primary);
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
            background: rgba(255,255,255,0.5);
            border: 1px solid rgba(0, 0, 0, 0.1);
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
            background: rgba(255, 255, 255, 0.8);
            backdrop-filter: blur(20px);
            border: 1px solid rgba(255, 255, 255, 1);
            padding: 6px 14px;
            border-radius: 100px;
            font-size: 13px;
            color: var(--text-primary);
            font-weight: 500;
            box-shadow: 0 4px 16px rgba(0,0,0,0.06);
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
            0% { box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.5); }
            70% { box-shadow: 0 0 0 8px rgba(16, 185, 129, 0); }
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
            0% { box-shadow: 0 0 0 0 rgba(147, 51, 234, 0.4); }
            70% { box-shadow: 0 0 0 8px rgba(147, 51, 234, 0); }
            100% { box-shadow: 0 0 0 0 rgba(147, 51, 234, 0); }
        }
</style>"""

new_content = re.sub(r'<style>.*?</style>', new_style, content, flags=re.DOTALL)

with open('index.html', 'w', encoding='utf-8') as f:
    f.write(new_content)
