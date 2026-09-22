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

# Habilita MIME types corretos para módulos .mjs e templates .ftl (pdf.js / ngx-extended-pdf-viewer)
RUN sed -i 's/application\/javascript                           js;/text\/javascript                                  js mjs;\n    application\/javascript                           js mjs;/' /etc/nginx/mime.types && \
    sed -i 's/text\/plain                                       txt;/text\/plain                                       txt ftl;/' /etc/nginx/mime.types

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
