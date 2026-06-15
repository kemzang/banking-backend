'use strict';

const amqp = require('amqplib');

const RABBITMQ_URL  = process.env.RABBITMQ_URL || 'amqp://guest:guest@localhost:5672';
const EXCHANGE      = 'banking.events';
const QUEUE_NOTIF   = 'transaction.notifications';
const ROUTING_KEY   = 'transaction.completed';

const RETRY_DELAY_MS = 5000;  // 5 s entre chaque tentative de reconnexion
const MAX_RETRIES    = 12;    // ~1 minute de tentatives au démarrage

/**
 * Connexion à RabbitMQ avec retry automatique.
 * Renvoie { channel } une fois prêt.
 */
async function connect(attempt = 1) {
  try {
    console.log(`[RabbitMQ] Tentative de connexion #${attempt} → ${RABBITMQ_URL}`);
    const conn    = await amqp.connect(RABBITMQ_URL);
    const channel = await conn.createChannel();

    // Déclare l'exchange et la queue (idempotent)
    await channel.assertExchange(EXCHANGE, 'topic', { durable: true });
    await channel.assertQueue(QUEUE_NOTIF, { durable: true });
    await channel.bindQueue(QUEUE_NOTIF, EXCHANGE, ROUTING_KEY);

    // Traitement 1 message à la fois
    channel.prefetch(1);

    console.log('[RabbitMQ] Connecté ✅  — en écoute sur', QUEUE_NOTIF);

    conn.on('error',  (err) => console.error('[RabbitMQ] Erreur connexion :', err.message));
    conn.on('close',  ()    => {
      console.warn('[RabbitMQ] Connexion fermée — reconnexion dans 5 s…');
      setTimeout(() => connect(1), RETRY_DELAY_MS);
    });

    return channel;
  } catch (err) {
    console.error(`[RabbitMQ] Connexion échouée (#${attempt}) :`, err.message);
    if (attempt < MAX_RETRIES) {
      await new Promise(r => setTimeout(r, RETRY_DELAY_MS));
      return connect(attempt + 1);
    }
    throw new Error('[RabbitMQ] Impossible de se connecter après plusieurs tentatives');
  }
}

module.exports = { connect, QUEUE_NOTIF };
