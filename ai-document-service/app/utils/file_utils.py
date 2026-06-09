from pathlib import Path
from uuid import uuid4

from fastapi import UploadFile

from app.core.exceptions import AppException


ALLOWED_IMAGE_EXTENSIONS = {".png", ".jpg", ".jpeg"}


async def save_upload_file(
    file: UploadFile | None,
    upload_dir: Path,
) -> tuple[str, str, Path]:
    if file is None or not file.filename:
        raise AppException(
            message="Aucun fichier image n'a été fourni",
            status_code=400,
        )

    original_filename = Path(file.filename).name
    extension = Path(original_filename).suffix.lower()
    if extension not in ALLOWED_IMAGE_EXTENSIONS:
        raise AppException(
            message="Extension de fichier non supportée",
            status_code=400,
            errors={"allowed_extensions": sorted(ALLOWED_IMAGE_EXTENSIONS)},
        )

    upload_dir.mkdir(parents=True, exist_ok=True)
    stored_filename = f"{uuid4().hex}{extension}"
    destination = upload_dir / stored_filename

    try:
        file_size = 0
        with destination.open("wb") as output:
            while chunk := await file.read(1024 * 1024):
                file_size += len(chunk)
                output.write(chunk)
    except Exception:
        delete_file(destination)
        raise
    finally:
        await file.close()

    if file_size == 0:
        delete_file(destination)
        raise AppException(
            message="Le fichier image est vide",
            status_code=400,
        )

    return original_filename, stored_filename, destination


def delete_file(file_path: Path) -> None:
    file_path.unlink(missing_ok=True)
