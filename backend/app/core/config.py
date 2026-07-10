from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    app_name: str = "Mo² LOG"
    app_env: str = "development"
    app_version: str = "10.1.0"
    database_url: str
    frontend_origin: str = "http://localhost:5173"
    secret_key: str = "development-only-not-a-secret"
    access_token_expire_minutes: int = 60 * 24


    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )


@lru_cache
def get_settings() -> Settings:
    return Settings()
