package com.aicraft.aiworkbench.repository;

import com.aicraft.aiworkbench.ai.model.GeneratedItemDefinition;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class JsonItemRepository implements ItemRepository {
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
            .setPrettyPrinting()
            .create();
    private final Path file;
    private final Map<String, GeneratedItemDefinition> items = new LinkedHashMap<>();

    public JsonItemRepository(JavaPlugin plugin) {
        this.file = plugin.getDataFolder().toPath().resolve("data").resolve("items.json");
    }

    public void load() throws IOException {
        if (!Files.exists(file)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            StoredItems stored = gson.fromJson(reader, StoredItems.class);
            if (stored != null && stored.items != null) {
                stored.items.forEach(item -> items.put(item.id(), item));
            }
        }
    }

    public void save() throws IOException {
        Files.createDirectories(file.getParent());
        try (Writer writer = Files.newBufferedWriter(file)) {
            gson.toJson(new StoredItems(items.values()), writer);
        }
    }

    @Override
    public void saveItem(GeneratedItemDefinition definition) throws IOException {
        items.put(definition.id(), definition);
        save();
    }

    @Override
    public Optional<GeneratedItemDefinition> find(String id) {
        return Optional.ofNullable(items.get(id));
    }

    @Override
    public Collection<GeneratedItemDefinition> items() {
        return items.values();
    }

    private record StoredItems(Collection<GeneratedItemDefinition> items) {
    }
}
