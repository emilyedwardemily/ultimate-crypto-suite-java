# UltimateCryptoSuite — Environment

## Java
- **SDKMAN** manages Java versions
- This project uses **Java 25.0.4-ea** (linked to `/usr/lib/jvm/java-25-openjdk-amd64`)
- `.sdkmanrc` in project root enables auto-switching when `cd`ing into directory

## Build & Run
```bash
# Build (sdkman auto-switches to Java 25)
mvn clean package

# Run
mvn javafx:run

# Or with explicit Java home:
JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 mvn javafx:run
```

## Backend
- Python backend hosted at Render: `https://ultimate-crypto-python.onrender.com`
- Node gateway: `https://ultimate-crypto-node-gateway.onrender.com`
- All API calls go through `ApiClient.java` / `Dashboard.java` (do NOT modify API endpoints or secrets)

## Important
- Do NOT change existing API logic in `ApiClient.java`, `DatabaseManager.java`, `LoginController.java`
- New features should follow patterns in existing modules (XOR, Caesar, etc.)
