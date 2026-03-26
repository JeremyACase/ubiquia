import logging

import click
from rich.logging import RichHandler

from util_simulation_service.command.simulation.simulation_group import simulation

logging.basicConfig(
    level=logging.INFO,
    format="%(message)s",
    datefmt="[%X]",
    handlers=[RichHandler(rich_tracebacks=True)],
)


@click.group()
def cli():
    """Ubiquia simulation utility for exercising multi-agent flow deployments."""


cli.add_command(simulation)


if __name__ == "__main__":
    cli()
