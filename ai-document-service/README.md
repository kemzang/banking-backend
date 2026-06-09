# AI Document Service

Microservice FastAPI pour l'analyse de données, préparé pour intégrer un
workflow OCR et une persistance SQLite.

## Installation

```powershell
python -m venv venv
.\venv\Scripts\Activate.ps1
pip install -r requirements.txt
Copy-Item .env.example .env
```

## Démarrage

```powershell
uvicorn app.main:app --reload --port 8001
```

Documentation interactive : `http://127.0.0.1:8001/docs`

## Base de données

La base SQLite est configurée dans `.env` :

```dotenv
DATABASE_URL=sqlite:///./storage/app.db
```

Le dossier `storage` et la table `document_analyses` sont créés
automatiquement au démarrage de l'application.

Pour vérifier la création du fichier :

```powershell
Test-Path .\storage\app.db
```

## OCR

Sous Windows, installer Tesseract puis configurer son exécutable dans `.env` :

```dotenv
TESSERACT_CMD=C:\Program Files\Tesseract-OCR\tesseract.exe
```

Les images envoyées sont enregistrées dans `app/storage/uploads`.

Depuis Swagger (`http://127.0.0.1:8001/docs`) :

1. Ouvrir `POST /api/v1/ocr/extract`.
2. Cliquer sur **Try it out**.
3. Sélectionner une image PNG, JPG ou JPEG.
4. Cliquer sur **Execute**.
5. Consulter ensuite `GET /api/v1/ocr/history`.

## Tests

```powershell
python -m pytest
```
