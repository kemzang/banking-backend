from collections.abc import Generator
from pathlib import Path

from sqlalchemy import create_engine
from sqlalchemy.orm import DeclarativeBase, Session, sessionmaker

from app.config import settings


class Base(DeclarativeBase):
    pass


connect_args = (
    {"check_same_thread": False}
    if settings.database_url.startswith("sqlite")
    else {}
)

engine = create_engine(settings.database_url, connect_args=connect_args)
SessionLocal = sessionmaker(bind=engine, autoflush=False, autocommit=False)


def get_db() -> Generator[Session, None, None]:
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


def init_db() -> None:
    if settings.database_url.startswith("sqlite:///"):
        database_path = settings.database_url.removeprefix("sqlite:///")
        Path(database_path).parent.mkdir(parents=True, exist_ok=True)

    # Models must be imported before SQLAlchemy creates the metadata.
    from app.models.document_analysis import DocumentAnalysis  # noqa: F401

    Base.metadata.create_all(bind=engine)
