from collections.abc import Generator
from pathlib import Path

from sqlalchemy import create_engine, inspect, text
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

    # create_all ne modifie pas les tables SQLite existantes. Ces colonnes
    # nullable assurent une migration idempotente de l'historique du TP.
    if settings.database_url.startswith("sqlite"):
        columns = {
            column["name"]
            for column in inspect(engine).get_columns("document_analyses")
        }
        additions = {
            "content_type": "VARCHAR(100)",
            "document_type": "VARCHAR(50)",
            "extracted_fields_json": "TEXT",
            "missing_fields_json": "TEXT",
            "recommendation": "VARCHAR(50)",
            "message": "TEXT",
        }
        with engine.begin() as connection:
            for name, sql_type in additions.items():
                if name not in columns:
                    connection.execute(
                        text(
                            f"ALTER TABLE document_analyses "
                            f"ADD COLUMN {name} {sql_type}"
                        )
                    )
