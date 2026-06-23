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

// Historique en memoire des dernieres notifications (consultable par le front).
const recent = [];
const MAX_RECENT = 50;
let seq = 0;
function addNotification(event) {
  recent.unshift({ id: ++seq, receivedAt: new Date().toISOString(), ...event });
  if (recent.length > MAX_RECENT) recent.pop();
}

app.get('/health', (_req, res) => res.json({ status: 'UP', service: 'notification-service' }));
app.get('/',       (_req, res) => res.json({ message: 'notification-service is running' }));

// Liste des notifications recentes (consommee par le frontend via la gateway)
app.get('/api/notifications', (req, res) => {
  const roles = String(req.header('X-User-Roles') || '').split(',').map((role) => role.trim());
  if (!roles.includes('CLIENT')) return res.json(recent);

  const email = String(req.header('X-User-Email') || '').toLowerCase();
  const own = recent.filter((notification) => {
    const owner = notification.userEmail || notification.clientEmail || notification.email;
    return owner && String(owner).toLowerCase() === email;
  });
  return res.json(own);
});

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
      addNotification(event);   // memorise pour l'historique expose au front
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
