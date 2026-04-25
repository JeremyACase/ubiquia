import click

from util_simulation_service.command.graph.visualize_command import visualize


@click.group("graph")
def graph():
    """Inspect and visualize Ubiquia DAG graphs."""


graph.add_command(visualize)
