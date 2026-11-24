package com.aicraft.aiworkbench;

import com.aicraft.aiworkbench.ai.model.AbilityDefinition;
import com.aicraft.aiworkbench.ai.model.AbilityType;
import com.aicraft.aiworkbench.ai.model.GeneratedItemDefinition;
import com.aicraft.aiworkbench.ai.model.GeneratedVisualKit;
import com.aicraft.aiworkbench.ai.model.MechanicDefinition;
import com.aicraft.aiworkbench.ai.model.MechanicStateDefinition;
import com.aicraft.aiworkbench.ai.model.Rarity;
import com.aicraft.aiworkbench.ai.model.RecipeDefinition;
import com.aicraft.aiworkbench.ai.model.TextureDefinition;
import com.aicraft.aiworkbench.ai.model.VisualAnimationDefinition;
import com.aicraft.aiworkbench.ai.model.VisualBindingDefinition;
import com.aicraft.aiworkbench.ai.model.VisualIdentityDefinition;
import com.aicraft.aiworkbench.ai.model.VisualStateDefinition;
import com.aicraft.aiworkbench.visual.ItemRuntimeState;
import com.aicraft.aiworkbench.visual.VisualStateResolver;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeneratedItemStressTest {
    private final VisualStateResolver resolver = new VisualStateResolver();

    @Test
    void resolvesRuntimeVisualStatesForFifteenGeneratedConcepts() {
        List<String> concepts = List.of(
                "charge_weapon", "multi_mode_weapon", "evolving_artifact", "custom_projectile_weapon", "summon_item",
                "deployable_machine", "zone_creator", "movement_tool", "mining_device", "farming_device",
                "combo_weapon", "environmental_artifact", "resource_spell_item", "boss_like_weapon", "unusual_concept"
        );
        for (String concept : concepts) {
            GeneratedItemDefinition item = item(concept);
            ItemRuntimeState low = new ItemRuntimeState();
            low.put("charge", "10");
            assertEquals("base", resolver.resolve(item, low).id());
            ItemRuntimeState high = new ItemRuntimeState();
            high.put("charge", "90");
            assertEquals("charged", resolver.resolve(item, high).id());
        }
    }

    private GeneratedItemDefinition item(String id) {
        return new GeneratedItemDefinition(
                id,
                id.replace('_', ' '),
                "IRON_SWORD",
                Rarity.RARE,
                10001,
                List.of("Generated stress item"),
                new MechanicDefinition(
                        6,
                        List.of("Stateful mechanic drives visual feedback"),
                        List.of(new MechanicStateDefinition("charge", "NUMBER", 0, 100, "0")),
                        List.of("ON_HIT", "SHIFT_RIGHT_CLICK"),
                        List.of("gain charge", "switch visual mode"),
                        List.of("base", "charged"),
                        List.of("PULL", "PUSH"),
                        List.of("base", "awakened")
                ),
                new RecipeDefinition("SHAPED", List.of(" I ", " I ", " S "), Map.of("I", "IRON_INGOT", "S", "STICK")),
                List.of(new AbilityDefinition(AbilityType.DAMAGE, 5, 1, 2, 1)),
                null,
                new TextureDefinition("consistent item texture", id),
                new GeneratedVisualKit(
                        new VisualIdentityDefinition("same item identity", "same silhouette", List.of("iron"), List.of("cyan"), List.of("core"), List.of("family change")),
                        "charge reacts visually",
                        List.of(
                                new VisualStateDefinition("base", "charge < 25", "low charge same identity", "HANDHELD", null),
                                new VisualStateDefinition("charged", "charge >= 25", "high charge same identity", "HANDHELD",
                                        new VisualAnimationDefinition("PULSE", 4, 2, true, 1.5))
                        ),
                        List.of(new VisualBindingDefinition("charge", "charge_level")),
                        "item icon",
                        "held model",
                        "projectile",
                        "zone",
                        "summon",
                        "placed"
                ),
                1,
                1,
                null,
                id,
                Instant.now(),
                true,
                0
        );
    }
}
