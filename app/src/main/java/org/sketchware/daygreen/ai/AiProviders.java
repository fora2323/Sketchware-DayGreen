package org.sketchware.daygreen.ai;

public final class AiProviders {

    public static final class Provider {
        public final String name;
        public final String baseUrl;
        public final boolean openAiCompatible;
        public final String[] models;

        Provider(String name, String baseUrl, boolean openAiCompatible, String... models) {
            this.name = name;
            this.baseUrl = baseUrl;
            this.openAiCompatible = openAiCompatible;
            this.models = models;
        }
    }

    public static final Provider[] ALL = {
            new Provider("OpenAI", "https://api.openai.com/v1/chat/completions", true, "gpt-4o", "gpt-4o-mini", "gpt-4.1", "gpt-4.1-mini"),
            new Provider("Anthropic (Claude)", null, false, "claude-sonnet-4-5", "claude-haiku-4-5", "claude-opus-4-1"),
            new Provider("Google (Gemini)", null, false, "gemini-2.5-pro", "gemini-2.5-flash"),
            new Provider("DeepSeek", "https://api.deepseek.com/chat/completions", true, "deepseek-chat", "deepseek-reasoner"),
            new Provider("OpenRouter", "https://openrouter.ai/api/v1/chat/completions", true, "openrouter/auto"),
            new Provider("Moonshot (Kimi)", "https://api.moonshot.ai/v1/chat/completions", true, "kimi-k2-0711-preview")
    };

    private AiProviders() {
    }
}