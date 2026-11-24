package com.aicraft.aiworkbench.repository;

import com.aicraft.aiworkbench.ai.model.GeneratedItemDefinition;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;

public interface ItemRepository {
    void saveItem(GeneratedItemDefinition definition) throws IOException;

    Optional<GeneratedItemDefinition> find(String id);

    Collection<GeneratedItemDefinition> items();
}
