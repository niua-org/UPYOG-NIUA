# UPYOG Voice Assistant (v2) — System Architecture Blueprint

## Executive Overview
The **UPYOG Voice Assistant (v2)** is an enterprise conversational AI platform that enables citizens to interact with municipal public services via natural, continuous bilingual voice (Hindi, English, Hinglish). 

It connects citizen voice interactions directly to the **National Urban Digital Mission (NUDM / UPYOG)** microservices infrastructure, handling citizen service discovery, grievance redressing (PGR), dynamic slot-booking for civic advertisements, document processing, and multi-draft state recovery.

---

## 1. System Master Architecture Diagram (Multi-Tier)

This diagram illustrates the comprehensive architecture across all tiers: Client Interface, API Gateway & Orchestration, Intelligence & Workflow, Storage & Memory, and External Municipal Microservices.

```mermaid
flowchart TB
    %% STYLING DEFINITIONS
    classDef client fill:#EBF5FB,stroke:#2980B9,stroke-width:2px,color:#1B4F72;
    classDef gateway fill:#E8F8F5,stroke:#16A085,stroke-width:2px,color:#0E6251;
    classDef brain fill:#FEF9E7,stroke:#F39C12,stroke-width:2px,color:#7D6608;
    classDef workflow fill:#F4ECF7,stroke:#8E44AD,stroke-width:2px,color:#512E5F;
    classDef storage fill:#FDEDEC,stroke:#C0392B,stroke-width:2px,color:#78281F;
    classDef external fill:#EAEDED,stroke:#7F8C8D,stroke-width:2px,color:#2C3E50;

    %% TIER 1: CLIENT & VOICE INTERACTION
    subgraph T1["1. Citizen Interaction Tier (Web / Mobile)"]
        BrowserUI["Web UI / PWA Frontend\n(HTML5, CSS3, ES6)"]:::client
        WebSpeech["Web Speech API\n(Continuous Listening STT)"]:::client
        AudioPlayer["HTML5 Streaming Audio Engine\n(Barge-in / Interruption Controller)"]:::client
        OTPModal["Citizen Auth & Mobile OTP Modal"]:::client
    end

    %% TIER 2: API GATEWAY & ORCHESTRATION
    subgraph T2["2. API Gateway & Routing Tier (Flask 8090)"]
        Nginx["Nginx Reverse Proxy & SSL Termination"]:::gateway
        FlaskServer["Flask Application Core (app.py)"]:::gateway
        ChatRoutes["Chat Blueprint (/chat, /stop)"]:::gateway
        AuthRoutes["Auth Blueprint (/auth/send-otp, /verify)"]:::gateway
        StaticRoutes["Static Asset & Form Blueprints"]:::gateway
        DynEnv["Dynamic UPYOG Environment Resolver\n(NIUATT / Sandbox / Prod)"]:::gateway
    end

    %% TIER 3: INTELLIGENCE & SPEECH PIPELINE
    subgraph T3["3. AI Intelligence & Speech Processing Tier"]
        LangDetect["Script & Language Classifier\n(Hindi / English / Hinglish / Phonetic)"]:::brain
        IntentClassifier["Intent & Emotion Classifier\n(Groq LPU - Llama 3.1 / GPT-OSS)"]:::brain
        RAGModule["RAG Semantic Retrieval Engine\n(SentenceTransformers: all-mpnet-base-v2)"]:::brain
        GroqLLM["High-Speed Inference Engine\n(Groq LPUs - Sub-500ms TTFT)"]:::brain
        TTSGateway["Adaptive TTS Synthesizer"]:::brain
        EdgeTTS["Edge-TTS (Primary Engine)"]:::brain
        BhashiniTTS["Bhashini Indic TTS (Govt AI Fallback)"]:::brain
    end

    %% TIER 4: TRANSACTIONAL WORKFLOW ENGINE
    subgraph T4["4. Transactional State & Workflow Tier (LangGraph)"]
        PluginRouter["Dynamic Plugin Discovery & Loader"]:::workflow
        AdvGraph["Advertisement Booking Workflow\n(Slot Search, Rates, MDMS, Cart)"]:::workflow
        PGRGraph["Grievance Registration Workflow\n(Category, Subcategory, Locality, Ticket)"]:::workflow
        DraftManager["Multi-Draft Switcher & Conflict Resolver\n(Phone:Module Isolation)"]:::workflow
        MCPBridge["Model Context Protocol (MCP) Tools"]:::workflow
    end

    %% TIER 5: DUAL PERSISTENCE & MEMORY TIER
    subgraph T5["5. State & Storage Persistence Tier"]
        RAMCheck["RAM In-Memory Checkpointer\n(langgraph MemorySaver)"]:::storage
        RedisStore["Redis Distributed Store\n(Active Session, Sliding Window, User Tokens)"]:::storage
        subgraph QdrantGroup["Qdrant Vector Database (gRPC:6334)"]
            QMem[("long_term_chat_memory\n(Dim: 768, Cosine, 30-Day TTL)")]:::storage
            QDraft[("draft_states\n(Payload-Indexed Multi-Drafts)")]:::storage
            QKB[("upyog_knowledge_base\n(FAQ & Municipal Knowledge)")]:::storage
        end
    end

    %% TIER 6: UPYOG MUNICIPAL BACKEND ECOSYSTEM
    subgraph T6["6. UPYOG Enterprise Microservices Ecosystem"]
        UserSvc["egov-user & OAuth Service\n(/user/oauth/token)"]:::external
        MDMSSvc["MDMS Master Data Service\n(Ad Types, PGR Categories, Tariffs)"]:::external
        LocSvc["egov-location Boundary Service\n(Wards, Localities, Boundaries)"]:::external
        PGRSvc["pgr-services v2\n(Grievance Dispatch & Tracking)"]:::external
        AdvSvc["adv-services v1\n(Booking, Inventory, Slot Reservation)"]:::external
        BillSvc["billing-service v2\n(Demand Calculation & Fetch Bill)"]:::external
        FileSvc["filestore Service\n(Document & Identity Proof Storage)"]:::external
    end

    %% INTERACTIONS & FLOWS
    BrowserUI -->|Microphone Stream| WebSpeech
    WebSpeech -->|Recognized Text + Session ID| Nginx
    Nginx --> FlaskServer
    FlaskServer --> DynEnv
    DynEnv --> ChatRoutes & AuthRoutes & StaticRoutes

    ChatRoutes --> LangDetect
    LangDetect --> IntentClassifier
    IntentClassifier --> DraftManager
    IntentClassifier -->|FAQ / Query Intent| RAGModule
    RAGModule --> QKB
    RAGModule --> GroqLLM

    DraftManager --> PluginRouter
    PluginRouter --> AdvGraph & PGRGraph
    AdvGraph & PGRGraph --> MCPBridge

    %% State persistence links
    AdvGraph & PGRGraph -.->|Fast Turn Checkpoint| RAMCheck
    ChatRoutes -.->|Real-time Context & Session Log| RedisStore
    DraftManager <-->|Save / Resume Multi-Drafts| QDraft
    ChatRoutes -.->|Semantic Memory Archive| QMem

    %% Backend tool calls
    MCPBridge --> MDMSSvc & LocSvc & AdvSvc & PGRSvc & BillSvc & FileSvc
    AuthRoutes --> UserSvc

    %% Synthesis & Audio delivery
    GroqLLM & AdvGraph & PGRGraph --> TTSGateway
    TTSGateway --> EdgeTTS
    TTSGateway -.->|Fallback| BhashiniTTS
    EdgeTTS & BhashiniTTS -->|Base64 Audio Payload| ChatRoutes
    ChatRoutes -->|JSON: Text + Voice + UI Controls| BrowserUI
    BrowserUI --> AudioPlayer
```

---

## 2. End-to-End Real-Time Voice Processing Pipeline

This sequence diagram illustrates the lifecycle of a single conversational turn, highlighting the low-latency design and barge-in capability.

```mermaid
sequenceDiagram
    autonumber
    actor Citizen as Citizen (Voice / Text)
    participant UI as Browser (Web Speech + Audio)
    participant Gateway as Flask Gateway (chat_routes)
    participant Intent as Intent & Lang Detector
    participant Workflow as LangGraph / RAG Engine
    participant Storage as Redis & Qdrant Cluster
    participant LLM as Groq LPU (Llama 3.1)
    participant UPYOG as UPYOG Microservices
    participant TTS as Speech Synthesis (Edge / Bhashini)

    Citizen->>UI: Speaks: "Water supply is not working in my ward"
    UI->>UI: Web Speech API streams continuous transcription
    UI->>Gateway: POST /chat {query, session_id, history}
    
    rect rgb(240, 248, 255)
        Note over Gateway, Intent: Step 1: Ingestion & Language Classification
        Gateway->>Intent: detect_language(query)
        Intent-->>Gateway: {lang: "en", script: "english", search_lang: "en"}
        Gateway->>Intent: classify_intent(query, history)
        Intent-->>Gateway: {intent: "grievance", service: "pgr", emotion: "frustrated"}
    end

    rect rgb(254, 249, 231)
        Note over Gateway, Storage: Step 2: Context & Draft Resolution
        Gateway->>Storage: Fetch active draft & recent context (Redis / Qdrant)
        Storage-->>Gateway: Active draft found: None | Phone: 9876543210
    end

    rect rgb(245, 238, 248)
        Note over Gateway, UPYOG: Step 3: Transactional Execution
        Gateway->>Workflow: Execute PGR Plugin Workflow
        Workflow->>UPYOG: Fetch categories (MDMS RAINMAKER-PGR)
        UPYOG-->>Workflow: Return service definitions
        Workflow->>LLM: Generate natural voice response + UI prompt
        LLM-->>Workflow: "I can help register a water supply complaint..."
    end

    rect rgb(232, 248, 245)
        Note over Gateway, TTS: Step 4: Speech Synthesis & Audio Delivery
        Workflow->>TTS: text_to_speech("I can help register...", lang="en")
        TTS-->>Gateway: Base64 Encoded Audio stream
        Gateway->>Storage: Update Redis chat history + sliding window
        Gateway-->>UI: 200 OK {response, audio, lang, mode: "pgr"}
    end

    UI->>Citizen: Audio playback + dynamic form cards rendered
    
    opt Citizen Barge-in (Interruption)
        Citizen->>UI: Citizen starts speaking again mid-playback
        UI->>Gateway: POST /stop {session_id}
        UI->>UI: Immediately abort HTML5 audio buffer
        UI->>Gateway: POST /chat (New Query)
    end
```

---

## 3. Multi-Tier State & Persistence Topology

A major architectural highlight of the v2 bot is the **3-layer memory hierarchy**, balancing millisecond latency with multi-week persistence:

```mermaid
flowchart LR
    subgraph L1["Layer 1: Ephemeral (Sub-Millisecond)"]
        RAM["RAM MemorySaver\n(langgraph.checkpoint)\n• Short-term turn state\n• Active node graph pointers"]
    end

    subgraph L2["Layer 2: Fast Cache & Session (1-2 ms)"]
        Redis["Redis Cache & Backbone\n• Active user session tokens\n• Rolling chat transcripts\n• Citizen profile cache\n• RESP2/RESP3 auto-negotiation"]
    end

    subgraph L3["Layer 3: Long-Term & Vector (3-5 ms)"]
        direction TB
        Q1[("long_term_chat_memory\n• 768-dim embeddings\n• Automated 30-Day TTL eviction\n• Cross-session semantic recall")]
        Q2[("draft_states\n• Phone:Plugin module isolation\n• Multi-draft pause / resume\n• Zero data collisions")]
        Q3[("upyog_knowledge_base\n• Municipal FAQs & FRS vectors\n• Cosine similarity search")]
    end

    L1 <--> L2
    L2 <--> L3
```

---

## 4. Key Architectural Pillars for Presentation Slides

### Pillar 1: Voice-First, Telephone-Call Experience
- **Continuous Listening**: Eliminates "push-to-talk" friction using browser Web Speech API.
- **Full Barge-in (Interruption)**: Citizens can interrupt the bot at any time; playback halts instantly and the server resets state cleanly.
- **Sub-2s Turnaround**: Average response latency between 1.5s – 2.5s from end-of-utterance to audio playback.

### Pillar 2: Scalable State & Multi-Draft Engine
- **Module Isolation (`phone_number:plugin_name`)**: A citizen can simultaneously hold an Advertisement Booking draft and a Grievance filing draft without overwrite collisions.
- **Resilient Fallback**: Every storage layer features transparent in-memory fallbacks (e.g., Redis down -> in-memory hash store; Qdrant down -> embedded memory client).

### Pillar 3: Ultra-Fast Inference Stack
- **Groq LPUs**: Powered by `llama-3.1-8b-instant` and specialized models providing 500+ tokens/second.
- **Dual Vector Architecture**: FAISS for fast local similarity and Qdrant gRPC for distributed cluster deployments.

### Pillar 4: Linguistic Inclusivity & Code-Switching
- **Per-Turn Dynamic Detection**: Seamlessly handles Hindi in Devanagari script, pure English, and colloquial Roman Hindi (Hinglish).
- **Dual TTS Engine**: High-fidelity Microsoft Edge-TTS for primary synthesis with Govt AI4Bharat / Bhashini fallback for regional dialect accuracy.

### Pillar 5: Deep Municipal Integration
- **Direct UPYOG Core Connectivity**: Authenticates via OAuth 2.0, pulls live MDMS configurations, resolves hierarchical GIS boundary trees, and files real municipal tickets with instant SMS notifications.

---

## 5. Latency Budget Breakdown (Slide Graphic)

| Processing Phase | Component / Engine | Typical Duration | Optimization Strategy |
| :--- | :--- | :--- | :--- |
| **Speech-to-Text** | Web Speech API | ~200 – 400 ms | Local/Edge browser processing |
| **Language & Intent** | Rule-Regex + Groq 8B | ~150 – 250 ms | Fast 150-token classifier, regex pre-routing |
| **Context & Vector Search** | Qdrant gRPC / FAISS | ~5 – 30 ms | Pre-indexed 768-dim vectors, cosine metric |
| **Workflow / API Call** | LangGraph + UPYOG | ~200 – 450 ms | Async HTTP sessions, cached MDMS masters |
| **LLM Token Generation** | Groq LPU | ~300 – 500 ms | High-throughput LPU hardware, max 300 tokens |
| **Speech Synthesis (TTS)**| Edge-TTS / Bhashini | ~400 – 700 ms | Text pre-cleaning pipeline, async audio chunks |
| **Network Round-Trip** | Flask + Nginx (Gzip) | ~50 – 100 ms | Localized hosting, compressed JSON payload |
| **Total End-to-End** | Full System Turn | **~1.5 – 2.4 sec** | **World-class conversational voice standard** |
