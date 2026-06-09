from pydantic import BaseModel, ConfigDict, Field


class AnalysisRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    values: list[float] = Field(
        description="Valeurs numériques à analyser",
        examples=[[10, 15.5, 20]],
    )


class AnalysisResult(BaseModel):
    count: int
    average: float
    minimum: float
    maximum: float
