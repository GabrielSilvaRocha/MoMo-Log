from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.database.dependencies import get_db
from app.models.exercise import Equipment, UserGymEquipment
from app.models.user import User
from app.schemas.exercise import UserGymEquipmentRead, UserGymEquipmentUpsert

router = APIRouter(prefix="/user-gym-equipment", tags=["user-gym-equipment"])


@router.get("", response_model=list[UserGymEquipmentRead])
def list_user_gym_equipment(
    user_id: int,
    db: Session = Depends(get_db),
) -> list[UserGymEquipment]:
    result = db.execute(
        select(UserGymEquipment)
        .where(UserGymEquipment.user_id == user_id)
        .order_by(UserGymEquipment.id)
    )
    return list(result.scalars().all())


@router.post("", response_model=UserGymEquipmentRead)
def upsert_user_gym_equipment(
    payload: UserGymEquipmentUpsert,
    db: Session = Depends(get_db),
) -> UserGymEquipment:
    if db.get(User, payload.user_id) is None:
        raise HTTPException(status_code=404, detail="User not found")

    if db.get(Equipment, payload.equipment_id) is None:
        raise HTTPException(status_code=404, detail="Equipment not found")

    item = db.execute(
        select(UserGymEquipment).where(
            UserGymEquipment.user_id == payload.user_id,
            UserGymEquipment.equipment_id == payload.equipment_id,
        )
    ).scalar_one_or_none()

    if item is None:
        item = UserGymEquipment(
            user_id=payload.user_id,
            equipment_id=payload.equipment_id,
            status=payload.status,
            notes=payload.notes,
        )
        db.add(item)
    else:
        item.status = payload.status
        item.notes = payload.notes

    db.commit()
    db.refresh(item)
    return item
