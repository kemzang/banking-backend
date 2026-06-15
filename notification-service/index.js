'use strict';

const express  = require('express');
const { connect, QUEUE_NOTIF } = require('./src/config/rabbitmq');
const { createTransporter }    = require('./src/config/mailer');
const { handleTransactionEvent } = require('./src/handlers/transactionHandler');

const PORT = process.env.PORT || 3000;

// ------------------------------------------------------------------ //
//  Serveur HTTP léger — health check pour Docker / Kubernetes
// ------------------------------------------------------------------ //
const app = express();
app.use(express.json());

app.get('/health', (_req, res) => res.json({ status: 'UP', service: 'notification-service' }));
app.get('/',       (_req, res) => res.json({ message: 'notification-service is running' }));

app.listen(PORT, () => console.log(`[HTTP] notification-service démarré sur le port ${PORT}`));

// ------------------------------------------------------------------ //
//  Consommateur RabbitMQ
// ------------------------------------------------------------------ //
async function start() {
  let transporter;
  try {
    transporter = await createTransporter();
    console.log('[Mailer] Transporter prêt');
  } catch (err) {
    console.error('[Mailer] Impossible d\'initialiser le transporter :', err.message);
    // Le service continue sans email — les logs console restent actifs
  }

  const channel = await connect();

  channel.consume(QUEUE_NOTIF, async (msg) => {
    if (!msg) return;

    let event;
    try {
      event = JSON.parse(msg.content.toString());
    } catch (err) {
      console.error('[Consumer] Message non-JSON reçu, rejeté :', msg.content.toString());
      channel.nack(msg, false, false);  // dead-letter sans requeue
      return;
    }

    try {
      await handleTransactionEvent(event, transporter);
      channel.ack(msg);  // acquittement : message traité avec succès
    } catch (err) {
      console.error('[Consumer] Erreur traitement, requeue :', err.message);
      channel.nack(msg, false, true);  // requeue = true → réessai
    }
  });
}

start().catch((err) => {
  console.error('[FATAL] Démarrage impossible :', err.message);
  process.exit(1);
});
