package com.aicraft.aiworkbench.visual;

import com.aicraft.aiworkbench.ai.model.GeneratedItemDefinition;
import com.aicraft.aiworkbench.ai.model.VisualStateDefinition;

import java.io.IOException;

public interface ImageProvider {
    byte[] generatePng(GeneratedItemDefinition definition, VisualStateDefinition state) throws IOException;
}
