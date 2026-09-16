import json
import logging
from typing import Awaitable, Callable
from uuid import UUID

from aio_pika.abc import AbstractIncomingMessage, AbstractRobustConnection

from domain.schemas.events import DocumentoRecebidoEvent
from infrastructure.config.settings import Settings
from infrastructure.messaging.topology import declare_topology

logger = logging.getLogger(__name__)

MessageHandler = Callable[[DocumentoRecebidoEvent], Awaitable[None]]


class RabbitMQConsumer:
    def __init__(
        self,
        connection: AbstractRobustConnection,
        settings: Settings,
        handler: MessageHandler,
    ) -> None:
        self._connection = connection
        self._settings = settings
        self._handler = handler

    async def start(self) -> None:
        channel = await self._connection.channel()
        await channel.set_qos(prefetch_count=1)
        await declare_topology(channel, self._settings)
        queue = await channel.get_queue(self._settings.queue_extrair)
        await queue.consume(self._on_message)
        logger.info("Consumindo fila %s", self._settings.queue_extrair)

    async def _on_message(self, message: AbstractIncomingMessage) -> None:
        async with message.process(requeue=False):
            try:
                event = self._parse_event(message)
                await self._handler(event)
            except Exception:
                logger.exception("Falha ao processar DocumentoRecebidoEvent")
                raise

    def _parse_event(self, message: AbstractIncomingMessage) -> DocumentoRecebidoEvent:
        payload = json.loads(message.body.decode("utf-8"))
        headers = message.headers or {}
        if "tenantId" not in payload and headers.get("X-Tenant-Id"):
            payload["tenantId"] = headers["X-Tenant-Id"]
        if "correlationId" not in payload and headers.get("X-Correlation-Id"):
            payload["correlationId"] = headers["X-Correlation-Id"]
        event = DocumentoRecebidoEvent.model_validate(payload)
        _ = UUID(str(event.tenant_id))
        return event
