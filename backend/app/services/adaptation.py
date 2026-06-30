from __future__ import annotations

from dataclasses import dataclass

from sqlalchemy import select
from sqlalchemy.orm import Session, joinedload

from app.models.exercise import ExerciseAlternative, ExerciseEquipment, UserGymEquipment
from app.schemas.exercise import ExerciseRead


@dataclass(frozen=True)
class UserEquipmentContext:
    unavailable: set[int]
    frequently_busy: set[int]
    favorite: set[int]


def _get_user_equipment_context(db: Session, user_id: int | None) -> UserEquipmentContext:
    if user_id is None:
        return UserEquipmentContext(unavailable=set(), frequently_busy=set(), favorite=set())

    rows = db.execute(select(UserGymEquipment).where(UserGymEquipment.user_id == user_id)).scalars().all()
    return UserEquipmentContext(
        unavailable={row.equipment_id for row in rows if row.status == "unavailable"},
        frequently_busy={row.equipment_id for row in rows if row.status == "frequently_busy"},
        favorite={row.equipment_id for row in rows if row.status == "favorite"},
    )


def _label_for_score(score: int, blocked: bool) -> str:
    if blocked:
        return "Fora do padrão da academia"
    if score >= 92:
        return "Substituição excelente"
    if score >= 85:
        return "Alta compatibilidade"
    if score >= 75:
        return "Boa alternativa"
    return "Alternativa manual"


def build_adaptation_suggestions(
    db: Session,
    exercise_id: int,
    user_id: int | None = None,
    mode: str = "default",
    reason: str = "equipment_busy",
) -> list[dict]:
    """Return ranked alternatives for the Adaptation Engine.

    mode=default hides unavailable equipment from primary suggestions.
    mode=all keeps unavailable equipment visible with penalties and warnings.
    """

    alternatives = db.execute(
        select(ExerciseAlternative)
        .options(joinedload(ExerciseAlternative.alternative_exercise))
        .where(ExerciseAlternative.exercise_id == exercise_id)
    ).scalars().all()

    context = _get_user_equipment_context(db, user_id)
    suggestions: list[dict] = []

    for alternative in alternatives:
        equipment_rows = db.execute(
            select(ExerciseEquipment)
            .options(joinedload(ExerciseEquipment.equipment))
            .where(
                ExerciseEquipment.exercise_id == alternative.alternative_exercise_id,
                ExerciseEquipment.is_required.is_(True),
            )
        ).scalars().all()

        required_equipment_ids = {row.equipment_id for row in equipment_rows}
        equipment_names = [row.equipment.name for row in equipment_rows if row.equipment is not None]

        blocked = bool(required_equipment_ids & context.unavailable)
        busy = bool(required_equipment_ids & context.frequently_busy)
        favorite = bool(required_equipment_ids & context.favorite)

        penalties: list[str] = []
        bonuses: list[str] = []
        badges: list[str] = []
        score = int(alternative.equivalence_score)
        equipment_status: str | None = None
        is_default_suggestion = True

        if blocked:
            score -= 35
            equipment_status = "unavailable"
            is_default_suggestion = False
            penalties.append("Equipamento marcado como inexistente na academia")
            badges.append("indisponível")
        elif busy:
            penalty = 16 if reason == "equipment_busy" else 10
            score -= penalty
            equipment_status = "frequently_busy"
            penalties.append("Equipamento frequentemente ocupado")
            badges.append("sempre ocupado")
        elif favorite:
            score += 7
            equipment_status = "favorite"
            bonuses.append("Equipamento favorito do usuário")
            badges.append("favorito")

        if reason == "pain_discomfort":
            score -= 5
            penalties.append("Troca por desconforto exige validação manual")
        elif reason == "preference":
            score += 3
            bonuses.append("Preferência manual do usuário")

        score = max(0, min(100, score))

        if mode == "default" and not is_default_suggestion:
            continue

        suggestions.append(
            {
                "id": alternative.id,
                "exercise_id": alternative.exercise_id,
                "alternative_exercise_id": alternative.alternative_exercise_id,
                "alternative_exercise": ExerciseRead.model_validate(alternative.alternative_exercise).model_dump(mode="json"),
                "equivalence_score": alternative.equivalence_score,
                "recommendation_score": score,
                "recommendation_label": _label_for_score(score, blocked),
                "reason": alternative.reason,
                "equipment_status": equipment_status,
                "is_default_suggestion": is_default_suggestion,
                "badges": badges,
                "equipment_names": equipment_names,
                "penalties": penalties,
                "bonuses": bonuses,
            }
        )

    suggestions.sort(key=lambda item: (item["is_default_suggestion"], item["recommendation_score"], item["equivalence_score"]), reverse=True)
    return suggestions
