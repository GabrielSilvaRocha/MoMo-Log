from __future__ import annotations

import base64
import hashlib
import hmac
import json
import secrets
from datetime import datetime, timedelta, timezone
from typing import Any

from app.core.config import get_settings

_HASH_NAME = "sha256"
_ITERATIONS = 210_000


def _b64url_encode(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).decode("utf-8").rstrip("=")


def _b64url_decode(data: str) -> bytes:
    padding = "=" * (-len(data) % 4)
    return base64.urlsafe_b64decode(data + padding)


def hash_password(password: str, salt: str | None = None) -> str:
    """Hash de senha com PBKDF2 usando apenas stdlib."""
    if not password:
        raise ValueError("password cannot be empty")

    salt = salt or secrets.token_urlsafe(16)
    digest = hashlib.pbkdf2_hmac(
        _HASH_NAME,
        password.encode("utf-8"),
        salt.encode("utf-8"),
        _ITERATIONS,
    )
    return f"pbkdf2_{_HASH_NAME}${_ITERATIONS}${salt}${digest.hex()}"


def verify_password(password: str, password_hash: str | None) -> bool:
    if not password_hash:
        return False

    try:
        algorithm, iterations, salt, expected_hash = password_hash.split("$", 3)
    except ValueError:
        return False

    if algorithm != f"pbkdf2_{_HASH_NAME}":
        return False

    digest = hashlib.pbkdf2_hmac(
        _HASH_NAME,
        password.encode("utf-8"),
        salt.encode("utf-8"),
        int(iterations),
    ).hex()
    return hmac.compare_digest(digest, expected_hash)


def create_access_token(subject: str, expires_delta: timedelta | None = None) -> str:
    settings = get_settings()
    now = datetime.now(timezone.utc)
    expires_at = now + (expires_delta or timedelta(minutes=settings.access_token_expire_minutes))

    header = {"alg": "HS256", "typ": "JWT"}
    payload = {
        "sub": subject,
        "iat": int(now.timestamp()),
        "exp": int(expires_at.timestamp()),
    }

    encoded_header = _b64url_encode(json.dumps(header, separators=(",", ":")).encode("utf-8"))
    encoded_payload = _b64url_encode(json.dumps(payload, separators=(",", ":")).encode("utf-8"))
    signing_input = f"{encoded_header}.{encoded_payload}".encode("utf-8")
    signature = hmac.new(settings.secret_key.encode("utf-8"), signing_input, hashlib.sha256).digest()
    return f"{encoded_header}.{encoded_payload}.{_b64url_encode(signature)}"


def decode_access_token(token: str) -> dict[str, Any] | None:
    settings = get_settings()
    try:
        encoded_header, encoded_payload, encoded_signature = token.split(".")
    except ValueError:
        return None

    signing_input = f"{encoded_header}.{encoded_payload}".encode("utf-8")
    expected_signature = hmac.new(settings.secret_key.encode("utf-8"), signing_input, hashlib.sha256).digest()

    try:
        signature = _b64url_decode(encoded_signature)
    except Exception:
        return None

    if not hmac.compare_digest(signature, expected_signature):
        return None

    try:
        payload = json.loads(_b64url_decode(encoded_payload))
    except Exception:
        return None

    exp = payload.get("exp")
    if not isinstance(exp, int) or datetime.now(timezone.utc).timestamp() > exp:
        return None

    return payload
