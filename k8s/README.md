# Déploiement Kubernetes — Plateforme Bancaire

Manifests pour déployer toute la plateforme sur un cluster Kubernetes (testé avec **minikube**).

## Fichiers
| Fichier | Contenu |
|---------|---------|
| `00-namespace-config.yaml` | Namespace `banking`, ConfigMap `bank-config`, Secret `bank-secret` |
| `10-infra.yaml` | PostgreSQL (PVC + init multi-bases) + RabbitMQ |
| `20-core.yaml` | config-service, discovery (Eureka), gateway |
| `30-business.yaml` | auth, customer, account, transaction, loan |
| `40-polyglot-frontend.yaml` | ai-document (Python), notification (Node), frontend (Nginx) + Ingress |

## Prérequis
- Un cluster (ex. `minikube start`) + `kubectl`.
- L'addon ingress : `minikube addons enable ingress`.

## 1. Construire les images
Les manifests utilisent les images construites par Docker Compose
(`plateforme-bancaire-<service>:latest`). Construire puis les charger dans le cluster :
```bash
# à la racine du projet
docker compose build
# charger les images dans minikube (sinon imagePullPolicy: IfNotPresent ne les trouve pas)
for img in config-service discovery-service gateway-service auth-service \
           customer-service account-service transaction-service loan-service \
           ai-document-service notification-service frontend-app; do
  minikube image load plateforme-bancaire-$img:latest
done
```

## 2. Déployer (ordre conseillé)
```bash
kubectl apply -f k8s/00-namespace-config.yaml
kubectl apply -f k8s/10-infra.yaml
kubectl apply -f k8s/20-core.yaml      # attendre que discovery soit Ready
kubectl apply -f k8s/30-business.yaml
kubectl apply -f k8s/40-polyglot-frontend.yaml
```

## 3. Accéder à l'application
```bash
echo "$(minikube ip) banking.local" | sudo tee -a /etc/hosts
# puis ouvrir http://banking.local
# (ou) acces direct au frontend :
kubectl -n banking port-forward svc/frontend-app 4200:80
```

## Vérifier
```bash
kubectl -n banking get pods
kubectl -n banking logs deploy/gateway-service
```

> Le `GOOGLE_CLIENT_ID` est vide par défaut dans le Secret : renseignez-le
> (`kubectl -n banking edit secret bank-secret`, valeur en base64) pour activer la connexion Google.
