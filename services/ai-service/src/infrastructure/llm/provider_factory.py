from infrastructure.config.settings import Settings
from infrastructure.llm.fallback_provider import LLMProviderStrategy
from infrastructure.llm.gemini_provider import GeminiProvider


def create_primary_provider(settings: Settings) -> GeminiProvider:
    return GeminiProvider(
        api_key=settings.require_gemini_api_key(),
        model_name=settings.gemini_primary_model,
    )


def create_fallback_provider(settings: Settings) -> GeminiProvider:
    return GeminiProvider(
        api_key=settings.require_gemini_api_key(),
        model_name=settings.gemini_fallback_model,
    )


def create_llm_strategy(settings: Settings) -> LLMProviderStrategy:
    return LLMProviderStrategy(
        primary=create_primary_provider(settings),
        fallback=create_fallback_provider(settings),
        confidence_threshold=settings.llm_confidence_threshold,
    )
