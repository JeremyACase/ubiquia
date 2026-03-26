from pydantic import BaseModel


class Network(BaseModel):
    name: str
    agents: list[str]
