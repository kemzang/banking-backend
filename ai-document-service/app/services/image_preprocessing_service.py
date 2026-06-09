from pathlib import Path
from typing import Any

import cv2

from app.core.exceptions import AppException


class ImagePreprocessingService:
    """Prepare an uploaded image for text extraction."""

    def preprocess(self, image_path: Path) -> Any:
        image = cv2.imread(str(image_path))
        if image is None:
            raise AppException(
                message="Le fichier ne contient pas une image valide",
                status_code=400,
            )

        grayscale = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
        denoised = cv2.GaussianBlur(grayscale, (3, 3), 0)
        _, thresholded = cv2.threshold(
            denoised,
            0,
            255,
            cv2.THRESH_BINARY + cv2.THRESH_OTSU,
        )
        return thresholded
