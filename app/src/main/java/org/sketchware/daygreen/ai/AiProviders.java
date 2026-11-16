package org.sketchware.daygreen.ai;

public final class AiProviders {

    public static final class Provider {
        public final String name;
        public final String baseUrl;
        public final String modelsUrl;
        public final boolean openAiCompatible;
        public final String[] models;

        Provider(String name, String baseUrl, String modelsUrl, boolean openAiCompatible, String... models) {
            this.name = name;
            this.baseUrl = baseUrl;
            this.modelsUrl = modelsUrl;
            this.openAiCompatible = openAiCompatible;
            this.models = models;
        }
    }

    public static final Provider[] ALL = {
            new Provider("OpenAI",
                    "https://api.openai.com/v1/chat/completions",
                    "https://api.openai.com/v1/models", true,
                    "gpt-4o", "gpt-4o-mini", "gpt-4.1", "gpt-4.1-mini"),
            new Provider("Anthropic (Claude)", null, null, false,
                    "claude-sonnet-4-5", "claude-haiku-4-5", "claude-opus-4-1"),
            new Provider("Google (Gemini)",
                    "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions",
                    "https://generativelanguage.googleapis.com/v1beta/openai/models", true,
                    "gemini-2.5-pro", "gemini-2.5-flash"),
            new Provider("DeepSeek",
                    "https://api.deepseek.com/chat/completions",
                    "https://api.deepseek.com/models", true,
                    "deepseek-chat", "deepseek-reasoner"),
            new Provider("OpenRouter",
                    "https://openrouter.ai/api/v1/chat/completions",
                    "https://openrouter.ai/api/v1/models", true,
                    "openrouter/auto"),
            new Provider("Moonshot (Kimi)",
                    "https://api.moonshot.ai/v1/chat/completions",
                    "https://api.moonshot.ai/v1/models", true,
                    "kimi-k2-0711-preview")
    };

    private AiProviders() {
    }
}