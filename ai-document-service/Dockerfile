# Service IA / OCR (Python + FastAPI)
FROM python:3.11-slim

# Dependances systeme pour Tesseract OCR et OpenCV
RUN apt-get update && apt-get install -y --no-install-recommends \
    tesseract-ocr \
    libtesseract-dev \
    libgl1 \
    libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Code source (main.py, app/...) — a ajouter
COPY . .

EXPOSE 8000
# Adapter "main:app" au point d'entree reel de votre application FastAPI
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
