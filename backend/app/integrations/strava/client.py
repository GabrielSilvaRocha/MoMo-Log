from __future__ import annotations

from datetime import datetime, timedelta, timezone
from typing import Any
from urllib.parse import urlencode

import httpx
from fastapi import HTTPException

from app.core.config import Settings
from app.models.running import StravaAccount


class StravaClient:
    def __init__(self, settings: Settings):
        self.settings = settings

    @property
    def is_configured(self) -> bool:
        return bool(self.settings.strava_client_id and self.settings.strava_client_secret)

    def build_authorization_url(self, user_id: int) -> str:
        if not self.settings.strava_client_id:
            raise HTTPException(status_code=400, detail="STRAVA_CLIENT_ID is not configured")

        params = {
            "client_id": self.settings.strava_client_id,
            "redirect_uri": self.settings.strava_redirect_uri,
            "response_type": "code",
            "approval_prompt": "auto",
            "scope": self.settings.strava_scope,
            "state": f"user:{user_id}",
        }
        return f"{self.settings.strava_oauth_base_url}/authorize?{urlencode(params)}"

    async def exchange_code(self, code: str) -> dict[str, Any]:
        if not self.is_configured:
            raise HTTPException(status_code=400, detail="Strava OAuth is not configured")

        payload = {
            "client_id": self.settings.strava_client_id,
            "client_secret": self.settings.strava_client_secret,
            "code": code,
            "grant_type": "authorization_code",
        }
        async with httpx.AsyncClient(timeout=20) as client:
            response = await client.post(f"{self.settings.strava_oauth_base_url}/token", data=payload)
        if response.status_code >= 400:
            raise HTTPException(status_code=400, detail=f"Strava token exchange failed: {response.text}")
        return response.json()

    async def refresh_access_token(self, refresh_token: str) -> dict[str, Any]:
        if not self.is_configured:
            raise HTTPException(status_code=400, detail="Strava OAuth is not configured")

        payload = {
            "client_id": self.settings.strava_client_id,
            "client_secret": self.settings.strava_client_secret,
            "grant_type": "refresh_token",
            "refresh_token": refresh_token,
        }
        async with httpx.AsyncClient(timeout=20) as client:
            response = await client.post(f"{self.settings.strava_oauth_base_url}/token", data=payload)
        if response.status_code >= 400:
            raise HTTPException(status_code=400, detail=f"Strava token refresh failed: {response.text}")
        return response.json()

    async def get_athlete_activities(self, access_token: str, per_page: int = 30) -> list[dict[str, Any]]:
        headers = {"Authorization": f"Bearer {access_token}"}
        params = {"page": 1, "per_page": per_page}
        async with httpx.AsyncClient(timeout=30) as client:
            response = await client.get(
                f"{self.settings.strava_api_base_url}/athlete/activities",
                headers=headers,
                params=params,
            )
        if response.status_code >= 400:
            raise HTTPException(status_code=response.status_code, detail=f"Strava activities request failed: {response.text}")
        return response.json()


def parse_user_id_from_state(state: str | None) -> int:
    if not state or not state.startswith("user:"):
        raise HTTPException(status_code=400, detail="Invalid Strava OAuth state")
    try:
        return int(state.split(":", 1)[1])
    except ValueError as exc:
        raise HTTPException(status_code=400, detail="Invalid Strava OAuth user id") from exc


def should_refresh_token(account: StravaAccount) -> bool:
    if account.token_expires_at is None:
        return False
    expires_at = account.token_expires_at
    if expires_at.tzinfo is None:
        expires_at = expires_at.replace(tzinfo=timezone.utc)
    return expires_at <= datetime.now(timezone.utc) + timedelta(minutes=10)
