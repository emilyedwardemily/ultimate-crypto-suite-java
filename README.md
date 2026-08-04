# 🛠️ Ultimate Crypto Suite (UC Suite Pro)

<div align="center">

[![Build](https://img.shields.io/badge/build-passing-brightgreen.svg)](.github/workflows/ci-cd.yml)
[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://openjdk.org/projects/jdk/25/)
[![JavaFX](https://img.shields.io/badge/JavaFX-25-%2300B0FF)](https://openjfx.io)
[![Python](https://img.shields.io/badge/Python-3.11%2B-blue)](https://www.python.org)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.1xx-009688)](https://fastapi.tiangolo.com)
[![Node.js](https://img.shields.io/badge/Node.js-18%2B-%23339933)](https://nodejs.org)
[![MongoDB Atlas](https://img.shields.io/badge/MongoDB%20Atlas-Cloud-%2347A248)](https://www.mongodb.com/atlas)
[![Deployed on Render](https://img.shields.io/badge/Deployed%20on-Render-%2346E3B7)](https://render.com)
[![CI/CD](https://img.shields.io/badge/CI%2FCD-GitHub%20Actions-%232088FF)](.github/workflows/ci-cd.yml)
[![Sponsors](https://img.shields.io/badge/Sponsor-GitHub%20Sponsors-ea4aaa)](https://github.com/sponsors/emilyedwardemily)

**Professional-Grade Cryptographic & Cybersecurity Simulation Platform**

> A multi-tier distributed architecture designed for secure client-side encryption, cryptographic auditing,
> interactive learning, and red-team cybersecurity simulations — powered by **JavaFX**, **FastAPI**, **Node.js**
> and **MongoDB Atlas**, deployed on **Render**.

</div>

---

## 📌 Overview

**Ultimate Crypto Suite (Professional Edition)** is a hybrid security platform combining client-side
zero-knowledge encryption, hardware-bound licensing, digital forensics generation, and an immersive
cybersecurity learning environment. It ships with modern cryptography standards, interactive simulation
modules (Cryptography, Steganography, Cryptanalysis, Binary Analysis), Data Structures & Algorithms taught
through a security lens, an x86/x64 Reverse-Engineering simulator, and a full Red Team attack laboratory.

The desktop application is backed by a distributed cloud stack: a **FastAPI** engine and a **Node.js** gateway
persisting ciphertext-only records in **MongoDB Atlas**, with CI/CD via **GitHub Actions** and hosting on **Render**.

---

## 🔒 Core Security Architecture

The platform relies on a 3-tier defense-in-depth model:

- **Client-Side Encryption (Java):** All sensitive data undergoes local encryption on the client device prior
  to transport, eliminating Man-in-the-Middle (MITM) attack vectors.
- **Zero-Knowledge Storage:** MongoDB Atlas holds raw ciphertext only. In the event of a database breach,
  zero plaintext data is accessible.
- **Hardware-ID (HWID) Binding:** Software license keys are cryptographically signed and bound to unique
  hardware signatures to prevent piracy and unauthorized instantiation.

Additional hardening:

- **HMAC-signed verifiable certificates** — certificate QR codes embed a `HmacSHA256` signature that is
  validated against the backend `/verify-cert` endpoint.
- **Forensic audit trail** — every operator action generates a SHA-256 authenticity stamp stored in the cloud.

---

## ⚡ Cryptographic Standards & Forensic Audit

### Cryptography Specifications

- **Primary Algorithm:** AES-256 in **GCM Mode** (Galois/Counter Mode).
- **Security Assurance:** Authenticated encryption — guarantees both confidentiality and integrity.
- **Key Management:** Master keys reside exclusively in volatile client RAM during active sessions and are
  never transmitted or stored on cloud servers.

### Forensics & Audit Capabilities

- **Cryptographic Audit Logs:** Generates SHA-256 authenticity stamps for every system action.
- **Non-Repudiation:** Generates tamper-evident PDF audit reports suitable for compliance and legal evidence.
- **Automated Forensic Export:** One-click export of system security state logs to local storage for security analysts.

---

## 🧩 Feature Modules

### 🎓 UC ACADEMY (Learning-First)
| Module | Description |
| --- | --- |
| 📚 **Lessons & Learning Paths** | Structured security curriculum from classical ciphers to modern attacks. |
| 🚩 **CTF Challenges & Quizzes** | Timed capture-the-flag arenas with XP, streaks and global leaderboards. |
| 🤖 **AI Mentor** | Natural-language tutor with mistake diagnosis, plain-English explanations, and adaptive lesson pathing. |
| 🏆 **Certificates & Career Mode** | Signed, QR-verifiable certificates and role-based career progression. |

### 🧪 UC-LABS (Hands-On Tools)
| Module | Description |
| --- | --- |
| 🔐 **Cryptography Lab** | AES, RSA, ECC, DES, Blowfish, Twofish, ChaCha20, Salsa20, SHA, HMAC, PBKDF2, Argon2 + classical ciphers. |
| 🖼️ **Steganography** | LSB visualizer, payload extraction, image difference, audio stego, metadata viewer. |
| 📊 **Cryptanalysis** | Frequency analysis, bit-flip simulator, padding-oracle visualizer, differential/linear, RSA weak-key scanner, entropy, avalanche. |
| 🧬 **Binary Lab** | Hex viewer, magic-byte detection, PE/ELF header parsing, binary diffing, string extraction. |
| ⚙️ **UC-DEVELOPER** | API key management, SDK reference, and Python/Java/cURL snippet libraries with copy-to-clipboard. |

### 🔬 UC-REVERSE (Reverse Engineering)
Assembly playground, disassembler, decompiler, PE/ELF/Mach-O analyzers, Control-Flow-Graph builder, stack/heap/
register/memory visualizers, debugger simulator, ROP chains, shellcode modeling, binary patching, sandboxing,
import/export analysis, and entropy mapping.

### 📐 UC-DSA (Data Structures in Security)
Every data structure taught **through its security application**: Merkle trees → blockchain proofs, Bloom filters →
malware membership, segment trees → firewall ranges, AVL/RB/B-trees → key stores, Big-O analysis → DoS-resistant
designs — each mapped to a 4-quadrant Cryptography / Malware / Networking / Blockchain matrix.

### 🚨 Red Team Attack Labs
Hash cracking, frequency analysis, padding-oracle visualization, bit-flip forgery, differential & linear
cryptanalysis, RSA weak-key scanning, entropy calculators, avalanche-effect meters, and network packet
crafting/parsing.

### 🌐 Extended Modules
**UC-OS**, **UC-NETWORK**, **UC-CLOUD**, **UC-MOBILE**, **UC-WEB**, **UC-BLOCKCHAIN** — OS internals, network
protocol analysis, cloud security, mobile (APK/IPA/Frida/Objection), web (OWASP: XSS, CSRF, SQLi, SSRF, SSTI,
XXE, JWT, OAuth, CSP), and blockchain fundamentals.

---

## 🏗️ Architecture & Tech Stack

| Layer | Technology | Operational Role |
| :--- | :--- | :--- |
| **Frontend UI** | Java 25 · JavaFX · Maven | Native graphical interface & local AES engine |
| **Auth & Logic API** | Python 3.11 · FastAPI · Uvicorn | License verification, HWID lock, crypto/simulation backend |
| **Cloud Gateway** | Node.js · Express · JWT · bcrypt | High-throughput API gateway & secure cloud bridge |
| **Database** | MongoDB Atlas (Motor / PyMongo) | Global encrypted persistence layer |
| **Deployment** | Render · GitHub Actions · Docker | CI/CD with secret scanning and auto-deploy on `main` |
| **Cryptography** | BouncyCastle · iTextPDF · ZXing · Gson | Crypto primitives, signed audit PDFs, verifiable certificates |

```
┌──────────────────────────────────────────────────────────────┐
│                  UltimateCryptoSuite (JavaFX)                 │
│            client-side AES-256 · visualizers · labs           │
└──────────────┬───────────────────────────────┬───────────────┘
               │  HTTPS + API key               │  HTTPS + API key
               ▼                               ▼
     ┌───────────────────┐           ┌───────────────────┐
     │   Node.js Gateway  │◄────────►│   FastAPI Backend  │
     │ (Express, JWT,     │           │  (Python, Uvicorn) │
     │  bcrypt, Nodemailer)│           │ /encrypt · /verify │
     └─────────┬─────────┘           └─────────┬─────────┘
               │                               │
               ▼                               ▼
        ┌───────────────────────────────────────────────┐
        │           MongoDB Atlas (Cloud)                 │
        │   ciphertext-only storage + audit logs          │
        └───────────────────────────────────────────────┘
```

### Live Endpoints (Render)
- **FastAPI backend:** `https://ultimate-crypto-python.onrender.com`
- **Node.js gateway:** `https://ultimate-crypto-node-gateway.onrender.com`

---

## 🚀 Quick Start

### Prerequisites
- **JDK 25** (project targets `maven.compiler.release=25`) — see `.sdkmanrc` / SDKMAN auto-switching.
- **Maven 3.9+**
- **Python 3.11+**
- **Node.js 18+** (only if running the gateway locally)

### 1. Clone the repository

```bash
git clone https://github.com/emilyedwardemily/UltimateCryptoSuite.git
cd UltimateCryptoSuite
```

### 2. Configure the Python backend

```bash
cd UC-BACKED
python3 -m venv venv
source venv/bin/activate          # Windows: venv\Scripts\activate
pip install -r requirements.txt
uvicorn main:app --reload         # http://localhost:8000
```

### 3. (Optional) Run the Node.js gateway locally

```bash
cd src/server
npm install
node index.js
```

### 4. Build & run the desktop client

```bash
cd ..                            # project root
mvn clean package                # full build
mvn javafx:run                   # launch the JavaFX app
```

### 5. Environment variables

| Variable | Where | Purpose |
| --- | --- | --- |
| `API_SECRET_KEY` | Backend & gateway | Shared API key gate (`x-api-key`) |
| `MONGO_URI` | Backend & gateway | MongoDB Atlas connection string |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | Gateway | SMTP credentials for OTP email delivery |
| `JWT_SECRET` | Gateway | Session signing |

---

## 📂 Project Structure

```
UltimateCryptoSuite/
├── pom.xml                     # Maven build (Java 25, JavaFX 25)
├── src/main/java/
│   ├── app/                    # Launcher, MainApp, auth, DB manager
│   ├── ui/                     # Dashboard, Login/Register/Forgot screens
│   ├── academy/                # Lessons, CTF, AI Mentor, certificates
│   ├── crypto/                 # AES, RSA, XOR utilities
│   ├── steganography/          # LSB + audio stego tools
│   └── storage/                # MongoDB client layer
├── src/server/                 # Node.js Express gateway
├── UC-BACKED/                  # Python FastAPI backend (requirements.txt)
├── web/                        # Next.js web frontend (optional)
├── .github/workflows/ci-cd.yml # Secret scan → build → deploy to Render
└── Dockerfile                  # Multi-stage container build
```

---

## 🧪 Testing & CI/CD

- **Secret scanning:** TruffleHog on every push/PR.
- **Python:** dependency install + AST syntax verification.
- **Node.js:** `node --check` syntax verification + Docker build.
- **Web:** TypeScript `tsc --noEmit` + production build.
- **Deploy:** automatic Render deploy of Python + Node services on `main`.

---

## 📸 Screenshots

> 🖼️ Placeholders — add your captures under `docs/screenshots/`.

| Dashboard | Cryptography Lab | AI Mentor |
| --- | --- | --- |
| `docs/screenshots/dashboard.png` | `docs/screenshots/crypto-lab.png` | `docs/screenshots/ai-mentor.png` |

| Reverse Engineering | Red Team Lab | Certificates |
| --- | --- | --- |
| `docs/screenshots/reverse-engineering.png` | `docs/screenshots/red-team-lab.png` | `docs/screenshots/certificates.png` |

---

## 💖 Sponsorship & Support

<div align="center">

[![GitHub Sponsors](https://img.shields.io/badge/Sponsor_%40emilyedwardemily-%23EA4AAA?style=for-the-badge&logo=githubsponsors&logoColor=white)](https://github.com/sponsors/emilyedwardemily)

</div>

**UltimateCryptoSuite is an open, community-driven project** built for students, researchers, CTF players, and
security professionals who learn better by *doing*.

We believe security education should be hands-on, visual, and — above all — free. Every sponsor directly funds
new labs, better visuals, server uptime for the live backend, and scholarships for students who cannot afford
commercial security training.

> 🔮 **Your sponsorship accelerates what's next.** We're actively building a full **Reverse Engineering
> simulator** (disassembler, PE/ELF/Mach-O analyzers, ROP & shellcode labs), **DSA-in-Security modules**
> (Merkle trees, Bloom filters, AVL/RB/B-trees mapped to real crypto/malware/network/blockchain attacks), and
> **Advanced Cryptography tooling** (side-channel demos, cryptanalysis suites, and verifiable-certificate
> infrastructure). Every sponsor moves these from the roadmap into the app.

### How to support

| Method | How |
| --- | --- |
| 💖 **GitHub Sponsors** | Click the **Sponsor** button at the top of this repository — or [github.com/sponsors/emilyedwardemily](https://github.com/sponsors/emilyedwardemily). |
| 🔁 **One-time donation** | Use the sponsor tiers below or reach out directly. |
| 🧑‍💻 **Contribute code** | Open issues, pick up good-first-issues, and submit PRs (see [Contributing](#-contributing)). |
| 🐛 **Report findings** | Security researchers are welcome — responsibly disclose via our contact below. |
| 🗣️ **Spread the word** | Star ⭐ the repo, share it with your uni, bootcamp, or CTF team. |

### Sponsorship tiers
- **🥉 Hacker Tier — $5/mo** · Shout-out on the README sponsor wall.
- **🥈 Analyst Tier — $20/mo** · Shout-out + early access to new labs + priority issue triage.
- **🥇 Architect Tier — $50/mo** · All of the above + your name in the About/credits panel + feature voting rights.
- **💎 Institution Tier — $500+/mo** · Enterprise sponsorship: logo placement, training sessions, custom lab requests.

> **Questions about sponsorship or commercial licensing?** Reach out: **emilyedward211@gmail.com**

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
Be constructive. Security education is serious work — keep discussions professional, and **only ever run attacks
inside this sandboxed simulator**.

### Security disclosures
If you find a vulnerability in the simulator, backend, or build pipeline, **please do not post it publicly**.
Contact us privately at **emilyedward211@gmail.com** with a PoC, and we will acknowledge you in our security hall of fame.

---

## 📜 License

This project is distributed under a **commercial license**. All source code and associated assets are the exclusive
property of **Emily Edward**. Unauthorized copying, modification, distribution, or use via any medium is strictly
prohibited.

For commercial licensing, redistribution, or academic use, please contact **emilyedward211@gmail.com**.

---

<div align="center">

**Built with ⚡ by Emily Edward** · [GitHub](https://github.com/emilyedwardemily) · [Report a Bug](../../issues) · [Request a Feature](../../issues)

*"If you want to master cryptography, stop reading about it — attack it."*

</div>
