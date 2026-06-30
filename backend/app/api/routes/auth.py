from datetime import timedelta

from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.core.security import create_access_token, decode_access_token, hash_password, verify_password
from app.database.dependencies import get_db
from app.models.user import User, UserPreference
from app.schemas.user import AuthenticatedUserRead, AuthTokenRead, UserCreate, UserLogin

router = APIRouter(prefix="/auth", tags=["auth"])
security = HTTPBearer(auto_error=False)


def _get_user_by_email(db: Session, email: str) -> User | None:
    return db.execute(select(User).where(User.email == email.lower())).scalar_one_or_none()


def _ensure_preferences(db: Session, user_id: int) -> UserPreference:
    preference = db.execute(select(UserPreference).where(UserPreference.user_id == user_id)).scalar_one_or_none()
    if preference:
        return preference

    preference = UserPreference(
        user_id=user_id,
        default_running_source="manual_treadmill",
        preferred_training_days="seg,ter,qua,qui,sex",
        weekly_running_goal_km=20,
        weekly_strength_goal_sessions=3,
        gym_notes="",
    )
    db.add(preference)
    db.commit()
    db.refresh(preference)
    return preference


def get_current_user(
    credentials: HTTPAuthorizationCredentials | None = Depends(security),
    db: Session = Depends(get_db),
) -> User:
    if credentials is None:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Token não informado")

    payload = decode_access_token(credentials.credentials)
    if payload is None or "sub" not in payload:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Token inválido ou expirado")

    try:
        user_id = int(payload["sub"])
    except (TypeError, ValueError):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Token inválido") from None

    user = db.get(User, user_id)
    if user is None:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Usuário não encontrado")
    return user


def _token_for_user(user: User) -> AuthTokenRead:
    settings = get_settings()
    token = create_access_token(subject=str(user.id), expires_delta=timedelta(minutes=settings.access_token_expire_minutes))
    return AuthTokenRead(access_token=token, user=user)


@router.post("/register", response_model=AuthTokenRead, status_code=status.HTTP_201_CREATED)
def register_user(payload: UserCreate, db: Session = Depends(get_db)) -> AuthTokenRead:
    email = payload.email.lower()
    existing_user = _get_user_by_email(db, email)
    if existing_user is not None:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="E-mail já cadastrado")

    user = User(
        name=payload.name,
        email=email,
        password_hash=hash_password(payload.password),
        avatar_url=payload.avatar_url,
    )
    db.add(user)
    db.commit()
    db.refresh(user)
    _ensure_preferences(db, user.id)
    return _token_for_user(user)


@router.post("/login", response_model=AuthTokenRead)
def login_user(payload: UserLogin, db: Session = Depends(get_db)) -> AuthTokenRead:
    user = _get_user_by_email(db, payload.email)
    if user is None or not verify_password(payload.password, user.password_hash):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="E-mail ou senha inválidos")

    _ensure_preferences(db, user.id)
    return _token_for_user(user)


@router.post("/demo-login", response_model=AuthTokenRead)
def demo_login(db: Session = Depends(get_db)) -> AuthTokenRead:
    user = _get_user_by_email(db, "gabriel.demo@mo2log.com.br")
    if user is None:
        user = User(
            id=1,
            name="Gabriel Rocha",
            email="gabriel.demo@mo2log.com.br",
            password_hash=None,
        )
        db.add(user)
        db.commit()
        db.refresh(user)

    if user.password_hash is None:
        # Demo user does not receive a repository-defined password
        db.commit()
        db.refresh(user)

    _ensure_preferences(db, user.id)
    return _token_for_user(user)


@router.get("/me", response_model=AuthenticatedUserRead)
def read_current_user(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> AuthenticatedUserRead:
    preference = _ensure_preferences(db, current_user.id)
    return AuthenticatedUserRead(user=current_user, preferences=preference)
