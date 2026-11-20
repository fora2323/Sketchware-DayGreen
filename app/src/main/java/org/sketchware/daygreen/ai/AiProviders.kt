package org.sketchware.daygreen.ai

object AiProviders {

    class Provider(
        @JvmField val name: String,
        @JvmField val baseUrl: String?,
        @JvmField val modelsUrl: String?,
        @JvmField val openAiCompatible: Boolean,
        @JvmField vararg val models: String
    )

    @JvmField
    val ALL: Array<Provider> = arrayOf(
        Provider(
            "OpenAI",
            "https://api.openai.com/v1/chat/completions",
            "https://api.openai.com/v1/models", true,
            "gpt-4o", "gpt-4o-mini", "gpt-4.1", "gpt-4.1-mini"
        ),
        Provider(
            "Anthropic (Claude)", null, null, false,
            "claude-sonnet-4-5", "claude-haiku-4-5", "claude-opus-4-1"
        ),
        Provider(
            "Google (Gemini)",
            "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions",
            "https://generativelanguage.googleapis.com/v1beta/openai/models", true,
            "gemini-2.5-pro", "gemini-2.5-flash"
        ),
        Provider(
            "DeepSeek",
            "https://api.deepseek.com/chat/completions",
            "https://api.deepseek.com/models", true,
            "deepseek-chat", "deepseek-reasoner"
        ),
        Provider(
            "OpenRouter",
            "https://openrouter.ai/api/v1/chat/completions",
            "https://openrouter.ai/api/v1/models", true,
            "openrouter/auto"
        ),
        Provider(
            "Moonshot (Kimi)",
            "https://api.moonshot.ai/v1/chat/completions",
            "https://api.moonshot.ai/v1/models", true,
            "kimi-k2-0711-preview"
        )
    )
}
