// URL de base des API. En dev, '/api' est redirige vers la Gateway (:8080)
// par le proxy (proxy.conf.json) -> evite les problemes de CORS.
export const environment = {
  production: false,
  apiUrl: '/api',
  // ID client OAuth Google (Google Cloud Console). Laisser vide pour desactiver
  // le bouton "Se connecter avec Google".
  googleClientId: '1067870807360-bqodaps1kjnjfaokhonvm66ep3f9s7hu.apps.googleusercontent.com',
};
