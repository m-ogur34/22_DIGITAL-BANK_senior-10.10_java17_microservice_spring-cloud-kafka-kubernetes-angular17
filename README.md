# DigitalBank Platform

Mülakat hazırlığı ve öğrenim amaçlı kapsamlı bir fintech platformu.

## Teknoloji Stack

| Katman | Teknoloji |
|--------|-----------|
| Backend | Java 17, Spring Boot 3.2, Spring Security 6, JPA |
| Auth | JWT (JJWT 0.12.3) — Access 15dk, Refresh 7gün |
| Cache | Redis 7 — bakiye cache, token blacklist, rate limit |
| Mesajlaşma | Apache Kafka — işlem ve kredi olayları |
| Arama | Elasticsearch 8 — full-text işlem arama |
| Veritabanı | PostgreSQL 16 — servis başına ayrı schema |
| Migration | Flyway — versiyonlu schema yönetimi |
| Frontend | Angular 17 — Standalone Components, Signals |
| UI | Angular Material 17 |
| Container | Docker, Docker Compose, Kubernetes |
| Build | Maven Multi-module |

## Servisler

| Servis | Port | Açıklama |
|--------|------|----------|
| auth-service | 8081 | Kullanıcı kayıt/giriş, JWT yönetimi |
| account-service | 8082 | IBAN hesap yönetimi, bakiye |
| transaction-service | 8083 | EFT/Havale, Saga pattern, Elasticsearch |
| loan-service | 8084 | Kredi başvuru, Decorator pattern hesaplama |
| notification-service | 8085 | Kafka consumer, bildirim gönderimi |
| frontend | 4200 | Angular 17 SPA |

## OOP Tasarım Desenleri

- **Inheritance** — `BaseUser → Customer/Employee`, `BaseAccount → Vadesiz/Vadeli/Tasarruf/Yatırım`
- **Strategy** — `TransferStrategy`: dahili vs harici transfer seçimi
- **Decorator** — `LoanCalculator`: Sigorta + Dosya masrafı zincirleme
- **Saga** — `TransferSaga.compensate()`: başarısız transferlerin geri alınması
- **Factory** — `AccountFactory`: hesap tipine göre nesne oluşturma

## Hızlı Başlangıç

### Docker Compose (Önerilen)

```bash
# Tüm servisleri başlat
docker compose up -d

# Logları izle
docker compose logs -f auth-service

# Durdur
docker compose down -v
```

**Başlangıç sırası**: Zookeeper → Kafka → PostgreSQL × 5 → Redis → Elasticsearch → Servisler → Frontend

### Yerel Geliştirme

#### Gereksinimler
- Java 17+
- Maven 3.9+
- Node.js 20+
- Docker (altyapı servisleri için)

```bash
# 1. Altyapıyı başlat (sadece DB, Redis, Kafka, ES)
docker compose up -d auth-db account-db transaction-db loan-db notification-db redis kafka zookeeper elasticsearch

# 2. common-lib derle
cd common-lib && mvn install -DskipTests

# 3. Backend servislerini başlat (her biri ayrı terminal)
cd auth-service        && mvn spring-boot:run
cd account-service     && mvn spring-boot:run
cd transaction-service && mvn spring-boot:run
cd loan-service        && mvn spring-boot:run
cd notification-service && mvn spring-boot:run

# 4. Frontend başlat
cd frontend && npm install && npm start
```

Uygulama: http://localhost:4200

## Test Kullanıcıları (Seed Data)

| E-posta | Şifre | Rol |
|---------|-------|-----|
| admin@digitalbank.com | Admin123! | ADMIN |
| ali.kaya@example.com | Admin123! | CUSTOMER |
| ayse.demir@example.com | Admin123! | CUSTOMER |
| mehmet.yilmaz@example.com | Admin123! | CUSTOMER |

## API Örnekleri

### Giriş
```http
POST http://localhost:8081/auth/login
Content-Type: application/json

{
  "email": "ali.kaya@example.com",
  "password": "Admin123!"
}
```

### Hesap Listesi
```http
GET http://localhost:8082/api/accounts/my
Authorization: Bearer <access_token>
```

### Para Transferi
```http
POST http://localhost:8083/api/transactions/transfer
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "senderIban": "TR330006100012345678901234",
  "receiverIban": "TR330006100098765432109876",
  "amount": 1000.00,
  "description": "Test transferi"
}
```

### Kredi Başvurusu
```http
POST http://localhost:8084/api/loans/apply
Authorization: Bearer <access_token>
X-Monthly-Income: 15000
Content-Type: application/json

{
  "loanType": "IHTIYAC",
  "amount": 50000,
  "termMonths": 24,
  "disbursementIban": "TR330006100012345678901234",
  "sigortaIsteniyor": true
}
```

### İşlem Arama (Elasticsearch)
```http
GET http://localhost:8083/api/transactions/search?keyword=market&startDate=2024-01-01T00:00:00Z&page=0&size=10
Authorization: Bearer <access_token>
```

## Kubernetes

```bash
# Namespace ve secret oluştur
kubectl apply -f k8s/infrastructure/

# Tüm servisleri deploy et
kubectl apply -f k8s/auth-service/
kubectl apply -f k8s/account-service/
kubectl apply -f k8s/transaction-service/
kubectl apply -f k8s/loan-service/
kubectl apply -f k8s/notification-service/
kubectl apply -f k8s/frontend/

# Durumu kontrol et
kubectl get pods -n digitalbank
kubectl get services -n digitalbank
```

## Proje Yapısı

```
digitalbank/
├── common-lib/              # Paylaşılan: JWT, entities, exceptions
├── auth-service/            # Port 8081 — Kimlik doğrulama
├── account-service/         # Port 8082 — Hesap yönetimi
├── transaction-service/     # Port 8083 — Transfer + Elasticsearch
├── loan-service/            # Port 8084 — Kredi + Decorator
├── notification-service/    # Port 8085 — Kafka consumer
├── frontend/                # Angular 17 SPA
├── k8s/                     # Kubernetes manifests
├── docker-compose.yml       # Tüm servisleri başlat
└── pom.xml                  # Maven multi-module root
```

## Güvenlik Notları

- JWT secret **production'da** güçlü rastgele değer olmalı (`openssl rand -base64 64`)
- `k8s/infrastructure/secrets.yaml` dosyasındaki değerleri production'da **asla** commit etme
- Kubernetes'te `kubectl create secret` veya Vault kullan
- Tüm servisler JWT'yi **bağımsız doğrular** — Gateway olmadan zero-trust mimarisi

---

## Öğrenilen Kavramlar

### OOP & Tasarım Desenleri
```
Inheritance (Kalıtım):
  BaseUser → Customer, Employee       (IS-A ilişkisi)
  BaseAccount → VadesizHesap, VadeliHesap, TasarrufHesap, YatirimHesap

Polymorphism (Çok Biçimlilik):
  faizHesapla()  → Her hesap türü farklı formül uygular
  islemIzniVar() → VadeliHesap'ta vade dolmadan çekim yasak

Strategy Pattern:
  TransferStrategy ← InternalTransferStrategy | ExternalTransferStrategy
  TransferContext: runtime'da IBAN'a bakarak hangisi kullanılacağını seçer

Decorator Pattern:
  LoanCalculator ← BaseLoanCalculator
                 ← SigortaDecorator(wrapped)
                 ← DosyaMasrafiDecorator(wrapped)
  Zincirleme: DosyaMasrafi(Sigorta(Base)) → her katman üzerine masraf ekler

Saga Pattern:
  Dağıtık transaction: Transfer 4 adım, her adım local transaction
  Başarısız adımda compensating transaction (geri alma işlemi)
```

### Spring Boot & JPA
```
@MappedSuperclass       → BaseEntity tüm entity'lere id, createdAt, version ekler
@Inheritance(JOINED)    → Hesap türleri accounts + alt tablolar JOIN ile sorgulanır
@DiscriminatorColumn    → Hangi alt sınıf olduğunu DB'de discriminator kolonu tutar
@Version                → Optimistic locking: eş zamanlı bakiye güncellemesi güvenli
@Transactional(
  isolation=REPEATABLE_READ) → Finansal işlemde dirty read engellenir
```

### Microservices
```
Database per Service    → Her servis kendi PostgreSQL schema'sı (cross-schema FK yok)
Zero-trust JWT          → Her servis JWT'yi bağımsız doğrular (auth-service'e bağımlılık yok)
Kafka Events            → transfer.completed, loan.approved eventleri async yayınlanır
Flyway Migration        → Schema versiyonları takip edilir, rollback mümkün
Redis                   → Bakiye cache (15dk TTL) + token blacklist + rate limiter
```

---

## Mülakat Soruları

**Q: Microservice mimarisinde distributed transaction nasıl yönetilir?**
A: 2-phase commit (2PC) distributed sistemlerde nadiren kullanılır — koordinatör tek hata noktasıdır ve lock süresi uzundur. Saga pattern tercih edilir: her servis lokal transaction yapar, hata durumunda compensating transaction (telafi işlemi) çalışır. Bu projede TransferSaga: (1) Debit kaydı, (2) Credit kaydı, (3) Elasticsearch kayıt, (4) Kafka event. 3. adım başarısız olursa 1 ve 2 geri alınır.

**Q: Strategy Pattern neden if-else'den üstündür?**
A: if-else (`if (internal) {...} else {...}`) her yeni transfer türünde mevcut kodu değiştirmek gerektirir (Open/Closed Principle ihlali). Strategy Pattern'da yeni bir sınıf (örn: CryptoTransferStrategy) yazılır, TransferContext'e eklenir — mevcut kod değişmez. Test edilebilirlik artar: her strateji izole test edilir.

**Q: Decorator Pattern neden inheritance yerine tercih edildi?**
A: Inheritance ile her kombinasyon için ayrı sınıf gerekir: `BaseWithSigorta`, `BaseWithDosyaMasrafi`, `BaseWithSigortaAndDosya` = N sınıf. Decorator ile runtime'da zincir kurulur: `DosyaMasrafi(Sigorta(Base))`. Yeni masraf türü → sadece yeni Decorator sınıfı. Combinatorial explosion önlenir.

**Q: @Inheritance(JOINED) vs SINGLE_TABLE farkı?**
A: SINGLE_TABLE: Tüm alt sınıf alanları tek tabloda, kullanılmayanlar NULL. Sorgular hızlı (JOIN yok) ama NULL alanlar veritabanını kirletiyor ve NOT NULL constraint konulamıyor. JOINED: Her alt sınıf kendi tablosunda, sorgularda JOIN gerekli. Veri tutarlılığı daha iyi, NULL sorun yok, constraint konulabilir. Fintech'te veri tutarlılığı kritik → JOINED seçildi.

**Q: JWT'yi her serviste bağımsız doğrulamak neden önemli?**
A: Auth-service'e her istekte gidilseydi auth-service tek hata noktası (SPOF) olurdu ve latency artar. JWT imzalı olduğu için secret key ile her servis bağımsız doğrular. Token blacklist (logout sonrası) Redis'te tutulur — her servis Redis'e sorarak invalid token'ları yakalar.

**Q: @Version ile Optimistic Locking nasıl çalışır?**
A: Her entity'de `@Version private Long version` alanı vardır. Hibernate güncelleme sırasında: `UPDATE accounts SET balance=?, version=6 WHERE id=1 AND version=5`. Eğer başka bir transaction versiyon=5'i güncellemiş ise bu UPDATE 0 satır etkiler → `OptimisticLockException` fırlatılır → uygulama yeniden deneyebilir veya kullanıcıya hata döner. Bu sayede eş zamanlı iki transfer aynı hesaptan yapılamaz.

**Q: Kafka vs Redis Pub/Sub farkı nedir?**
A: Redis Pub/Sub: fire-and-forget, mesaj kalıcı değil (consumer offline ise mesaj kaybolur), hız kritik durumlar için. Kafka: mesajlar disk'e yazılır, consumer offline olsa bile tüketebilir, partition ile yatay ölçekleme, consumer group ile yük dağıtımı, replayable. Finansal event'ler (transfer, kredi) için Kafka tercih edilir — mesaj kaybı kabul edilemez.

**Q: Database per Service neden önemlidir?**
A: Ortak DB: Bir servisin şema değişikliği diğer servisleri etkiler, tüm servisler aynı DB'ye bağımlı (SPOF), bağımsız ölçekleme mümkün değil. Database per Service: Her servis bağımsız migrate edilebilir, bağımsız ölçeklenir, farklı DB teknolojisi (PostgreSQL + MongoDB + Redis karışık) kullanılabilir. Dezavantaj: Cross-service join mümkün değil → event-driven veya API composition gerekli.

**Q: Flyway vs Liquibase farkı?**
A: İkisi de versiyonlu DB migration aracı. Flyway: SQL/Java migration dosyaları, basit, convention-over-configuration (V1__init.sql), geniş topluluk. Liquibase: XML/YAML/JSON/SQL formatları, rollback desteği daha güçlü, daha kompleks. Production'da hangisi olursa olsun migration'lar test ortamında test edilmeli.
