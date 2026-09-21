# syntax=docker/dockerfile:1

# ==============================================================================
# Stage 1: Build Angular SPA
# ==============================================================================
FROM node:22-alpine AS build
WORKDIR /app

# Install dependencies (utiliza package-lock.json para builds determinísticos)
COPY package*.json ./
RUN npm ci

# Copy source code and build production bundle
COPY . .
RUN npm run build -- --configuration production

# ==============================================================================
# Stage 2: Serve with lightweight Nginx
# ==============================================================================
FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
