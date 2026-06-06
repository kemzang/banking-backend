from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.config import settings
from app.core.database import init_db
from app.core.exceptions import register_exception_handlers
from app.routes.analysis import router as analysis_router
from app.routes.health import router as health_router


@asynccontextmanager
async def lifespan(app: FastAPI):
    init_db()
    yield


app = FastAPI(
    title=settings.app_name,
    description=settings.app_description,
    version=settings.app_version,
    debug=settings.debug,
    lifespan=lifespan,
)

app.include_router(health_router, prefix="/api/v1/health", tags=["Health"])
app.include_router(analysis_router, prefix="/api/v1/analysis", tags=["Analysis"])

register_exception_handlers(app)
