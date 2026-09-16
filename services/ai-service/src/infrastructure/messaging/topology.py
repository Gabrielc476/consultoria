import aio_pika
from aio_pika import ExchangeType
from aio_pika.abc import AbstractChannel, AbstractRobustConnection

from infrastructure.config.settings import Settings


async def connect_rabbitmq(settings: Settings) -> AbstractRobustConnection:
    return await aio_pika.connect_robust(settings.rabbitmq_url)


async def declare_topology(channel: AbstractChannel, settings: Settings) -> None:
    exchange = await channel.declare_exchange(
        settings.exchange_documentos,
        ExchangeType.TOPIC,
        durable=True,
    )
    dlx = await channel.declare_exchange(
        settings.exchange_documentos_dlx,
        ExchangeType.TOPIC,
        durable=True,
    )

    queue_dlq = await channel.declare_queue(settings.queue_dlq, durable=True)
    await queue_dlq.bind(dlx, routing_key="#")

    queue_extrair = await channel.declare_queue(
        settings.queue_extrair,
        durable=True,
        arguments={
            "x-dead-letter-exchange": settings.exchange_documentos_dlx,
            "x-dead-letter-routing-key": "documento.recebido.falha",
        },
    )
    await queue_extrair.bind(exchange, routing_key=settings.routing_key_recebido)

    queue_processados = await channel.declare_queue(
        settings.queue_processados,
        durable=True,
    )
    await queue_processados.bind(exchange, routing_key=settings.routing_key_extraido)
