import logging
import time

import click

from util_simulation_service.command.simulate_command import simulate

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)-8s] %(name)s: %(message)s",
    datefmt="%Y-%m-%dT%H:%M:%SZ",
)
logging.Formatter.converter = time.gmtime


@click.group()
def cli():
    """Ubiquia simulation utility for exercising multi-agent flow deployments."""


cli.add_command(simulate)


if __name__ == "__main__":
    cli()
