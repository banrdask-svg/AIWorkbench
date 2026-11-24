package com.aicraft.aiworkbench.ai.provider;

import java.util.concurrent.CompletableFuture;

public interface AIProvider {
    CompletableFuture<String> generateJson(String systemPrompt, String userPrompt);
}
