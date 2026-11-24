package com.aicraft.aiworkbench.ai.provider;

import com.aicraft.aiworkbench.config.PluginConfig;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public final class OpenAICompatibleProvider implements AIProvider {
    private final Gson gson = new Gson();
    private final HttpClient client;
    private final PluginConfig config;
    private final Logger logger;

    public OpenAICompatibleProvider(HttpClient client, PluginConfig config, Logger logger) {
        this.client = client;
        this.config = config;
        this.logger = logger;
    }

    @Override
    public CompletableFuture<String> generateJson(String systemPrompt, String userPrompt) {
        JsonObject body = new JsonObject();
        body.addProperty("model", config.model());
        JsonArray messages = new JsonArray();
        JsonObject system = new JsonObject();
        system.addProperty("role", "system");
        system.addProperty("content", systemPrompt);
        messages.add(system);
        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty("content", userPrompt);
        messages.add(user);
        body.add("messages", messages);
        body.addProperty("stream", false);
        JsonObject thinking = new JsonObject();
        thinking.addProperty("type", "enabled");
        body.add("thinking", thinking);
        body.addProperty("reasoning_effort", config.reasoningEffort());
        JsonObject format = new JsonObject();
        format.addProperty("type", "json_object");
        body.add("response_format", format);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.baseUrl()))
                .timeout(Duration.ofSeconds(config.timeoutSeconds()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + config.apiKey())
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .build();
        long started = System.currentTimeMillis();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (config.debug()) {
                        logger.info("AI request completed model=" + config.model()
                                + " status=" + response.statusCode()
                                + " durationMs=" + (System.currentTimeMillis() - started));
                    }
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        throw new IllegalStateException("AI provider returned HTTP " + response.statusCode());
                    }
                    return extractText(response.body());
                });
    }

    private String extractText(String responseBody) {
        JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
        JsonArray choices = root.getAsJsonArray("choices");
        if (choices != null && !choices.isEmpty()) {
            JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
            if (message != null && message.has("content") && !message.get("content").isJsonNull()) {
                return message.get("content").getAsString();
            }
        }
        if (root.has("output_text")) {
            return root.get("output_text").getAsString();
        }
        JsonArray output = root.getAsJsonArray("output");
        if (output == null) {
            return responseBody;
        }
        StringBuilder builder = new StringBuilder();
        output.forEach(item -> item.getAsJsonObject().getAsJsonArray("content").forEach(content -> {
            JsonObject contentObject = content.getAsJsonObject();
            if (contentObject.has("text")) {
                builder.append(contentObject.get("text").getAsString());
            }
        }));
        return builder.toString();
    }
}
