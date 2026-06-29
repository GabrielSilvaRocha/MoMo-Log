from fastapi import APIRouter

router = APIRouter(tags=["Root"])


@router.get("/")
def root():
    return {"message": "Mo² LOG API"}
