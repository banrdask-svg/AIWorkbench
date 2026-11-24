package com.aicraft.aiworkbench.visual;

import com.aicraft.aiworkbench.ai.model.GeneratedItemDefinition;
import com.aicraft.aiworkbench.ai.model.VisualStateDefinition;
import com.aicraft.aiworkbench.config.PluginConfig;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

public final class GeminiImageProvider implements ImageProvider {
    private final Gson gson = new Gson();
    private final HttpClient client;
    private final PluginConfig config;

    public GeminiImageProvider(PluginConfig config) {
        this.config = config;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(config.geminiTimeoutSeconds()))
                .build();
    }

    @Override
    public byte[] generatePng(GeneratedItemDefinition definition, VisualStateDefinition state) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("model", config.geminiModel());
        JsonArray input = new JsonArray();
        JsonObject text = new JsonObject();
        text.addProperty("type", "text");
        text.addProperty("text", prompt(definition, state));
        input.add(text);
        body.add("input", input);
        JsonObject responseFormat = new JsonObject();
        responseFormat.addProperty("type", "image");
        responseFormat.addProperty("mime_type", "image/png");
        responseFormat.addProperty("aspect_ratio", "1:1");
        responseFormat.addProperty("image_size", config.geminiImageSize());
        body.add("response_format", responseFormat);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.geminiBaseUrl()))
                .timeout(Duration.ofSeconds(config.geminiTimeoutSeconds()))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", config.geminiApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("Gemini вернул HTTP " + response.statusCode());
            }
            String base64 = findImageData(JsonParser.parseString(response.body()))
                    .orElseThrow(() -> new IOException("Gemini не вернул изображение"));
            return Base64.getDecoder().decode(base64);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Запрос Gemini был прерван", exception);
        }
    }

    private String prompt(GeneratedItemDefinition definition, VisualStateDefinition state) {
        String identity = definition.visualKit() == null || definition.visualKit().identity() == null
                ? "единый стиль предмета"
                : definition.visualKit().identity().identity();
        return """
                Нарисуй Minecraft resource pack texture для предмета.
                Формат: PNG, квадрат, pixel art, читаемый как item texture, без текста и букв.
                Предмет: %s.
                Визуальная идентичность: %s.
                Состояние: %s.
                Условие состояния: %s.
                Описание текстуры: %s.
                Сохрани силуэт и общую идентичность предмета между состояниями.
                """.formatted(definition.name(), identity, state.id(), state.condition(), state.texturePrompt());
    }

    private Optional<String> findImageData(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return Optional.empty();
        }
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has("mime_type") && object.has("data")
                    && object.get("mime_type").getAsString().startsWith("image/")) {
                return Optional.of(object.get("data").getAsString());
            }
            if (object.has("mimeType") && object.has("data")
                    && object.get("mimeType").getAsString().startsWith("image/")) {
                return Optional.of(object.get("data").getAsString());
            }
            if (object.has("output_image")) {
                Optional<String> nested = findImageData(object.get("output_image"));
                if (nested.isPresent()) {
                    return nested;
                }
            }
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                Optional<String> nested = findImageData(entry.getValue());
                if (nested.isPresent()) {
                    return nested;
                }
            }
        }
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                Optional<String> nested = findImageData(child);
                if (nested.isPresent()) {
                    return nested;
                }
            }
        }
        return Optional.empty();
    }
}
