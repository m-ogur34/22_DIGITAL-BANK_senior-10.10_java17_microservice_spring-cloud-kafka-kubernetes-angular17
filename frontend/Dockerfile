# Çok aşamalı build — final image sadece nginx + compiled JS
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
# ci: package-lock.json'u tam olarak kullan, daha hızlı ve deterministik
RUN npm ci
COPY . .
RUN npm run build

# Production: sadece Nginx + statik dosyalar — ~20MB image
FROM nginx:alpine AS runtime
COPY --from=builder /app/dist/digitalbank-frontend/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
