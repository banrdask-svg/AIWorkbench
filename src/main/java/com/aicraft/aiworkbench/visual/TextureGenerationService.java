package com.aicraft.aiworkbench.visual;

import com.aicraft.aiworkbench.ai.model.GeneratedItemDefinition;
import com.aicraft.aiworkbench.ai.model.VisualStateDefinition;
import com.aicraft.aiworkbench.config.PluginConfig;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

public final class TextureGenerationService {
    private final PluginConfig config;
    private final ImageProvider primaryProvider;
    private final ImageProvider fallbackProvider;
    private final Logger logger;

    public TextureGenerationService(PluginConfig config, Logger logger) {
        this.config = config;
        this.logger = logger;
        this.fallbackProvider = new LocalImageProvider(config);
        this.primaryProvider = config.geminiEnabled() && !config.geminiApiKey().isBlank()
                ? new GeminiImageProvider(config)
                : fallbackProvider;
    }

    public void writeTexture(Path file, GeneratedItemDefinition definition, VisualStateDefinition state) throws IOException {
        Files.createDirectories(file.getParent());
        Files.write(file, generateSafePng(definition, state));
        if (state.animation() != null && state.animation().frameCount() > 1) {
            int frameTime = Math.max(1, state.animation().frameTime());
            Files.writeString(file.resolveSibling(file.getFileName() + ".mcmeta"), """
                    {"animation":{"frametime":%d,"interpolate":%s}}
                    """.formatted(frameTime, state.animation().interpolate()));
        }
    }

    private byte[] generateSafePng(GeneratedItemDefinition definition, VisualStateDefinition state) throws IOException {
        try {
            return normalizePng(primaryProvider.generatePng(definition, state));
        } catch (IOException exception) {
            if (primaryProvider != fallbackProvider) {
                logger.warning("Gemini не смог создать текстуру для " + definition.id() + "/" + state.id() + ": " + exception.getMessage());
            }
            return normalizePng(fallbackProvider.generatePng(definition, state));
        }
    }

    private byte[] normalizePng(byte[] input) throws IOException {
        if (input.length > config.maxTextureBytes()) {
            throw new IOException("PNG слишком большой");
        }
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(input));
        if (image == null) {
            throw new IOException("Файл не является PNG/изображением");
        }
        int size = Math.max(1, config.textureSize());
        BufferedImage resized = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = resized.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        graphics.drawImage(image, 0, 0, size, size, null);
        graphics.dispose();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(resized, "png", output);
        byte[] png = output.toByteArray();
        if (png.length > config.maxTextureBytes()) {
            throw new IOException("PNG после обработки слишком большой");
        }
        return png;
    }
}
