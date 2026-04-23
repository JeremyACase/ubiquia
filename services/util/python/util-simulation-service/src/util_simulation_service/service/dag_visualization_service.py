import pathlib
from collections import defaultdict

import yaml

_CONN = "    "  # spaces from indent to connector/arrow column


class DagVisualizationService:
    """Renders an ASCII visualization of a named DAG from a domain ontology YAML."""

    def visualize_all(self, ontology_path: pathlib.Path) -> str:
        data = yaml.safe_load(ontology_path.read_text())
        graphs = data.get("graphs", [])
        return "\n\n".join(
            self.visualize(ontology_path, g["name"], _data=data) for g in graphs
        )

    def visualize(self, ontology_path: pathlib.Path, graph_name: str, *, _data: dict | None = None) -> str:
        data = _data if _data is not None else yaml.safe_load(ontology_path.read_text())
        graphs = data.get("graphs", [])
        graph = next((g for g in graphs if g["name"] == graph_name), None)
        if graph is None:
            available = [g["name"] for g in graphs]
            raise ValueError(
                f"Graph '{graph_name}' not found in {ontology_path.name}. "
                f"Available: {available}"
            )

        nodes = {n["name"]: n for n in graph.get("nodes", [])}

        downstream: dict[str, list[str]] = defaultdict(list)
        has_upstream: set[str] = set()
        for edge in graph.get("edges", []):
            for right in edge.get("rightNodeNames", []):
                downstream[edge["leftNodeName"]].append(right)
                has_upstream.add(right)

        roots = [name for name in nodes if name not in has_upstream]
        visited: set[str] = set()

        lines = [graph_name, "=" * len(graph_name)]
        for root in roots:
            lines.append("")
            self._render(root, nodes, downstream, visited, lines, indent="")

        return "\n".join(lines)

    # ------------------------------------------------------------------
    # Internal helpers
    # ------------------------------------------------------------------

    def _make_box(self, node: dict, name: str) -> list[str]:
        """Return box lines (no leading indent)."""
        ntype = f"[{node.get('nodeType', '?')}]"
        endpoint = node.get("endpoint", "")
        content = [f"{ntype} {name}"]
        if endpoint:
            content.append(endpoint)
        width = max(len(l) for l in content)
        border = "+" + "-" * (width + 2) + "+"
        box = [border]
        for l in content:
            box.append("| " + l.ljust(width) + " |")
        box.append(border)
        return box

    def _render(
        self,
        name: str,
        nodes: dict,
        downstream: dict,
        visited: set,
        lines: list,
        indent: str,
    ) -> None:
        if name in visited:
            return
        visited.add(name)

        node = nodes.get(name, {})
        for line in self._make_box(node, name):
            lines.append(indent + line)

        children = downstream.get(name, [])
        if not children:
            return

        if len(children) == 1:
            lines.append(indent + _CONN + "|")
            lines.append(indent + _CONN + "v")
            self._render(children[0], nodes, downstream, visited, lines, indent)
        else:
            lines.append(indent + _CONN + "|")
            for i, child in enumerate(children):
                is_last = i == len(children) - 1
                branch   = "'---> " if is_last else "+---> "
                cont     = "      " if is_last else "|     "
                child_prefix = indent + _CONN + cont

                # Render child subtree into a temp buffer using the continuation indent.
                child_lines: list[str] = []
                self._render(child, nodes, downstream, visited, child_lines, child_prefix)

                # Swap the continuation prefix on the very first line for the branch arrow.
                if child_lines:
                    first_content = child_lines[0][len(child_prefix):]
                    lines.append(indent + _CONN + branch + first_content)
                    lines.extend(child_lines[1:])

                if not is_last:
                    lines.append(indent + _CONN + "|")
