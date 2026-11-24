package com.aicraft.aiworkbench.visual;

import com.aicraft.aiworkbench.ai.model.GeneratedItemDefinition;
import com.aicraft.aiworkbench.ai.model.VisualStateDefinition;
import com.aicraft.aiworkbench.config.PluginConfig;

import java.util.zip.CRC32;

public final class LocalImageProvider implements ImageProvider {
    private final PluginConfig config;

    public LocalImageProvider(PluginConfig config) {
        this.config = config;
    }

    @Override
    public byte[] generatePng(GeneratedItemDefinition definition, VisualStateDefinition state) {
        CRC32 crc = new CRC32();
        crc.update((definition.id() + ":" + state.id() + ":" + state.texturePrompt()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        long value = crc.getValue();
        byte r = (byte) (40 + (value & 0x7F));
        byte g = (byte) (40 + ((value >> 8) & 0x7F));
        byte b = (byte) (40 + ((value >> 16) & 0x7F));
        return PngWriter.solid(config.textureSize(), config.textureSize(), r, g, b, (byte) 0xFF);
    }
}
