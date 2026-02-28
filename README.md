# 🔗 SmartURL — AI-Powered URL Shortener

A production-grade URL shortening service with a dual-model ML classifier that screens every URL for phishing and malicious content before shortening, full click analytics, JWT authentication, and Redis-backed redirects at sub-10ms latency.

> Built with Java/Spring Boot, Python/FastAPI, Redis, PostgreSQL, scikit-learn, and TensorFlow.

---

## 📸 Screenshots

![SmartURL Home](docs/home.png)

![SmartURL ML Models](docs/ml.png)

---

## 🏗️ Architecture

```
                    ┌─────────────────────────────────────┐
                    │         User / Browser               │
                    └──────┬───────────────────┬──────────┘
                           │                   │
                    Shorten URL           Click Short URL
                           │                   │
                           ▼                   ▼
                    ┌─────────────────────────────────────┐
                    │      SmartURL API (Spring Boot)      │
                    │                                      │
                    │  ┌──────────────┐ ┌──────────────┐  │
                    │  │  URL         │ │  Redirect    │  │
                    │  │  Shortener   │ │  Service     │  │
                    │  │  (Base62)    │ │  (302)       │  │
                    │  └──────────────┘ └──────────────┘  │
                    │  ┌──────────────┐ ┌──────────────┐  │
                    │  │  Analytics   │ │  JWT Auth    │  │
                    │  │  Tracker     │ │  Service     │  │
                    │  └──────────────┘ └──────────────┘  │
                    └──────┬──────────────────┬───────────┘
                           │                  │
                    ┌──────▼──────┐   ┌───────▼────────┐
                    │    Redis    │   │   PostgreSQL   │
                    │  (redirect  │   │  URLs, clicks, │
                    │   cache)    │   │  users         │
                    └─────────────┘   └────────────────┘
                           │
                    ┌──────▼──────────────────────────────┐
                    │     ML Classification Service        │
                    │         (Python / FastAPI)           │
                    │                                      │
                    │  ┌─────────────┐ ┌───────────────┐  │
                    │  │   Random    │ │    Neural     │  │
                    │  │   Forest    │ │    Network    │  │
                    │  │  87.97% acc │ │  85.47% acc   │  │
                    │  └─────────────┘ └───────────────┘  │
                    └─────────────────────────────────────┘
```

---

## ✨ Features

**AI-Powered URL Safety**
- Every URL screened by a dual-model classifier before shortening
- Random Forest (87.97% accuracy) + Neural Network (85.47% accuracy) trained on 549,000 real phishing URLs
- Both models must agree before a URL is blocked — minimises false positives
- Returns triggered features explaining why a URL was flagged

**High-Performance Redirects**
- Base62 encoding guarantees collision-free short codes (62^6 = 56 billion possible URLs)
- Redis-first lookup pattern: sub-10ms latency on cache hits, ~280ms on cold start (first click)
- HTTP 302 (not 301) ensures analytics are captured on every click — not just the first
- Async click recording never blocks the redirect response

**Full Click Analytics**
- Geographic distribution by country
- Device type breakdown (Mobile / Desktop / Tablet)
- Referrer source tracking
- Time-series click chart per URL

**User Tiers**
- Anonymous users: instant shortening, URLs expire after 30 days
- Registered users: permanent URLs, full analytics dashboard, JWT authentication

---

## 📊 Performance

| Metric | Result |
|---|---|
| Redirect latency (cache hit) | ~7–11ms |
| Redirect latency (cache miss) | ~280ms |
| Concurrent redirect throughput | 200+ req/sec |
| Concurrent shortening throughput | 50+ req/sec |
| RF Model accuracy | 87.97% |
| NN Model accuracy | 85.47% |
| Training dataset size | 549,346 URLs |

---

## 🛠️ Tech Stack

| Component | Technology |
|---|---|
| API | Java 17, Spring Boot 3 |
| Cache | Redis 7 |
| Database | PostgreSQL 15 |
| Authentication | JWT (jjwt 0.12.3) |
| ML Service | Python 3.11, FastAPI |
| ML Models | scikit-learn (Random Forest), TensorFlow/Keras (Neural Network) |
| Frontend | React 18, Chart.js |
| Containerization | Docker |

---

## 🚀 Getting Started

### Prerequisites
- Java 17+
- Python 3.11+
- Docker Desktop

### 1. Start Redis and PostgreSQL
```bash
docker run --name redis-smarturl -p 6379:6379 -d redis:7

docker run --name smarturl-postgres \
  -e POSTGRES_PASSWORD=password \
  -e POSTGRES_DB=smarturl \
  -p 5432:5432 -d postgres:15
```

### 2. Start the SmartURL API
```bash
cd smarturl
./mvnw spring-boot:run
```
API starts on `http://localhost:8082`

### 3. Start the ML Service
```bash
cd smarturl-ml
python3 -m venv venv
source venv/bin/activate
pip install fastapi uvicorn scikit-learn tensorflow numpy pandas
uvicorn app.main:app --reload --port 8083
```
ML service starts on `http://localhost:8083` and trains both models on startup (~2 minutes).

### 4. Open the Dashboard
Navigate to `http://localhost:8082`

---

## 📡 API Reference

### Shorten a URL
```
POST /api/v1/urls/shorten
```
```json
{
  "originalUrl": "https://example.com/very/long/path",
  "userId": "1"
}
```
**Response (safe URL):**
```json
{
  "shortCode": "000001",
  "shortUrl": "http://localhost:8082/r/000001",
  "originalUrl": "https://example.com/very/long/path",
  "safe": true,
  "mlConfidence": 0.03,
  "safetyMessage": "URL appears safe",
  "expiresAt": null
}
```
**Response (malicious URL):**
```json
{
  "shortCode": null,
  "shortUrl": null,
  "safe": false,
  "mlConfidence": 0.9758,
  "safetyMessage": "URL blocked — detected as malicious by AI classifier"
}
```

### Redirect
```
GET /r/{shortCode}
→ 302 redirect to original URL
```

### Analytics
```
GET /api/v1/urls/{shortCode}/analytics
```

### Auth
```
POST /api/v1/auth/register
POST /api/v1/auth/login
Body: { "email": "user@example.com", "password": "password123" }
```

### ML Classify
```
POST /ml/classify  (port 8083)
Body: { "url": "https://example.com" }
```

---

## 🧠 Design Decisions

**Why Base62 encoding and not random strings?**
Random strings require collision checking on every insert. Base62 encoding of a monotonically increasing database ID is mathematically guaranteed to be unique — no collision check needed. At 6 characters we support 56 billion URLs before needing to increase length.

**Why 302 redirect and not 301?**
301 is a permanent redirect — browsers cache it and never call your server again, losing all analytics data after the first click per browser. 302 ensures every click comes through your server, enabling complete analytics capture.

**Why Redis for redirects?**
PostgreSQL adds 5-20ms per query. Redis operates in-memory at under 1ms. For a redirect service, this latency difference is user-facing on every single click. Redis also handles traffic spikes gracefully — if a URL goes viral, Redis absorbs the load without hammering the database.

**Why both RF and NN instead of just one model?**
Model selection is a deliberate engineering decision. Random Forest is faster to train, more interpretable (feature importances are human-readable), and less prone to overfitting on imbalanced datasets. Neural Network achieves slightly higher precision (90.22% vs 89.64%) but lower recall. Requiring both models to agree before blocking reduces false positives — a legitimate URL is only blocked if both classifiers flag it independently.

**Why async click recording?**
Redirect latency is user-facing and must be under 10ms. Database writes add 5-20ms. Using Spring's `@Async`, clicks are recorded on a background thread — users get their redirect instantly while analytics are captured without blocking.

---

## 📁 Project Structure

```
smarturl/                           # Spring Boot service
├── src/main/java/com/smarturl/
│   ├── controller/
│   │   ├── UrlController.java
│   │   └── AuthController.java
│   ├── service/
│   │   ├── UrlService.java
│   │   ├── AuthService.java
│   │   ├── JwtService.java
│   │   └── MlClassifierClient.java
│   ├── repository/
│   │   ├── UrlRepository.java
│   │   ├── ClickRepository.java
│   │   └── UserRepository.java
│   ├── model/
│   │   ├── Url.java
│   │   ├── Click.java
│   │   └── User.java
│   ├── dto/
│   │   ├── ShortenRequest.java
│   │   ├── ShortenResponse.java
│   │   ├── AnalyticsResponse.java
│   │   └── AuthResponse.java
│   ├── config/
│   │   ├── RedisConfig.java
│   │   ├── SecurityConfig.java
│   │   └── AsyncConfig.java
│   └── util/
│       ├── Base62Encoder.java
│       └── UserAgentParser.java
└── src/main/resources/
    ├── static/index.html           # React frontend
    └── application.properties

smarturl-ml/                        # Python ML service
├── app/
│   ├── main.py                     # FastAPI endpoints
│   ├── models.py                   # RF + NN training and inference
│   ├── features.py                 # URL feature extraction (13 features)
│   └── schemas.py                  # Pydantic request/response models
└── data/
    └── phishing_site_urls.csv      # Kaggle phishing dataset (549k URLs)
```

---

## 🔮 Future Improvements

- Docker Compose for one-command startup
- Custom short codes chosen by user
- QR code generation per URL
- Kubernetes deployment with horizontal scaling
- Model retraining pipeline on real traffic logs
- Prometheus + Grafana observability
- Rate limiting per user (using the companion rate limiter project)

---

## 👩‍💻 Author

**Shilpa Lingadal**
Master of Science in Software Engineering Systems — Northeastern University
[LinkedIn](https://linkedin.com/in/your-profile) · [GitHub](https://github.com/your-username)
