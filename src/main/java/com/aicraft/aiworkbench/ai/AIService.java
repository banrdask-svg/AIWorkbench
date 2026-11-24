package com.aicraft.aiworkbench.ai;

import com.aicraft.aiworkbench.ai.model.GeneratedItemDefinition;
import com.aicraft.aiworkbench.ai.prompt.ItemGenerationPrompt;
import com.aicraft.aiworkbench.ai.provider.AIProvider;
import com.aicraft.aiworkbench.config.PluginConfig;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.Set;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public final class AIService {
    private final JavaPlugin plugin;
    private final AIProvider provider;
    private final PluginConfig config;
    private final Logger logger;
    private final Gson gson = new Gson();
    private final ItemDefinitionValidator validator;
    private final LocalItemGenerator localItemGenerator = new LocalItemGenerator();

    public AIService(JavaPlugin plugin, AIProvider provider, PluginConfig config, Logger logger) {
        this.plugin = plugin;
        this.provider = provider;
        this.config = config;
        this.logger = logger;
        this.validator = new ItemDefinitionValidator(config);
    }

    public CompletableFuture<GeneratedItemDefinition> generateItem(String prompt, Collection<String> customItemIds) {
        return generate(prompt, customItemIds, false);
    }

    private CompletableFuture<GeneratedItemDefinition> generate(String prompt, Collection<String> customItemIds, boolean retry) {
        String actualPrompt = prompt == null || prompt.isBlank() ? ItemGenerationPrompt.randomPrompt() : prompt;
        if (!config.aiEnabled() || config.apiKey().isBlank()) {
            return CompletableFuture.completedFuture(localItemGenerator.generate(actualPrompt, customItemIds));
        }
        return provider.generateJson(ItemGenerationPrompt.systemPrompt(customItemIds), actualPrompt)
                .thenApply(response -> parseAndValidate(response, actualPrompt, Set.copyOf(customItemIds)))
                .exceptionallyCompose(throwable -> {
                    if (!retry && config.retryOnInvalidJson()) {
                        return generate(actualPrompt + "\nИсправь предыдущий ответ и верни только валидный JSON.", customItemIds, true);
                    }
                    logger.warning("AI generation failed: " + throwable.getMessage());
                    return CompletableFuture.failedFuture(throwable);
                });
    }

    private GeneratedItemDefinition parseAndValidate(String response, String prompt, Set<String> customItemIds) {
        if (response.length() > config.maxAiResponseChars()) {
            throw new IllegalArgumentException("Ответ ИИ слишком большой");
        }
        try {
            GeneratedItemDefinition definition = gson.fromJson(response, GeneratedItemDefinition.class);
            List<String> errors = validator.validate(definition, customItemIds);
            if (!errors.isEmpty()) {
                throw new IllegalArgumentException(String.join(", ", errors));
            }
            return definition.approve(null, prompt);
        } catch (JsonParseException exception) {
            throw new IllegalArgumentException("Ответ ИИ не является валидным JSON", exception);
        }
    }
}
