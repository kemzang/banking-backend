'use strict';

const { buildEmail } = require('../templates/email');

/**
 * Traite un événement transaction.completed reçu depuis RabbitMQ.
 * - Log en console (toujours)
 * - Envoi d'un email (si transporter disponible)
 */
async function handleTransactionEvent(event, transporter) {
  const { reference, type, montant, devise, statut, compteSourceId, compteDestId } = event;

  // ---- LOG CONSOLE ----
  console.log(
    `[Notification] ${new Date().toISOString()} | ${type} | ${reference} | ${montant} ${devise} | ${statut}`
  );

  // ---- EMAIL ----
  const destinataire = process.env.NOTIFICATION_EMAIL || 'admin@bankingapp.cm';

  try {
    const { sujet, html } = buildEmail(event);
    const info = await transporter.sendMail({
      from:    `"BankingApp" <${process.env.SMTP_USER || 'noreply@bankingapp.cm'}>`,
      to:      destinataire,
      subject: sujet,
      html,
    });
    console.log(`[Mailer] Email envoyé → ${destinataire} | messageId: ${info.messageId}`);

    // Lien de prévisualisation en mode Ethereal (dev)
    const nodemailer = require('nodemailer');
    const previewUrl = nodemailer.getTestMessageUrl(info);
    if (previewUrl) console.log(`[Mailer] Prévisualisation : ${previewUrl}`);

  } catch (err) {
    console.error('[Mailer] Échec envoi email :', err.message);
    // On ne rejette pas — le message RabbitMQ sera quand même acquitté
  }
}

module.exports = { handleTransactionEvent };
