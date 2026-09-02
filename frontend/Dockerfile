# ============================================================
#  Etapa 1: build de Vite
#  Node 22: Vite 8 requiere Node 20 o superior.
# ============================================================
FROM node:22-alpine AS build
WORKDIR /build

COPY package*.json ./
RUN npm ci

COPY . .

# Las variables VITE_* se resuelven EN TIEMPO DE BUILD: quedan escritas
# dentro del JS generado. Por eso la URL de la API no puede ser
# "http://localhost:8080/api/v1" (esa URL no existe para el navegador
# de otra maquina). Se usa una ruta relativa y nginx hace de proxy.
ARG VITE_API_BASE_URL=/api/v1
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

RUN npm run build

# ============================================================
#  Etapa 2: servir los estaticos con nginx
# ============================================================
FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /build/dist /usr/share/nginx/html
EXPOSE 80
