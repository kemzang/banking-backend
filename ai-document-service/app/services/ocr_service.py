import re
from pathlib import Path

import pytesseract
from fastapi import UploadFile
from starlette.concurrency import run_in_threadpool

from app.config import settings
from app.core.exceptions import AppException
from app.models.document_analysis import DocumentAnalysis
from app.repositories.document_repository import DocumentRepository
from app.services.image_preprocessing_service import ImagePreprocessingService
from app.utils.file_utils import delete_file, save_upload_file


class OcrService:
    """Coordinate file storage, image processing, OCR and persistence."""

    def __init__(
        self,
        repository: DocumentRepository,
        preprocessing_service: ImagePreprocessingService | None = None,
        upload_dir: Path | None = None,
    ) -> None:
        self.repository = repository
        self.preprocessing_service = (
            preprocessing_service or ImagePreprocessingService()
        )
        self.upload_dir = upload_dir or settings.upload_dir

        if settings.tesseract_cmd:
            pytesseract.pytesseract.tesseract_cmd = settings.tesseract_cmd

    async def extract(self, file: UploadFile) -> DocumentAnalysis:
        saved_path: Path | None = None

        try:
            original_filename, stored_filename, saved_path = (
                await save_upload_file(file, self.upload_dir)
            )
            processed_image = await run_in_threadpool(
                self.preprocessing_service.preprocess,
                saved_path,
            )
            extracted_text = await run_in_threadpool(
                pytesseract.image_to_string,
                processed_image,
                settings.ocr_languages,
                "--psm 6",
            )

            text = extracted_text.strip()
            result = self._analyze_text(text)

            return self.repository.create(
                original_filename=original_filename,
                stored_filename=stored_filename,
                extracted_text=text,
                confidence_score=result["score"],
                status="COMPLETED",
                content_type=file.content_type,
                document_type=result["document_type"],
                extracted_fields=result["fields"],
                missing_fields=result["missing_fields"],
                recommendation=result["recommendation"],
                message=result["message"],
            )
        except AppException:
            if saved_path:
                delete_file(saved_path)
            raise
        except pytesseract.TesseractNotFoundError as exc:
            if saved_path:
                delete_file(saved_path)
            raise AppException(
                message=(
                    "Tesseract est introuvable. Vérifiez TESSERACT_CMD "
                    "dans le fichier .env"
                ),
                status_code=503,
                errors={"tesseract_cmd": settings.tesseract_cmd or "tesseract"},
            ) from exc
        except pytesseract.TesseractError as exc:
            if saved_path:
                delete_file(saved_path)
            raise AppException(
                message="Une erreur est survenue pendant l'extraction OCR",
                status_code=500,
                errors={"detail": str(exc)},
            ) from exc
        except Exception as exc:
            if saved_path:
                delete_file(saved_path)
            raise AppException(
                message="Impossible de traiter le document",
                status_code=500,
                errors={"detail": str(exc)},
            ) from exc

    def get_history(self) -> list[DocumentAnalysis]:
        return self.repository.find_all()

    def get_history_item(self, analysis_id: int) -> DocumentAnalysis:
        analysis = self.repository.find_by_id(analysis_id)
        if analysis is None:
            raise AppException(
                message="Analyse OCR introuvable",
                status_code=404,
                errors={"id": analysis_id},
            )
        return analysis

    @staticmethod
    def _analyze_text(text: str) -> dict:
        normalized = " ".join(text.split())
        upper = normalized.upper()
        date_match = re.search(
            r"\b(?:\d{2}[/-]\d{2}[/-]\d{4}|\d{4}-\d{2}-\d{2})\b",
            normalized,
        )
        number_match = re.search(
            r"(?:NUM(?:E|É)RO|N[°O]|ID)\s*[:#-]?\s*([A-Z0-9-]{5,})",
            upper,
        )
        last_name_match = re.search(
            r"(?:NOM|SURNAME)\s*[:#-]?\s*([A-ZÀ-ÖØ-Ý'-]{2,})",
            upper,
        )
        first_name_match = re.search(
            r"(?:PR(?:E|É)NOM|GIVEN NAME)\s*[:#-]?\s*([A-ZÀ-ÖØ-Ý'-]{2,})",
            upper,
        )

        fields = {
            "firstName": first_name_match.group(1) if first_name_match else None,
            "lastName": last_name_match.group(1) if last_name_match else None,
            "birthDate": date_match.group(0) if date_match else None,
            "documentNumber": number_match.group(1) if number_match else None,
            "expirationDate": None,
        }
        missing = [name for name, value in fields.items() if value is None]
        keywords = sum(
            keyword in upper
            for keyword in ("NOM", "PRENOM", "DATE", "NUMERO", "IDENTITE", "PASSPORT")
        )
        detected = len(fields) - len(missing)
        score = min(len(normalized) // 2, 25)
        score += min(keywords * 5, 20)
        score += 10 if re.search(r"\d", normalized) else 0
        score += 10 if date_match else 0
        score += min(detected * 7, 28)
        score = min(100, score) if normalized else 0

        if "PASSPORT" in upper or "PASSEPORT" in upper:
            document_type = "IDENTITY_DOCUMENT"
        elif any(word in upper for word in ("IDENTITE", "IDENTITY", "CNI")):
            document_type = "IDENTITY_DOCUMENT"
        elif any(word in upper for word in ("FACTURE", "INVOICE", "ADRESSE")):
            document_type = "SUPPORTING_DOCUMENT"
        else:
            document_type = "UNKNOWN"

        if score >= 70:
            recommendation = "ACCEPT_FOR_REVIEW"
            message = "Document lisible avec un bon niveau de fiabilité."
        elif score >= 40:
            recommendation = "MANUAL_REVIEW_REQUIRED"
            message = "Une vérification humaine du document est recommandée."
        else:
            recommendation = "REQUEST_NEW_DOCUMENT"
            message = "Document peu lisible. Un nouveau document est recommandé."

        return {
            "score": score,
            "document_type": document_type,
            "fields": fields,
            "missing_fields": missing,
            "recommendation": recommendation,
            "message": message,
        }
