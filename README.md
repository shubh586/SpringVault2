# 🔐 App Vault — Unified Credential Vault for Microservices

A centralized, secure credential vault that stores encrypted secrets and delivers them dynamically to microservices. Built with Spring Boot 3, PostgreSQL, and AES-256-GCM encryption.

**Say goodbye to hardcoded passwords, `.env` files, and secret sprawl across services.**

---

## ✨ Features

- **AES-256-GCM Encryption** — Every secret encrypted with a unique IV before storage
- **ABAC Policy Engine** — Attribute-based access control (service, environment, region, path)
- **Audit Logging** — Every access recorded: who, what, when, from where, success/failure
- **Token Authentication** — SHA-256 hashed service tokens (raw tokens never stored)
- **Secret Rotation** — Update secrets without redeploying services
- **Zero-Code Integration** — Add one dependency, configure 4 lines of YAML, done
- **3 Integration Methods** — `@Value`, `@VaultSecret` annotation, programmatic `VaultClient`
- **Web Dashboard** — Monitor secrets, policies, and audit trail in real-time

---

## ⚡ Why App Vault? — What Makes It Different

### Comparison with Existing Solutions

| Capability | HashiCorp Vault | Spring Cloud Vault | AWS Secrets Manager | **App Vault** |
|---|---|---|---|---|
| **ABAC Policy Engine** (service + env + region + path) | ❌ Path-based ACL only | ❌ No policies (client only) | ⚠️ Via IAM tags (external, complex) | ✅ **Built-in multi-attribute ABAC** |
| **`@VaultSecret` Custom Annotation** | ❌ (Server is Go) | ❌ (`@Value` / `@ConfigurationProperties` only) | ❌ (Programmatic SDK only) | ✅ **Custom annotation with defaults** |
| **Pre-startup secret injection** | ❌ (Needs Spring Cloud or Vault Agent) | ✅ (via ConfigData API) | ❌ | ✅ **Built-in `EnvironmentPostProcessor`** |
| **Built-in web dashboard** | ✅ Basic UI (Enterprise adds Namespaces, Sentinel) | ❌ | ✅ (AWS Console, vendor-locked) | ✅ **Free, self-hosted, with audit trail** |
| **Server + Client in same language** | ❌ Server=Go, Client=Java | ❌ Client only (no server) | ❌ Cloud-managed only | ✅ **Both in Java, single repo** |
| **One-command setup** | ❌ (Init + Unseal ceremony required) | ❌ (Needs a running Vault server) | ❌ (Needs AWS account + IAM setup) | ✅ **`docker-compose up` — done** |
| **Audit trail with dashboard** | File/syslog/socket (raw JSON, no UI) | ❌ | CloudTrail (separate service) | ✅ **Real-time web dashboard** |
| **Self-hosted, no vendor lock-in** | ✅ (BSL 1.1 license since 2023) | Partial (needs HashiCorp Vault) | ❌ (AWS-locked) | ✅ **MIT License** |

---

### 🛡️ Key Differentiator #1: ABAC Policy Engine

Most secret vaults use simple **path-based ACL** — *"this token can read `/secret/*`"*. App Vault uses **Attribute-Based Access Control (ABAC)** which evaluates **4 attributes simultaneously**:

```
┌──────────────────────────────────────────────────────────┐
│                    ABAC Policy Evaluation                 │
│                                                          │
│   Request:  service=order-service                        │
│             environment=prod                             │
│             region=us-east                               │
│             path=database/credentials                    │
│                                                          │
│   Policy:   service=order-service  ✅ match              │
│             environment=prod       ✅ match              │
│             region=us-east         ✅ match              │
│             path=database/*        ✅ wildcard match     │
│             effect=ALLOW                                 │
│                                                          │
│   Result:   ✅ ACCESS GRANTED                            │
│                                                          │
│   ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─  │
│                                                          │
│   But same service from staging?                         │
│             environment=staging    ❌ no match            │
│                                                          │
│   Result:   ❌ ACCESS DENIED                             │
└──────────────────────────────────────────────────────────┘
```

**Real-world scenario this solves:**
> Your `payment-service` in **production** needs access to Stripe API keys.
> But the same `payment-service` in **staging** should NEVER see production keys.
> With path-based ACL, you can't enforce this. With ABAC, one policy handles it:
> ```json
> {
>   "serviceName": "payment-service",
>   "environment": "prod",
>   "region": "us-east",
>   "allowedPaths": "api/stripe/*",
>   "effect": "ALLOW"
> }
> ```
> Staging payment-service? **Denied automatically.** Different region? **Denied automatically.**

DENY policies always override ALLOW — giving you a **deny-first security model** similar to AWS IAM.

---

### 🎯 Key Differentiator #2: `@VaultSecret` Annotation

Neither HashiCorp Vault, Spring Cloud Vault, nor AWS SDK provide a **field-level annotation** for secret injection. App Vault does:

```java
// ❌ HashiCorp / Spring Cloud Vault — only @Value, no vault-specific annotation
@Value("${database.credentials.password}")
private String password;
// Problem: You can't tell if this comes from vault, env var, or application.yml

// ✅ App Vault — explicit vault annotation (your code is self-documenting)
@VaultSecret(path = "database/credentials", key = "password")
private String password;
// Clear: this MUST come from the vault. Path and key are explicit.
// Supports defaultValue if vault is unreachable.
```

**Why this matters:**
- **Self-documenting** — Any developer reading the code instantly knows this field comes from the vault
- **Explicit path + key** — No guessing which property maps to which secret
- **Default values** — `@VaultSecret(path = "...", key = "...", defaultValue = "fallback")` for graceful degradation
- **Works after startup** — Unlike `@Value` (resolved once at startup), `@VaultSecret` can work with the live `VaultClient` and cached values

---

### 🖥️ Key Differentiator #3: Built-in Dashboard

HashiCorp Vault's UI is **Enterprise-only** (paid). Spring Cloud Vault has **no UI at all**. App Vault includes a free, real-time monitoring dashboard:

- **Live audit trail** — See every secret access with service name, action, timestamp, and status
- **Policy management** — Create, view, and delete ABAC policies from the UI
- **System stats** — Total secrets, active policies, 24h access count, denied requests
- **Filter & pagination** — Filter audit logs by action type (READ, WRITE, ROTATE, DELETE)

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                      Any Microservice                           │
│                                                                 │
│  application.yml:                                               │
│    password: ${database.credentials.password}  ← from vault    │
│                                                                 │
│  ┌─────────────────────┐                                        │
│  │    vault-client      │  ← just add this dependency           │
│  │  (auto-configured)   │                                       │
│  └──────────┬──────────┘                                        │
└─────────────┼──────────────────────────────────────────────────┘
              │ X-Vault-Token
              ▼
┌──────────────────────────────────────────┐
│         Vault Server (:8200)              │
│                                          │
│  ┌──────────┐  ┌───────────┐  ┌───────┐ │
│  │ Policy   │  │ Encryption│  │ Audit │ │
│  │ Engine   │  │ AES-256   │  │ Logs  │ │
│  └──────────┘  └───────────┘  └───────┘ │
│                     │                    │
│              ┌──────▼──────┐             │
│              │ PostgreSQL  │             │
│              └─────────────┘             │
└──────────────────────────────────────────┘
```

---

## 🚀 Quick Start

### Option 1: Docker Compose (Recommended)

```bash
# Clone the repo
git clone https://github.com/YOUR_USERNAME/App-Vault.git
cd App-Vault

# Build the project
mvn clean package -DskipTests

# Start everything (PostgreSQL + Vault + Dashboard)
docker-compose up -d

# Vault is now running at http://localhost:8200
# Dashboard at http://localhost:3000
```

### Option 2: Local Development

```bash
# Prerequisites: Java 17+, Maven, PostgreSQL

# 1. Create the database
psql -U postgres -c "CREATE DATABASE vault_db;"

# 2. Set environment variables
export VAULT_MASTER_KEY=your-base64-encoded-32-byte-key
export VAULT_ADMIN_TOKEN=your-secure-admin-token

# 3. Build and run
mvn clean install -DskipTests
cd vault-server && mvn spring-boot:run
```

---

## 📦 Using the Vault Client Library

### Step 1: Publish the library (one-time setup)

**Option A: GitHub Packages** (recommended for teams)

Add to `vault-client/pom.xml`:
```xml
<distributionManagement>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/YOUR_USERNAME/App-Vault</url>
    </repository>
</distributionManagement>
```

Then publish:
```bash
mvn deploy -pl vault-client
```

**Option B: JitPack** (easiest — no setup needed)

Just push your code to GitHub. Any developer can use it via JitPack:

```xml
<!-- Add JitPack repository -->
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<!-- Add the dependency -->
<dependency>
    <groupId>com.github.YOUR_USERNAME</groupId>
    <artifactId>vault-client</artifactId>
    <version>1.0.0</version>
</dependency>
```

**Option C: Local install** (for development)

```bash
mvn clean install -DskipTests
# vault-client is now in your local .m2 repository
```

### Step 2: Add to your microservice

```xml
<dependency>
    <groupId>com.appvault</groupId>
    <artifactId>vault-client</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Step 3: Configure `application.yml`

```yaml
vault:
  url: http://localhost:8200          # Vault server URL
  token: ${VAULT_SERVICE_TOKEN}       # From environment variable
  environment: dev                    # dev / staging / prod
  secret-paths:                       # Paths to fetch at startup
    - database/credentials
    - api/payment
  fail-on-error: false                # true = fail startup if vault is down
```

### Step 4: Use secrets (pick any method)

```java
// Method 1: @Value — secrets available as Spring properties (RECOMMENDED)
@Value("${database.credentials.password}")
private String dbPassword;

// Method 2: @VaultSecret — Custom annotation
@VaultSecret(path = "database/credentials", key = "password")
private String dbPassword;

// Method 3: Programmatic — VaultClient bean
@Autowired
private VaultClient vaultClient;
String pw = vaultClient.getSecret("database/credentials", "password");
```

**That's it. No other code changes needed.** The library handles everything automatically.

---

## 🔧 Admin Operations

### Generate a service token
```bash
curl -X POST http://localhost:8200/api/v1/auth/token \
  -H "Content-Type: application/json" \
  -H "X-Vault-Token: YOUR_ADMIN_TOKEN" \
  -d '{"serviceName":"my-service","environment":"dev","region":"us-east"}'
```

### Store a secret
```bash
curl -X POST http://localhost:8200/api/v1/secrets \
  -H "Content-Type: application/json" \
  -H "X-Vault-Token: YOUR_ADMIN_TOKEN" \
  -d '{"path":"database/credentials","key":"password","value":"SuperSecret@123","environment":"dev"}'
```

### Create an access policy
```bash
curl -X POST http://localhost:8200/api/v1/policies \
  -H "Content-Type: application/json" \
  -H "X-Vault-Token: YOUR_ADMIN_TOKEN" \
  -d '{"name":"my-service-dev","serviceName":"my-service","environment":"dev","allowedPaths":"database/*","effect":"ALLOW"}'
```

### Rotate a secret
```bash
curl -X PUT http://localhost:8200/api/v1/secrets \
  -H "Content-Type: application/json" \
  -H "X-Vault-Token: YOUR_ADMIN_TOKEN" \
  -d '{"path":"database/credentials","key":"password","value":"NewPassword@456","environment":"dev"}'
```

### View audit logs
```bash
curl http://localhost:8200/api/v1/audit/logs?page=0&size=20 \
  -H "X-Vault-Token: YOUR_ADMIN_TOKEN"
```

---

## 📁 Project Structure

```
App_Vault/
├── vault-server/           # Central vault service (Spring Boot)
├── vault-client/           # Client library (add as dependency)
├── vault-dashboard/        # Web monitoring UI (HTML/CSS/JS)
├── restaurant-service/     # Real-world demo — uses vault for DB creds
├── sample-microservice/    # Demo showing all 3 integration methods
├── docker-compose.yml      # One-command deployment
├── Dockerfile              # Vault server container
└── pom.xml                 # Parent POM (multi-module)
```

---

## 🔐 Security Model

| Layer | Implementation |
|-------|---------------|
| Encryption | AES-256-GCM with unique IV per secret |
| Authentication | SHA-256 hashed tokens (`X-Vault-Token` header) |
| Authorization | ABAC policies (service + environment + region + path) |
| Audit | Async logging of every operation |
| Transport | TLS recommended in production |

---

## 📊 API Reference

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/v1/health` | — | Health check |
| GET | `/api/v1/dashboard/summary` | — | Dashboard stats |
| POST | `/api/v1/auth/token` | Admin | Generate service token |
| POST | `/api/v1/secrets` | Service | Store a secret |
| GET | `/api/v1/secrets` | Service | Fetch a secret |
| PUT | `/api/v1/secrets` | Service | Rotate a secret |
| DELETE | `/api/v1/secrets` | Service | Delete a secret |
| POST | `/api/v1/secrets/batch` | Service | Batch fetch |
| POST | `/api/v1/policies` | Admin | Create policy |
| GET | `/api/v1/policies` | Admin | List policies |
| DELETE | `/api/v1/policies/{id}` | Admin | Delete policy |
| GET | `/api/v1/audit/logs` | Admin | Audit logs |

---

## 🛠️ Tech Stack

- **Backend**: Java 17, Spring Boot 3.2, Spring Security, Spring Data JPA
- **Database**: PostgreSQL 16
- **Encryption**: AES-256-GCM (javax.crypto)
- **Dashboard**: Vanilla HTML/CSS/JS
- **Containerization**: Docker, Docker Compose

---

## 📜 License

MIT License — Use it, modify it, build on it.
