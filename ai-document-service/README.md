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

## Tests

```powershell
python -m pytest
```
