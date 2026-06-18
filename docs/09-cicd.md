# Intégration & Déploiement Continus (CI/CD)

Pipeline défini dans [`.github/workflows/ci.yml`](../.github/workflows/ci.yml),
exécuté par **GitHub Actions** à chaque `push` et `pull request`.

## Étapes (jobs)
| Job | Rôle |
|-----|------|
| **java-services** (matrice) | Build + tests (`./mvnw verify`) des 8 services Spring Boot, en parallèle |
| **ai-document** | Python : `pip install` + `pytest` (service OCR) |
| **notification** | Node : `npm install` (service notifications) |
| **frontend** | Angular : `npm ci` + `npm run build` (production) |
| **docker-images** | Sur `main` uniquement : build des `.jar` puis `docker compose build` (toutes les images) |

## Logique
1. À chaque push/PR : on **compile et teste** chaque service dans sa techno.
2. La matrice Java permet de paralléliser et d'isoler les échecs par service.
3. Le job `docker-images` ne s'exécute **que sur `main`** (après succès des builds) :
   il valide que toutes les images Docker se construisent.

## Évolutions possibles (perspectives)
- Pousser les images vers un registre (GHCR / Docker Hub) via `docker/login-action`.
- Déploiement continu : `kubectl apply -f k8s/` vers un cluster (étape `deploy`).
- Analyse qualité (SonarQube), scan de sécurité des images (Trivy).
