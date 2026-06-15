'use strict';

/**
 * Génère le sujet et le corps HTML de l'email
 * selon le type et le statut de la transaction.
 */
function buildEmail(event) {
  const { reference, type, montant, devise, statut, motif, compteSourceId, compteDestId, dateExecution } = event;

  const montantFmt = new Intl.NumberFormat('fr-FR', {
    style: 'currency', currency: devise || 'XAF', minimumFractionDigits: 0,
  }).format(montant);

  const libelles = {
    DEPOT:    'Dépôt',
    RETRAIT:  'Retrait',
    TRANSFERT:'Transfert',
  };

  const icones = { SUCCES: '✅', ECHEC: '❌', EN_COURS: '⏳' };

  const sujet = `${icones[statut] || ''} [BankingApp] ${libelles[type] || type} ${montantFmt} — ${reference}`;

  const ligneComptes = type === 'TRANSFERT'
    ? `<tr><td>Compte source</td><td>#${compteSourceId}</td></tr>
       <tr><td>Compte destination</td><td>#${compteDestId}</td></tr>`
    : type === 'DEPOT'
    ? `<tr><td>Compte crédité</td><td>#${compteDestId}</td></tr>`
    : `<tr><td>Compte débité</td><td>#${compteSourceId}</td></tr>`;

  const html = `
<!DOCTYPE html>
<html lang="fr">
<head><meta charset="UTF-8"><style>
  body { font-family: Arial, sans-serif; background: #f4f6f8; margin:0; padding:20px; }
  .card { background:#fff; border-radius:8px; padding:24px; max-width:520px; margin:auto;
          box-shadow:0 2px 8px rgba(0,0,0,.1); }
  h2 { color: ${statut === 'SUCCES' ? '#1a7a4a' : statut === 'ECHEC' ? '#c0392b' : '#2980b9'}; }
  table { width:100%; border-collapse:collapse; margin-top:16px; }
  td { padding:8px 12px; border-bottom:1px solid #eee; font-size:14px; }
  td:first-child { color:#666; font-weight:600; width:40%; }
  .footer { margin-top:20px; font-size:12px; color:#999; text-align:center; }
</style></head>
<body>
  <div class="card">
    <h2>${icones[statut] || ''} ${libelles[type] || type} — ${statut}</h2>
    <table>
      <tr><td>Référence</td><td><strong>${reference}</strong></td></tr>
      <tr><td>Montant</td><td><strong>${montantFmt}</strong></td></tr>
      ${ligneComptes}
      ${motif ? `<tr><td>Motif</td><td>${motif}</td></tr>` : ''}
      <tr><td>Date</td><td>${dateExecution || new Date().toISOString()}</td></tr>
      <tr><td>Statut</td><td>${statut}</td></tr>
    </table>
    <div class="footer">BankingApp — notification automatique, ne pas répondre à cet email.</div>
  </div>
</body>
</html>`;

  return { sujet, html };
}

module.exports = { buildEmail };
