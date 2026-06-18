# ---- Etape 1 : build de l'app Angular ----
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build -- --configuration production

# ---- Etape 2 : service statique via Nginx ----
FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/frontend-app/browser /usr/share/nginx/html
EXPOSE 80
