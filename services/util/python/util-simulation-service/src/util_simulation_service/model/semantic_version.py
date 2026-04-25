from pydantic import BaseModel


class SemanticVersion(BaseModel):
    major: int
    minor: int
    patch: int
