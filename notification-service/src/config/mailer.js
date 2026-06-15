'use strict';

const nodemailer = require('nodemailer');

/**
 * Transporter Nodemailer.
 * En développement : Ethereal (faux SMTP, emails visibles sur https://ethereal.email).
 * En production : renseigner SMTP_HOST, SMTP_PORT, SMTP_USER, SMTP_PASS.
 */
async function createTransporter() {
  if (process.env.SMTP_HOST) {
    return nodemailer.createTransport({
      host:   process.env.SMTP_HOST,
      port:   parseInt(process.env.SMTP_PORT  || '587'),
      secure: process.env.SMTP_SECURE === 'true',
      auth: {
        user: process.env.SMTP_USER,
        pass: process.env.SMTP_PASS,
      },
    });
  }

  // Mode dev : Ethereal auto-généré
  const testAccount = await nodemailer.createTestAccount();
  console.log('[Mailer] Compte Ethereal :', testAccount.user);
  return nodemailer.createTransport({
    host:   'smtp.ethereal.email',
    port:   587,
    secure: false,
    auth: { user: testAccount.user, pass: testAccount.pass },
  });
}

module.exports = { createTransporter };
