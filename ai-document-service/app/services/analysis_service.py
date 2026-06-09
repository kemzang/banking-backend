class AnalysisService:
    """Contains the business rules for numerical analysis."""

    def analyze_values(self, values: list[float]) -> dict[str, int | float]:
        if not values:
            return {
                "count": 0,
                "average": 0.0,
                "minimum": 0.0,
                "maximum": 0.0,
            }

        return {
            "count": len(values),
            "average": sum(values) / len(values),
            "minimum": min(values),
            "maximum": max(values),
        }


def analyze_values(values: list[float]) -> dict[str, int | float]:
    """Backward-compatible function for existing internal imports."""
    return AnalysisService().analyze_values(values)
