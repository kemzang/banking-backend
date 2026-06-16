# Activer la connexion Google (Sign in with Google)

Le code est **déjà en place** (back-end + front). Il manque uniquement un
**ID client OAuth Google**, à créer une fois dans la console Google (gratuit).

## 1. Créer l'ID client (Google Cloud Console)
1. https://console.cloud.google.com/ → créer un projet.
2. **API & Services → Écran de consentement OAuth** : type « Externe », renseigner le nom de l'app.
3. **API & Services → Identifiants → Créer des identifiants → ID client OAuth**.
4. Type d'application : **Application Web**.
5. **Origines JavaScript autorisées** : `http://localhost:4200`.
6. Copier l'**ID client** (forme `xxxxxx.apps.googleusercontent.com`).

## 2. Configurer le back-end
Dans `.env` (à la racine) :
```
GOOGLE_CLIENT_ID=xxxxxx.apps.googleusercontent.com
```
Puis : `docker compose up -d auth-service`

## 3. Configurer le front
Dans `frontend-app/src/environments/environment.ts` :
```ts
googleClientId: 'xxxxxx.apps.googleusercontent.com',
```
Puis relancer `ng serve`. Le bouton « Se connecter avec Google » apparaît alors
sur la page de connexion.

## Comment ça marche (pour le rapport)
1. Le front charge **Google Identity Services**, affiche le bouton Google.
2. Après connexion Google, le front reçoit un **ID token** (JWT signé par Google).
3. Il l'envoie à `POST /api/auth/google`.
4. `auth-service` **vérifie** la signature + l'audience (`GoogleIdTokenVerifier`),
   extrait l'email, crée l'utilisateur s'il n'existe pas, et délivre **notre propre JWT**.
5. La suite est identique à une connexion classique.

> C'est le **2ᵉ mécanisme d'authentification** (en plus de email/mot de passe),
> exigé par le sujet. Tant que `GOOGLE_CLIENT_ID` est vide, l'endpoint répond
> `503` (fonctionnalité désactivée proprement).
