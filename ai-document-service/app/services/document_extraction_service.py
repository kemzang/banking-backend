import json
import re


class DocumentExtractionService:
    """Classify documents and extract structured fields from OCR text."""

    DOCUMENT_TYPES = ("salaire", "releve_bancaire", "cni", "inconnu")

    def classify(self, text: str) -> str:
        lower = text.lower()
        salaire_keywords = ["salaire", "bulletin de paie", "paie", "salaire net", "employeur"]
        releve_keywords = ["releve de compte", "releve bancaire", "solde", "transaction", "credit", "debit"]
        cni_keywords = ["carte d'identite", "cni", "republique", "nationalite", "lieu de naissance", "delivre le"]

        scores = {
            "salaire": sum(1 for kw in salaire_keywords if kw in lower),
            "releve_bancaire": sum(1 for kw in releve_keywords if kw in lower),
            "cni": sum(1 for kw in cni_keywords if kw in lower),
        }
        best = max(scores, key=scores.get)
        return best if scores[best] > 0 else "inconnu"

    def extract_fields(self, text: str, document_type: str) -> dict:
        extractors = {
            "salaire": self._extract_salaire,
            "releve_bancaire": self._extract_releve,
            "cni": self._extract_cni,
        }
        extractor = extractors.get(document_type)
        if extractor:
            return extractor(text)
        return {}

    def _extract_salaire(self, text: str) -> dict:
        data: dict = {}
        patterns = {
            "salaireMensuel": [
                r"[Ss]alaire\s*(?:net|brut)?\s*:?\s*([\d\s.,]+)\s*(?:FCFA|XAF|francs)?",
                r"(?:net|brut)\s*(?:a\s*payer)?\s*:?\s*([\d\s.,]+)\s*(?:FCFA|XAF)?",
                r"(\d[\d\s.,]+)\s*(?:FCFA|XAF)\s*(?:net|brut)",
            ],
            "nomEmployeur": [
                r"[Ee]mployeur\s*:?\s*(.+?)(?:\n|$)",
                r"(?:Societe|Société)\s*:?\s*(.+?)(?:\n|$)",
            ],
            "nomEmploye": [
                r"[Nn]om\s*(?:et\s*[Pp]renom)?\s*:?\s*(.+?)(?:\n|$)",
                r"[Ee]mploye\s*:?\s*(.+?)(?:\n|$)",
            ],
            "periode": [
                r"(?:mois|periode|period)\s*:?\s*(.+?)(?:\n|$)",
                r"(\w+\s*\d{4})",
            ],
        }
        for field, pats in patterns.items():
            for pat in pats:
                match = re.search(pat, text)
                if match:
                    value = match.group(1).strip()
                    if field == "salaireMensuel":
                        cleaned = re.sub(r"[\s]", "", value)
                        cleaned = cleaned.replace(",", ".")
                        try:
                            data[field] = float(cleaned)
                        except ValueError:
                            data[field] = value
                    else:
                        data[field] = value
                    break
        return data

    def _extract_releve(self, text: str) -> dict:
        data: dict = {}
        patterns = {
            "soldeMoyen": [
                r"[Ss]olde\s*(?:moyen|debiteur|crediteur)?\s*:?\s*([\d\s.,]+)\s*(?:FCFA|XAF)?",
                r"(\d[\d\s.,]+)\s*(?:FCFA|XAF)",
            ],
            "fluxMensuel": [
                r"(?:total|sum|montant)\s*(?:operations|transactions)?\s*:?\s*([\d\s.,]+)",
            ],
            "numeroCompte": [
                r"(?:compte|account)\s*(?:n°|num|numero|#)?\s*:?\s*(\d[\d\s]+\d)",
                r"(\d{5,})",
            ],
        }
        for field, pats in patterns.items():
            for pat in pats:
                match = re.search(pat, text)
                if match:
                    value = match.group(1).strip()
                    if field in ("soldeMoyen", "fluxMensuel"):
                        cleaned = re.sub(r"[\s]", "", value)
                        cleaned = cleaned.replace(",", ".")
                        try:
                            data[field] = float(cleaned)
                        except ValueError:
                            data[field] = value
                    else:
                        data[field] = value
                    break
        return data

    def _extract_cni(self, text: str) -> dict:
        data: dict = {}
        patterns = {
            "nom": [
                r"[Nn]om\s*:?\s*(.+?)(?:\n|$)",
            ],
            "prenom": [
                r"[Pp]renoms?\s*:?\s*(.+?)(?:\n|$)",
            ],
            "numeroPiece": [
                r"(?:n°|numero|#)\s*:?\s*(\w[\w\s-]+\w)",
                r"(\d{6,})",
            ],
            "dateNaissance": [
                r"[Nn]e\s*(?:le)?\s*:?\s*(\d{1,2}[\s/\.]\d{1,2}[\s/\.]\d{2,4})",
                r"[Dd]ate\s*(?:de\s*)?naissance\s*:?\s*(\d{1,2}[\s/\.]\d{1,2}[\s/\.]\d{2,4})",
            ],
            "lieuNaissance": [
                r"[Ll]ieu\s*(?:de\s*)?naissance\s*:?\s*(.+?)(?:\n|$)",
            ],
        }
        for field, pats in patterns.items():
            for pat in pats:
                match = re.search(pat, text)
                if match:
                    data[field] = match.group(1).strip()
                    break
        return data

    def process(self, text: str) -> tuple[str, dict]:
        doc_type = self.classify(text)
        fields = self.extract_fields(text, doc_type)
        return doc_type, fields
