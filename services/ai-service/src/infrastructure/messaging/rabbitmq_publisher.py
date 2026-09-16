import json
from typing import Any, Dict
from uuid import UUID

import aio_pika
from aio_pika.abc import AbstractChannel

from domain.schemas.events import DocumentoExtraidoEvent
from infrastructure.config.settings import Settings


class RabbitMQPublisher:
    def __init__(self, channel: AbstractChannel, settings: Settings) -> None:
        self._channel = channel
        self._settings = settings

    async def publish_documento_extraido(self, event: DocumentoExtraidoEvent) -> None:
        exchange = await self._channel.get_exchange(self._settings.exchange_documentos)
        body = event.model_dump(by_alias=True, mode="json")
        message = aio_pika.Message(
            body=json.dumps(body, default=str).encode("utf-8"),
            content_type="application/json",
            delivery_mode=aio_pika.DeliveryMode.PERSISTENT,
            headers=self._build_headers(
                tenant_id=event.tenant_id,
                correlation_id=event.correlation_id,
                event_type=event.event_type,
                event_version=event.event_version,
            ),
        )
        await exchange.publish(
            message,
            routing_key=self._settings.routing_key_extraido,
        )

    @staticmethod
    def _build_headers(
        tenant_id: UUID,
        correlation_id: UUID,
        event_type: str,
        event_version: str,
    ) -> Dict[str, Any]:
        return {
            "X-Tenant-Id": str(tenant_id),
            "X-Correlation-Id": str(correlation_id),
            "X-Event-Type": event_type,
            "X-Event-Version": event_version,
        }
