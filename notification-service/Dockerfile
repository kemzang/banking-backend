# Service de notification (Node.js) — consommateur RabbitMQ + API d'historique
FROM node:20-alpine
WORKDIR /app
# Cache des dependances
COPY package*.json ./
RUN npm install --omit=dev
# Code source
COPY . .
EXPOSE 3000
CMD ["node", "index.js"]
