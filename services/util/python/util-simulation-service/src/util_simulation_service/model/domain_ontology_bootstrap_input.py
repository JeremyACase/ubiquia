import pathlib

from pydantic import BaseModel


class DomainOntologyBootstrapInput(BaseModel):
    file: pathlib.Path
    targets: list[str]
