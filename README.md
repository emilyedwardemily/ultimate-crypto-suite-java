# 🛡️ UltimateCryptoSuite

<div align="center">

### Enterprise-Grade Cryptographic & Cybersecurity Simulation Suite

> **UltimateCryptoSuite is not an app. It is an arsenal.**
>
> A standalone, enterprise-grade cryptographic and cybersecurity simulation platform that fuses a
> **production-grade AES-256 / RSA / ChaCha20 encryption engine**, a **visual cryptanalysis laboratory**,
> a **full Reverse-Engineering simulator**, an **AI security mentor**, and **offensive Red Team attack labs** —
> all in one neon-drenched, cyberpunk-grade desktop interface backed by a distributed FastAPI + MongoDB Atlas cloud stack.

[![Java](https://img.shields.io/badge/Java-25-%23ED8B00?logo=java&logoColor=white)](https://openjdk.org/projects/jdk/25/)
[![JavaFX](https://img.shields.io/badge/JavaFX-25-%2300B0FF?logo=java&logoColor=white)](https://openjfx.io)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.1xx-%23009688?logo=fastapi&logoColor=white)](https://fastapi.tiangolo.com)
[![MongoDB Atlas](https://img.shields.io/badge/MongoDB%20Atlas-Cloud-%2347A248?logo=mongodb&logoColor=white)](https://www.mongodb.com/atlas)
[![Render](https://img.shields.io/badge/Render-Hosted-%2346E3B7?logo=render&logoColor=white)](https://render.com)
[![CI/CD](https://img.shields.io/badge/CI%2FCD-GitHub%20Actions-%232088FF?logo=githubactions&logoColor=white)](.github/workflows/ci-cd.yml)
[![Made by](https://img.shields.io/badge/Crafted%20by-Emily%20Edward-%23FFD700)](https://github.com/emilyedwardemily)

</div>

---

## ⚡ TL;DR

| Capability | What you get |
| --- | --- |
| 🔐 **Cryptography Labs** | AES-256 (GCM), RSA, ECC, DES, Blowfish, Twofish, ChaCha20, Salsa20, SHA family, HMAC, PBKDF2, Argon2, XOR & classical ciphers — with live step-by-step visualization. |
| 🧠 **Visual Learning Engine** | Every algorithm rendered as an animated, color-coded state machine. Watch bytes flow, keys expand, and rounds transform — byte by byte. |
| 🤖 **AI Mentor** | An always-on, in-app security tutor that diagnoses your mistakes, explains attacks in plain language, and curates your next lesson. |
| 🔬 **Reverse Engineering Simulator** | Assembly playground, disassembler, decompiler, PE/ELF/Mach-O analyzers, Control-Flow-Graph builder, stack/heap/register visualizers, ROP & shellcode labs. |
| 📐 **DSA in Security** | Data Structures & Algorithms taught *through* a security lens — Merkle trees, Bloom filters, segment trees, AVL/RB/B-trees — each mapped to real crypto/malware/network/blockchain applications. |
| 🚨 **Red Team Attack Labs** | Simulated offensive scenarios: hash cracking, padding-oracle attacks, bit-flip forgery, differential & linear cryptanalysis, RSA weak-key scanning, network packet crafting and more. |

---

## 🎯 Key Feature Breakdown

### 🔐 Cryptography Labs
- **Modern symmetric & asymmetric suites**: AES-256 (GCM/CBC/CTR), RSA, ECC, DES, Blowfish, Twofish, ChaCha20, Salsa20.
- **Hashing & KDFs**: SHA-1/256/512, HMAC, PBKDF2, Argon2 — with rainbow-table resistance demos and entropy scoring.
- **Classical ciphers**: Caesar, Vigenère, Playfair, Hill, Transposition, XOR — complete with cryptanalysis tools.
- **Key management**: session keys held in RAM only; zero-knowledge ciphertext storage; certificate generation with **scannable HMAC-signed QR codes** and backend signature verification.

### 🧠 Visual Learning Engine
- **Animated state machines** for AES key expansion, Feistel networks, S-box substitution, and round functions.
- **Step / autoplay / replay** controls so you can slow an algorithm to a crawl and watch every transformation.
- Algorithm *and* attack visualizers that show exactly where a defense breaks — and why.

### 🤖 AI Mentor
- Natural-language Q&A over every topic in the suite.
- Misstep diagnosis with **plain-English explanations** of why your solution failed.
- **Adaptive lesson pathing** that recommends the next concept based on your weak families.
- Quick-ask chips, streak tracking, and an integrated `teach me` deep-dive mode.

### 🔬 Reverse Engineering Simulator
- **Assembly Playground**, disassembler, and decompiler with syntax highlighting.
- **PE / ELF / Mach-O binary analyzers**: sections, imports/exports, magic bytes, entropy maps.
- **Control-Flow-Graph (CFG) builder** and register/stack/heap/memory visualizers.
- **Exploitation primitives**: ROP-chain construction, shellcode modeling, and a debugger simulator.
- **Binary labs**: hex viewer, magic-byte detection, binary diffing, string extraction, entropy analysis.

### 📐 DSA in Security
- Every data structure taught **through its security application**:
  - Merkle trees → blockchain proofs & Git integrity
  - Bloom filters → malware sandbox membership / SSRF allowlists
  - Segment trees → packet-range firewalls & rate limiters
  - AVL/RB/B-trees → database indexes and key stores
  - Big-O analysis → spotting DoS-prone designs
- Each concept ships with a **4-quadrant application matrix** (Cryptography / Malware / Networking / Blockchain) so theory always lands in practice.

### 🚨 Red Team Attack Labs
- Hash cracking, frequency analysis, padding-oracle visualization, bit-flip forgery, differential & linear cryptanalysis.
- RSA weak-key scanner, entropy calculators, avalanche-effect meters.
- Network attack surface: protocol parsers, packet builders, JWT forgery decoder.
- Career Mode + CTF arenas with XP, global leaderboards, certificates and an audit trail.

---

## 🏗️ Architecture & Tech Stack

```
┌────────────────────────────────────────────────────────────────┐
│                    ULTIMATECRYPTOSUITE                           │
│                                                            │
│  ┌─────────────────────────┐   ┌──────────────────────────┐  │
│  │   JavaFX Desktop Client │   │   Next.js Web Frontend    │  │
│  │   (Java 25 + Maven)     │   │   (web/)                  │  │
│  └───────────┬─────────────┘   └───────────┬──────────────┘  │
│              │ HTTPS / API Key             │ HTTPS            │
└──────────────┼─────────────────────────────┼─────────────────┘
               ▼                             ▼
      ┌──────────────────┐          ┌──────────────────┐
      │  Node.js Gateway  │◄───────►│  FastAPI Backend  │
      │  (Express, JWT,   │          │  (Python, Uvicorn)│
      │   Helmet, bcrypt) │          │  /encrypt /verify │
      └────────┬─────────┘          └───────┬──────────┘
               │                            │
               ▼                            ▼
      ┌───────────────────────────────────────────────┐
      │              MongoDB Atlas (Cloud)             │
      │   Zero-knowledge ciphertext + audit logs       │
      └───────────────────────────────────────────────┘
```

| Layer | Technology | Role |
| --- | --- | --- |
| **Desktop Client** | Java 25 · JavaFX 25 · Maven | UI, local AES-256 encryption, visualizers, attack simulators |
| **Encryption Engine** | Python 3.11+ · FastAPI · Uvicorn | Server-side crypto endpoints, certificate verification, telemetry |
| **Cloud API Gateway** | Node.js · Express · JWT · Helmet · bcrypt | Auth bridge, MFA/OTP, session management |
| **Database** | MongoDB Atlas (Motor / PyMongo) | Zero-knowledge ciphertext storage, audit logs, leaderboards |
| **Deployment** | Render (Free-tier Cloud) · GitHub Actions · Docker | CI/CD with secret scanning, auto-deploy on `main` |
| **Crypto / PDF / QR** | BouncyCastle · iTextPDF · ZXing · Gson | Cryptographic primitives, signed audit PDFs, verifiable certificates |

> **Live endpoints**
> - Python backend: `https://ultimate-crypto-python.onrender.com`
> - Node gateway: `https://ultimate-crypto-node-gateway.onrender.com`

---

## 📸 Screenshots

> 🖼️ *Screenshots will be added here as the UI evolves. Replace the placeholders below with your captures — see the [Contributing](#-contributing) guide.*

| Dashboard | Cryptography Lab |
| --- | --- |
| ![Dashboard](docs/screenshots/dashboard.png) | ![Crypto Lab](docs/screenshots/crypto-lab.png) |

| AI Mentor | Reverse Engineering |
| --- | --- |
| ![AI Mentor](docs/screenshots/ai-mentor.png) | ![Reverse Engineering](docs/screenshots/reverse-engineering.png) |

| Red Team Lab | Certificates |
| --- | --- |
| ![Red Team Lab](docs/screenshots/red-team-lab.png) | ![Certificates](docs/screenshots/certificates.png) |

---

## 💻 Installation & Local Setup

### Prerequisites
- **JDK 25+** (project targets `maven.compiler.release=25`)
- **Maven 3.9+**
- **Python 3.11+**
- **Node.js 18+** (only if running the gateway locally)
- (Optional) **SDKMAN** — the repo ships a `.sdkmanrc` that auto-switches your Java version.

### 1. Clone the repository
```bash
git clone https://github.com/emilyedwardemily/ultimate-crypto-suite-java.git
cd ultimate-crypto-suite-java
```

### 2. Set up the Python backend
```bash
cd UC-BACKED
python3 -m venv venv
source venv/bin/activate        # Windows: venv\Scripts\activate
pip install -r requirements.txt
```

### 3. Configure environment variables
```bash
cp .env.example .env            # then fill in your secrets
```
Key values: `API_SECRET_KEY`, MongoDB Atlas connection string, Render service IDs (only needed for cloud features).

### 4. Run the Python backend
```bash
uvicorn main:app --reload --port 8000
```

### 5. Build & run the desktop client
```bash
cd ..                            # back to the project root
mvn clean package
mvn javafx:run
```

> **Linux / Debian package**: a prebuilt `ultimatecryptosuite_1.0_amd64.deb` is available for direct installation.
>
> **Docker**: the repo includes a multi-stage `Dockerfile` — build with `docker build -t ultimate-crypto-suite .`.

### 6. (Optional) Run the Node.js gateway locally
```bash
cd src/server
npm ci
node index.js
```

---

## 🚀 Roadmap Highlights

- [x] AES-256 GCM cascading engine & classical cipher suite
- [x] Visual learning engine with animated state machines
- [x] AI Mentor with adaptive lesson pathing
- [x] Red Team attack labs & CTF arenas
- [x] HMAC-signed verifiable certificates (QR + backend verification)
- [ ] Expanded UC-MODULES: UC-OS, UC-NETWORK, UC-CLOUD, UC-MOBILE, UC-WEB deep dives
- [ ] WebAssembly version of the crypto engine for the browser frontend
- [ ] Plugin marketplace for community cipher modules
- [ ] Bug-bounty-friendly fuzzing harnesses for all attack simulators

---

## 🤝 Sponsorship & Support

**UltimateCryptoSuite is an open, community-driven project** built for students, researchers, CTF players, and security professionals who learn better by *doing*.

We believe security education should be hands-on, visual, and — above all — free. Every sponsor directly funds new labs, better visuals, server uptime for the live backend, and scholarships for students who cannot afford commercial security training.

### How to support

| Method | How |
| --- | --- |
| 💖 **GitHub Sponsors** | Click the **Sponsor** button at the top of this repository — one-time or monthly. |
| 🔁 **One-time donation** | Use the sponsor tiers below or reach out directly. |
| 🧑‍💻 **Contribute code** | Open issues, pick up good-first-issues, and submit PRs (see [Contributing](#-contributing)). |
| 🐛 **Report findings** | Security researchers are welcome — responsibly disclose via our contact below. |
| 🗣️ **Spread the word** | Star ⭐ the repo, share it with your uni, bootcamp, or CTF team. |

### Sponsorship tiers
- **🥉 Hacker Tier — $5/mo** · Shout-out on the README sponsor wall.
- **🥈 Analyst Tier — $20/mo** · Shout-out + early access to new labs + priority issue triage.
- **🥇 Architect Tier — $50/mo** · All of the above + your name in the About/credits panel + feature voting rights.
- **💎 Institution Tier — $500+/mo** · Enterprise sponsorship: logo placement, training sessions, custom lab requests.

> **Questions about sponsorship or commercial licensing?**
> Reach out: **emilyedward211@gmail.com**

---

## 🤖 Contributing

We welcome contributors of every skill level — from your first PR to a senior reverse-engineering review.

### Getting started
1. **Fork** the repository and clone your fork.
2. Create a feature branch: `git checkout -b feat/your-feature`.
3. Follow the existing patterns in `src/main/java` (modules mirror the Crypto / Academy / UI layout).
4. **Never modify** the API logic in `ApiClient.java`, `DatabaseManager.java`, or `LoginController.java` without discussion.
5. Commit with a clear message, push, and open a **Pull Request** against `main`.

### Areas we always need help with
- New visualizers & animated algorithm demos
- Additional Red Team attack labs and CTF challenges
- Reverse-engineering simulator improvements (PE/ELF/Mach-O parsing, ROP, shellcode)
- Test coverage — unit + integration tests for the crypto and academy modules
- Documentation, translation, and accessibility polish

### Code of conduct
Be constructive. Security education is serious work — keep discussions professional, and **only ever run attacks inside this sandboxed simulator**.

### Security disclosures
If you find a vulnerability in the simulator, backend, or build pipeline, **please do not post it publicly**. Contact us privately at **emilyedward211@gmail.com** with a PoC, and we will acknowledge you in our security hall of fame.

---

## 📜 License

This project is distributed under a **commercial license**. All source code and associated assets are the exclusive property of **Emily Edward**. Unauthorized copying, modification, distribution, or use via any medium is strictly prohibited.

For commercial licensing, redistribution, or academic use, please contact **emilyedward211@gmail.com**.

---

<div align="center">

**Built with ⚡ by Emily Edward** · [GitHub](https://github.com/emilyedwardemily) · [Report a Bug](../../issues) · [Request a Feature](../../issues)

*"If you want to master cryptography, stop reading about it — attack it."*

</div>
