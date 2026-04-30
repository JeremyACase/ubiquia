import logging

from util_simulation_service.service.command.event_command import EventCommand
from util_simulation_service.model.events.event import Event

logger = logging.getLogger(__name__)


class EventManager:
    """Invoker that dispatches each event to its registered EventCommand.

    Commands are registered by the event type discriminator string (e.g. "simulation").
    Raises ValueError for any event whose type has no registered command.
    """

    def __init__(self, commands: dict[str, EventCommand]):
        self._commands = commands

    def dispatch(self, event: Event) -> None:
        logger.debug("Dispatching event: type=%s", event.type)
        command = self._commands.get(event.type)
        if command is None:
            raise ValueError(f"No command registered for event type: '{event.type}'")
        command.execute(event)
