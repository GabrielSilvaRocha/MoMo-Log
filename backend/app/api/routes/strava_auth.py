from datetime import datetime, timezone

from fastapi import APIRouter, Depends, HTTPException, Query
from fastapi.responses import RedirectResponse
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.database.dependencies import get_db
from app.integrations.strava.client import StravaClient, parse_user_id_from_state
from app.models.running import StravaAccount
from app.models.user import User

router = APIRouter(tags=["strava-auth"])


@router.get("/auth/strava/authorize")
def authorize_strava(user_id: int, db: Session = Depends(get_db)) -> dict[str, str | bool]:
    if db.get(User, user_id) is None:
        raise HTTPException(status_code=404, detail="User not found")

    settings = get_settings()
    client = StravaClient(settings)
    authorization_url = client.build_authorization_url(user_id)
    return {
        "authorization_url": authorization_url,
        "configured": client.is_configured,
        "scope": settings.strava_scope,
        "redirect_uri": settings.strava_redirect_uri,
    }


@router.get("/auth/strava/callback")
async def strava_callback(
    code: str | None = None,
    scope: str | None = None,
    state: str | None = None,
    error: str | None = None,
    user_id: int | None = Query(default=None),
    db: Session = Depends(get_db),
):
    settings = get_settings()
    frontend_error_url = f"{settings.frontend_origin}?view=running&strava=error"

    if error:
        return RedirectResponse(f"{frontend_error_url}&message={error}")
    if not code:
        return RedirectResponse(f"{frontend_error_url}&message=missing_code")

    resolved_user_id = user_id if user_id is not None else parse_user_id_from_state(state)
    user = db.get(User, resolved_user_id)
    if user is None:
        return RedirectResponse(f"{frontend_error_url}&message=user_not_found")

    client = StravaClient(settings)
    token_data = await client.exchange_code(code)
    athlete = token_data.get("athlete") or {}
    athlete_id = str(athlete.get("id") or "")
    if not athlete_id:
        return RedirectResponse(f"{frontend_error_url}&message=missing_athlete")

    account = db.execute(select(StravaAccount).where(StravaAccount.user_id == resolved_user_id)).scalar_one_or_none()
    expires_at_raw = token_data.get("expires_at")
    token_expires_at = (
        datetime.fromtimestamp(int(expires_at_raw), tz=timezone.utc) if expires_at_raw is not None else None
    )

    if account is None:
        account = StravaAccount(user_id=resolved_user_id, strava_athlete_id=athlete_id)
        db.add(account)

    account.strava_athlete_id = athlete_id
    account.access_token_encrypted = token_data.get("access_token")
    account.refresh_token_encrypted = token_data.get("refresh_token")
    account.token_expires_at = token_expires_at

    db.commit()
    return RedirectResponse(f"{settings.frontend_origin}?view=running&strava=connected&scope={scope or ''}")
