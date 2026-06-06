# Service de notification (Node.js)
FROM node:20-alpine
WORKDIR /app
# Cache des dependances
COPY package*.json ./
RUN npm install --omit=dev
# Code source (index.js, src/...) — a ajouter
COPY . .
EXPOSE 3000
CMD ["node", "index.js"]
