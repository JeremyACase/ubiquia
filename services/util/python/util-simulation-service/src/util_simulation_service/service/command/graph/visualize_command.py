import pathlib

import click

from util_simulation_service.service.dag_visualization_service import DagVisualizationService


@click.command("visualize")
@click.option(
    "--file",
    "ontology_file",
    type=click.Path(exists=True, dir_okay=False, path_type=pathlib.Path),
    required=True,
    help="Path to the domain ontology YAML file.",
)
def visualize(ontology_file: pathlib.Path):
    """Render an ASCII visualization of every DAG in a domain ontology YAML."""
    output = DagVisualizationService().visualize_all(ontology_file)
    click.echo(output)
