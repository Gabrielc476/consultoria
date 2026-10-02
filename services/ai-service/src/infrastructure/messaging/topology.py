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
    dlx_whatsapp = await channel.declare_exchange(
        "govflow.dlx",
        ExchangeType.TOPIC,
        durable=True,
    )
    dlx_events = await channel.declare_exchange(
        "govflow.events.dlx",
        ExchangeType.TOPIC,
        durable=True,
    )

    queue_dlq = await channel.declare_queue(settings.queue_dlq, durable=True)
    await queue_dlq.bind(dlx_whatsapp, routing_key="#")

    queue_extrair = await channel.declare_queue(
        settings.queue_extrair,
        durable=True,
        arguments={
            "x-dead-letter-exchange": "govflow.dlx",
            "x-dead-letter-routing-key": "whatsapp.documento.extrair.dlq",
        },
    )
    await queue_extrair.bind(exchange, routing_key=settings.routing_key_recebido)

    queue_processados = await channel.declare_queue(
        settings.queue_processados,
        durable=True,
        arguments={
            "x-dead-letter-exchange": "govflow.events.dlx",
            "x-dead-letter-routing-key": "documento.extraido.dlq",
        },
    )
    await queue_processados.bind(exchange, routing_key=settings.routing_key_extraido)
    await queue_processados.bind(exchange, routing_key=settings.routing_key_classificado)
