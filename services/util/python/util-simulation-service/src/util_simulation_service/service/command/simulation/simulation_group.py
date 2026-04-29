import click

from util_simulation_service.service.command.simulation.run_command import run


@click.group("simulation")
def simulation():
    """Manage and run Ubiquia simulations."""


simulation.add_command(run)
