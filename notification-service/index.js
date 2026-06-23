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
  const notification = {
    id: ++seq,
    receivedAt: new Date().toISOString(),
    status: 'UNREAD',
    ...event
  };
  recent.unshift(notification);
  if (recent.length > MAX_RECENT) recent.pop();
  return notification;
}

app.get('/health', (_req, res) => res.json({ status: 'UP', service: 'notification-service' }));
app.get('/',       (_req, res) => res.json({ message: 'notification-service is running' }));

// Liste des notifications recentes (consommee par le frontend via la gateway)
app.get('/api/notifications', (req, res) => {
  const roles = String(req.header('X-User-Roles') || '').split(',').map((role) => role.trim());
  if (roles.includes('ADMIN_PLATFORM')) return res.json(recent);

  if (roles.includes('OPERATOR_ADMIN') || roles.includes('OPERATOR_AGENT')) {
    const operatorId = Number(req.header('X-Operator-Id'));
    if (!Number.isFinite(operatorId)) return res.status(403).json({ message: 'Identite operateur manquante' });
    return res.json(recent.filter((notification) => Number(notification.operatorId) === operatorId));
  }

  if (!roles.includes('CLIENT')) return res.status(403).json({ message: 'Acces refuse' });

  const email = String(req.header('X-User-Email') || '').toLowerCase();
  const own = recent.filter((notification) => {
    const owner = notification.userEmail || notification.clientEmail || notification.email;
    return owner && String(owner).toLowerCase() === email;
  });
  return res.json(own);
});

// Creation reservee aux appels directs des microservices. La gateway supprime
// X-Internal-Service des requetes publiques avant routage.
app.post('/api/notifications', (req, res) => {
  const caller = String(req.header('X-Internal-Service') || '');
  const allowed = ['customer-service', 'account-service', 'loan-service', 'transaction-service'];
  if (!allowed.includes(caller)) return res.status(403).json({ message: 'Appel interne refuse' });
  return res.status(201).json(addNotification(req.body || {}));
});

app.patch('/api/notifications/:id/read', (req, res) => {
  const roles = String(req.header('X-User-Roles') || '').split(',').map((role) => role.trim());
  const notification = recent.find((item) => item.id === Number(req.params.id));
  if (!notification) return res.status(404).json({ message: 'Notification introuvable' });

  const isPlatformAdmin = roles.includes('ADMIN_PLATFORM');
  const isOperatorOwner = (roles.includes('OPERATOR_ADMIN') || roles.includes('OPERATOR_AGENT'))
    && Number(notification.operatorId) === Number(req.header('X-Operator-Id'));
  const email = String(req.header('X-User-Email') || '').toLowerCase();
  const owner = String(notification.userEmail || notification.clientEmail || notification.email || '').toLowerCase();
  const isClientOwner = roles.includes('CLIENT') && owner && owner === email;
  if (!isPlatformAdmin && !isOperatorOwner && !isClientOwner) {
    return res.status(403).json({ message: 'Acces refuse' });
  }
  notification.status = 'READ';
  return res.json(notification);
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
