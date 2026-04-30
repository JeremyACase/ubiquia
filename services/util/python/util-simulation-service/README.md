# util-simulation-service

CLI for running multi-agent simulations against a live Ubiquia deployment.

## Prerequisites

- Python 3.10+
- [uv](https://docs.astral.sh/uv/)

## Run a simulation

```bash
uv run util-simulation-service simulation run --input-file simulations/devops-simulation.yaml
```

By default the event dump is written to the current directory. Use `--output-path` and `--output-file-name` to change the destination:

```bash
uv run util-simulation-service simulation run \
  --input-file simulations/devops-simulation.yaml \
  --output-path /tmp/results \
  --output-file-name my-run.json
```

### Simulation file format

```yaml
name: my-simulation
agents:
  - name: agent-a
    mode: test                        # test | microweight | kind
    base_url: http://ubiquia-agent-a:8080
events: []
networks: []
speed: 1.0                            # wall-clock multiplier
```

Example files live in the `simulations/` directory.

## Other commands

```bash
uv run util-simulation-service --help
uv run util-simulation-service simulation --help
uv run util-simulation-service graph --help
```
