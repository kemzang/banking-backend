# AI Document Service

Microservice REST construit avec FastAPI pour l'analyse numérique et
l'extraction de texte depuis des images avec OpenCV et Tesseract OCR.
L'historique des traitements OCR est conservé dans une base SQLite avec
SQLAlchemy 2.

## Fonctionnalités

- Vérification de l'état du service.
- Analyse statistique d'une liste de nombres.
- Upload d'images PNG, JPG et JPEG.
- Prétraitement OpenCV : niveaux de gris, réduction du bruit et seuillage.
- Extraction de texte avec Tesseract OCR.
- Score de fiabilité explicable de 0 à 100 et recommandation de décision.
- Détection simple des noms, dates et numéros de document sans données inventées.
- Historique persistant des analyses OCR.
- Réponses JSON et erreurs standardisées.
- Documentation interactive Swagger et ReDoc.
- Exécution locale ou avec Docker Compose.

## Architecture

Le projet suit une architecture inspirée de la Clean Architecture :

```text
app/
├── core/           # Base de données et gestion globale des erreurs
├── models/         # Modèles SQLAlchemy
├── repositories/   # Accès aux données
├── routes/         # Contrôleurs HTTP FastAPI
├── schemas/        # Contrats Pydantic
├── services/       # Logique métier, OCR et traitement d'image
├── utils/          # Réponses et gestion des fichiers
├── config.py       # Configuration centralisée
└── main.py         # Création de l'application FastAPI
```

Flux principal :

```text
Route -> Service -> Repository -> SQLAlchemy -> SQLite
             |
             +-> ImagePreprocessingService -> OpenCV -> Tesseract
```

Les routes gèrent uniquement HTTP et l'injection des dépendances. La logique
métier reste dans les services et les accès SQLite dans le repository.

## Prérequis

- Python 3.11 ou 3.12
- Tesseract OCR pour une exécution locale
- Docker Desktop pour une exécution conteneurisée

## Installation locale

Depuis le dossier `ai-document-service` :

```powershell
python -m venv venv
.\venv\Scripts\Activate.ps1
python -m pip install --upgrade pip
pip install -r requirements.txt
Copy-Item .env.example .env
```

Installer Tesseract sous Windows, puis vérifier que ce fichier existe :

```powershell
Test-Path "C:\Program Files\Tesseract-OCR\tesseract.exe"
```

Configurer ensuite `.env` :

```dotenv
APP_NAME=Microservice Python - TP INF462
APP_DESCRIPTION=Microservice Python pour le traitement et l'analyse de documents
APP_VERSION=1.0.0
DEBUG=false
DATABASE_URL=sqlite:///./storage/app.db
TESSERACT_CMD=C:\Program Files\Tesseract-OCR\tesseract.exe
OCR_LANGUAGES=fra+eng
MAX_UPLOAD_SIZE_MB=10
```

Le chemin `TESSERACT_CMD` doit correspondre à l'installation réelle.

## Démarrage local

```powershell
uvicorn app.main:app --reload --port 8001
```

Interfaces disponibles :

- Swagger UI : `http://127.0.0.1:8001/docs`
- ReDoc : `http://127.0.0.1:8001/redoc`
- OpenAPI JSON : `http://127.0.0.1:8001/openapi.json`

## Démarrage Docker

Le conteneur installe Tesseract avec les langues française et anglaise.

```powershell
docker compose -f Docker-compose.yml up --build
```

Arrêt :

```powershell
docker compose -f Docker-compose.yml down
```

Les volumes conservent :

- la base SQLite dans `storage/app.db` ;
- les images dans `app/storage/uploads`.

Dans Docker, `TESSERACT_CMD` est automatiquement remplacé par
`/usr/bin/tesseract`.

## Endpoints

| Méthode | Endpoint | Description |
|---|---|---|
| `GET` | `/health` | Santé simplifiée (`status: UP`) |
| `POST` | `/analysis` | Analyse OCR structurée (format recommandé) |
| `GET` | `/api/v1/health/` | Vérifier l'état du service |
| `POST` | `/api/v1/analysis/` | Analyser une liste de nombres |
| `POST` | `/api/v1/ocr/extract` | Envoyer une image et effectuer l'OCR |
| `GET` | `/api/v1/ocr/history` | Lister les analyses OCR |
| `GET` | `/api/v1/ocr/history/{id}` | Consulter une analyse OCR |

## Tester l'OCR dans Swagger

1. Ouvrir `http://127.0.0.1:8001/docs`.
2. Déplier `POST /api/v1/ocr/extract`.
3. Cliquer sur **Try it out**.
4. Cliquer sur **Choose File** dans le champ `file`.
5. Sélectionner une image PNG, JPG ou JPEG.
6. Cliquer sur **Execute**.

Le champ doit être un sélecteur de fichier. Si Swagger affiche encore une
zone de texte, arrêter complètement Uvicorn, le relancer, puis actualiser la
page avec `Ctrl+F5` afin de vider l'ancien schéma OpenAPI du navigateur.

Exemple avec `curl` :

```bash
curl -X POST "http://127.0.0.1:8001/api/v1/ocr/extract" \
  -H "accept: application/json" \
  -F "file=@facture.png;type=image/png"
```

Format structuré recommandé :

```bash
curl -X POST "http://127.0.0.1:8001/analysis" \
  -H "accept: application/json" \
  -F "file=@cni.png;type=image/png"
```

## Exemples de réponses

Succès OCR :

```json
{
  "status": "success",
  "message": "OCR effectué avec succès",
  "data": {
    "id": 1,
    "original_filename": "facture.png",
    "extracted_text": "Numéro de facture : 2026-001",
    "confidence_score": 82.0,
    "status": "COMPLETED",
    "document_type": "IDENTITY_DOCUMENT",
    "reliability_level": "HIGH",
    "recommendation": "ACCEPT_FOR_REVIEW",
    "created_at": "2026-06-09T10:30:00"
  }
}
```

Erreur :

```json
{
  "status": "error",
  "message": "Extension de fichier non supportée",
  "errors": {
    "allowed_extensions": [".jpeg", ".jpg", ".png"]
  }
}
```

## Stockage et base de données

La table `document_analyses` contient :

- le nom original et le nom stocké du fichier ;
- le texte extrait ;
- le score de confiance ;
- le statut du traitement ;
- le type de document, les champs détectés et les champs manquants ;
- la recommandation (`ACCEPT_FOR_REVIEW`, `MANUAL_REVIEW_REQUIRED` ou
  `REQUEST_NEW_DOCUMENT`) ;
- la date de création.

La base et les tables sont créées automatiquement au démarrage :

```powershell
Test-Path .\storage\app.db
Get-ChildItem .\app\storage\uploads
```

Alembic n'est pas utilisé pour ce MVP.

Le score est borné entre 0 et 100 et repose sur des règles simples : quantité
de texte, mots-clés, chiffres, dates et champs détectés. Les seuils sont :

- 70–100 : `HIGH` / `ACCEPT_FOR_REVIEW` ;
- 40–69 : `MEDIUM` / `MANUAL_REVIEW_REQUIRED` ;
- 0–39 : `LOW` / `REQUEST_NEW_DOCUMENT`.

## Tests

```powershell
python -m pytest -q
python -m pip check
```

Les tests couvrent les routes principales, le repository, le service OCR,
les erreurs et le schéma OpenAPI du champ upload.

## Dépannage

### Swagger affiche une zone de texte

Vérifier que la route utilise un `UploadFile` obligatoire avec `File(...)`,
redémarrer Uvicorn et forcer l'actualisation de Swagger avec `Ctrl+F5`.

### Tesseract est introuvable

Vérifier :

```powershell
Test-Path $env:TESSERACT_CMD
```

Ou contrôler directement le chemin défini dans `.env`. L'API reste
disponible, mais l'endpoint OCR retourne `503` tant que Tesseract est absent.

### Le port 8001 est occupé

```powershell
Get-NetTCPConnection -LocalPort 8001
```

Arrêter l'ancien processus Uvicorn ou utiliser temporairement un autre port.
