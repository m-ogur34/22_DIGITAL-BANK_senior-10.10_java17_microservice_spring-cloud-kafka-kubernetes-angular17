# ================================================================
# Auth Service Dockerfile — Multi-stage build
# Multi-stage: Build ortamı ile runtime ortamını ayırır
# Sonuç: Daha küçük image (JDK yerine JRE), daha hızlı deploy
# ================================================================

# ── Stage 1: Build aşaması ──
# Maven + JDK 17 ile JAR dosyası derlenir
FROM eclipse-temurin:17-jdk-alpine AS builder

# Çalışma dizini
WORKDIR /build

# ── Bağımlılık cache optimizasyonu ──
# Önce sadece pom.xml kopyalanır → bağımlılıklar indirilir
# Kod değişmeden pom değişmedikçe bu layer cache'den gelir
COPY pom.xml .
COPY ../pom.xml /root-pom.xml

# Maven bağımlılıklarını önceden indir (cache katmanı)
# -B: batch mode (interaktif sorular yok)
# -q: quiet (gereksiz çıktı yok)
RUN mvn -f /root-pom.xml dependency:go-offline -B -q 2>/dev/null || true

# Kaynak kodları kopyala
COPY src ./src

# JAR'ı derle — testleri atla (runtime'da çalıştırılacak)
RUN mvn package -DskipTests -B -q

# ── Stage 2: Runtime aşaması ──
# Sadece JRE kullanır — JDK gerekmez → ~200MB daha küçük image
FROM eclipse-temurin:17-jre-alpine AS runtime

# Güvenlik: root olmayan kullanıcı ile çalıştır
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

WORKDIR /app

# Build aşamasından yalnızca JAR'ı kopyala
COPY --from=builder /build/target/*.jar app.jar

# Servis portu
EXPOSE 8081

# JVM tuning parametreleri:
# -XX:+UseContainerSupport: Docker container memory limitini otomatik algıla
# -XX:MaxRAMPercentage: Container memory'sinin %75'ini heap için kullan
# -Djava.security.egd: Entropy source — container'da /dev/random yavaş olabilir
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
