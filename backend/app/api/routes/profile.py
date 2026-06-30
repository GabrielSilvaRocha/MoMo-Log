from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.database.dependencies import get_db
from app.models.user import User, UserPreference
from app.schemas.user import AuthenticatedUserRead, UserPreferenceRead, UserPreferenceUpdate, UserProfileUpdate, UserRead

router = APIRouter(prefix="/profile", tags=["profile"])


def _ensure_preferences(db: Session, user_id: int) -> UserPreference:
    preference = db.execute(select(UserPreference).where(UserPreference.user_id == user_id)).scalar_one_or_none()
    if preference is not None:
        return preference

    preference = UserPreference(
        user_id=user_id,
        default_running_source="manual_treadmill",
        preferred_training_days="seg,ter,qua,qui,sex",
        weekly_running_goal_km=20,
        weekly_strength_goal_sessions=3,
    )
    db.add(preference)
    db.commit()
    db.refresh(preference)
    return preference


@router.get("/{user_id}", response_model=AuthenticatedUserRead)
def get_profile(user_id: int, db: Session = Depends(get_db)) -> AuthenticatedUserRead:
    user = db.get(User, user_id)
    if user is None:
        raise HTTPException(status_code=404, detail="Usuário não encontrado")
    return AuthenticatedUserRead(user=user, preferences=_ensure_preferences(db, user.id))


@router.patch("/{user_id}", response_model=UserRead)
def update_profile(user_id: int, payload: UserProfileUpdate, db: Session = Depends(get_db)) -> User:
    user = db.get(User, user_id)
    if user is None:
        raise HTTPException(status_code=404, detail="Usuário não encontrado")

    updates = payload.model_dump(exclude_unset=True)
    for field, value in updates.items():
        setattr(user, field, value)

    db.commit()
    db.refresh(user)
    return user


@router.get("/{user_id}/preferences", response_model=UserPreferenceRead)
def get_preferences(user_id: int, db: Session = Depends(get_db)) -> UserPreference:
    user = db.get(User, user_id)
    if user is None:
        raise HTTPException(status_code=404, detail="Usuário não encontrado")
    return _ensure_preferences(db, user.id)


@router.patch("/{user_id}/preferences", response_model=UserPreferenceRead)
def update_preferences(user_id: int, payload: UserPreferenceUpdate, db: Session = Depends(get_db)) -> UserPreference:
    user = db.get(User, user_id)
    if user is None:
        raise HTTPException(status_code=404, detail="Usuário não encontrado")

    preference = _ensure_preferences(db, user.id)
    updates = payload.model_dump(exclude_unset=True)
    for field, value in updates.items():
        setattr(preference, field, value)

    db.commit()
    db.refresh(preference)
    return preference
