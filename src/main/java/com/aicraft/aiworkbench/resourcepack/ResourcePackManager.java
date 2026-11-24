package com.aicraft.aiworkbench.resourcepack;

import com.aicraft.aiworkbench.ai.model.GeneratedItemDefinition;
import com.aicraft.aiworkbench.ai.model.VisualAnimationDefinition;
import com.aicraft.aiworkbench.ai.model.VisualStateDefinition;
import com.aicraft.aiworkbench.config.PluginConfig;
import com.aicraft.aiworkbench.visual.TextureGenerationService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HexFormat;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ResourcePackManager {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final JavaPlugin plugin;
    private final PluginConfig config;
    private final TextureGenerationService textureGenerationService;
    private String lastSha1 = "";
    private int packVersion;

    public ResourcePackManager(JavaPlugin plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.textureGenerationService = new TextureGenerationService(config, plugin.getLogger());
    }

    public Path rebuild(Collection<GeneratedItemDefinition> definitions) throws IOException {
        Path root = plugin.getDataFolder().toPath().resolve("generated-resourcepack");
        Path assets = root.resolve("assets").resolve("aiworkbench");
        Path itemDefinitions = assets.resolve("items").resolve("generated");
        Path models = assets.resolve("models").resolve("item").resolve("generated");
        Path textures = assets.resolve("textures").resolve("item").resolve("generated");
        Files.createDirectories(itemDefinitions);
        Files.createDirectories(models);
        Files.createDirectories(textures);
        packVersion = readPackVersion(root) + 1;
        Files.writeString(root.resolve(config.packVersionFile()), String.valueOf(packVersion));
        Files.writeString(root.resolve("pack.mcmeta"), gson.toJson(Map.of(
                "pack", Map.of("pack_format", 55, "description", "AIWorkbench generated resource pack")
        )));
        for (GeneratedItemDefinition definition : definitions) {
            for (VisualStateDefinition state : visualStates(definition)) {
                String stateId = safeFileName(state.id());
                Path itemDir = itemDefinitions.resolve(safeFileName(definition.id()));
                Path modelDir = models.resolve(safeFileName(definition.id()));
                Path textureDir = textures.resolve(safeFileName(definition.id()));
                Files.createDirectories(itemDir);
                Files.createDirectories(modelDir);
                Files.createDirectories(textureDir);
                Files.writeString(itemDir.resolve(stateId + ".json"), gson.toJson(Map.of(
                        "model", Map.of(
                                "type", "minecraft:model",
                                "model", "aiworkbench:item/generated/" + safeFileName(definition.id()) + "/" + stateId
                        )
                )));
                String parent = "HANDHELD".equalsIgnoreCase(state.modelType()) ? "minecraft:item/handheld" : "minecraft:item/generated";
                Files.writeString(modelDir.resolve(stateId + ".json"), gson.toJson(Map.of(
                        "parent", parent,
                        "textures", Map.of("layer0", "aiworkbench:item/generated/" + safeFileName(definition.id()) + "/" + stateId)
                )));
                Path texture = textureDir.resolve(stateId + ".png");
                textureGenerationService.writeTexture(texture, definition, clampAnimation(state));
            }
        }
        Path zip = plugin.getDataFolder().toPath().resolve("AIWorkbench-resourcepack.zip");
        zip(root, zip);
        lastSha1 = sha1(zip);
        return zip;
    }

    public String lastSha1() {
        return lastSha1;
    }

    public int packVersion() {
        return packVersion;
    }

    private Collection<VisualStateDefinition> visualStates(GeneratedItemDefinition definition) {
        if (definition.visualKit() != null && definition.visualKit().states() != null && !definition.visualKit().states().isEmpty()) {
            return definition.visualKit().states().stream().limit(config.maxTexturesPerItem()).toList();
        }
        String prompt = definition.texture() == null ? definition.name() : definition.texture().description();
        return java.util.List.of(new VisualStateDefinition("base", "default", prompt, "HANDHELD", null));
    }

    private VisualStateDefinition clampAnimation(VisualStateDefinition state) {
        VisualAnimationDefinition animation = state.animation();
        if (animation == null) {
            return state;
        }
        int frames = Math.max(1, Math.min(config.maxAnimationFrames(), animation.frameCount()));
        return new VisualStateDefinition(state.id(), state.condition(), state.texturePrompt(), state.modelType(),
                new VisualAnimationDefinition(animation.type(), frames, animation.frameTime(), animation.interpolate(), animation.speed()));
    }

    private int readPackVersion(Path root) throws IOException {
        Path file = root.resolve(config.packVersionFile());
        if (!Files.exists(file)) {
            return 0;
        }
        try {
            return Integer.parseInt(Files.readString(file).trim());
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private String safeFileName(String value) {
        String base = value == null || value.isBlank() ? "generated_item" : value;
        return base.toLowerCase().replaceAll("[^a-z0-9_\\-]", "_");
    }

    private void zip(Path source, Path target) throws IOException {
        Files.createDirectories(target.getParent());
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(target))) {
            try (var paths = Files.walk(source)) {
                for (Path path : paths.filter(Files::isRegularFile).toList()) {
                    zip.putNextEntry(new ZipEntry(source.relativize(path).toString().replace('\\', '/')));
                    try (InputStream input = Files.newInputStream(path)) {
                        input.transferTo(zip);
                    }
                    zip.closeEntry();
                }
            }
        }
    }

    private String sha1(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            try (InputStream input = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IOException("SHA-1 unavailable", exception);
        }
    }
}
