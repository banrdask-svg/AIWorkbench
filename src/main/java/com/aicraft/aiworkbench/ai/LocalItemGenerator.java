package com.aicraft.aiworkbench.ai;

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
import com.aicraft.aiworkbench.ai.model.VisualDefinition;
import com.aicraft.aiworkbench.ai.model.VisualIdentityDefinition;
import com.aicraft.aiworkbench.ai.model.VisualStateDefinition;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.CRC32;

public final class LocalItemGenerator {
    public GeneratedItemDefinition generate(String prompt, Collection<String> customItemIds) {
        String idea = prompt == null || prompt.isBlank() ? "загадочный артефакт" : prompt.trim();
        String id = uniqueId(idea);
        boolean wantsLightning = containsAny(idea, "молни", "гром", "электр");
        boolean wantsFire = containsAny(idea, "огонь", "плам", "ад");
        boolean wantsIce = containsAny(idea, "лёд", "лед", "мороз", "снег");
        boolean wantsStaff = containsAny(idea, "посох", "жезл");
        boolean wantsSword = containsAny(idea, "меч", "клинок", "коса");
        String material = wantsStaff ? "BLAZE_ROD" : wantsSword ? "IRON_SWORD" : "AMETHYST_SHARD";
        AbilityType ability = wantsLightning ? AbilityType.LIGHTNING : wantsFire ? AbilityType.BURN : wantsIce ? AbilityType.FREEZE : AbilityType.DAMAGE;
        String name = title(idea);
        String customIngredient = customItemIds == null || customItemIds.isEmpty() ? null : customItemIds.iterator().next();
        return new GeneratedItemDefinition(
                id,
                name,
                material,
                Rarity.RARE,
                10000 + Math.abs(id.hashCode() % 80000),
                List.of("Создано из идеи: " + shorten(idea, 48), "Сила предмета растёт во время боя"),
                mechanic(name),
                recipe(customIngredient, wantsStaff, wantsSword),
                List.of(new AbilityDefinition(ability, 8, 4, 2.0, 3.0)),
                new VisualDefinition(wantsIce ? "SNOWFLAKE" : wantsFire ? "FLAME" : "ENCHANT", "BLOCK_AMETHYST_BLOCK_CHIME"),
                new TextureDefinition("Русская 16x16 текстура предмета: " + name + ", состояние меняется от заряда", id),
                visualKit(id, name),
                0,
                0,
                null,
                idea,
                Instant.now(),
                true,
                0
        );
    }

    private RecipeDefinition recipe(String customIngredient, boolean staff, boolean sword) {
        if (customIngredient != null) {
            return new RecipeDefinition("SHAPED", List.of(" A ", " B ", " C "), Map.of(
                    "A", "AMETHYST_SHARD",
                    "B", "aiworkbench:" + customIngredient,
                    "C", staff ? "BLAZE_ROD" : "STICK"
            ));
        }
        if (sword) {
            return new RecipeDefinition("SHAPED", List.of(" A ", " B ", " C "), Map.of("A", "AMETHYST_SHARD", "B", "IRON_INGOT", "C", "STICK"));
        }
        return new RecipeDefinition("SHAPELESS", List.of(), Map.of("A", "AMETHYST_SHARD", "B", "REDSTONE", "C", "LAPIS_LAZULI"));
    }

    private MechanicDefinition mechanic(String name) {
        return new MechanicDefinition(
                5,
                List.of(name + " накапливает заряд при использовании", "Внешний вид меняется от текущего заряда"),
                List.of(new MechanicStateDefinition("charge", "NUMBER", 0, 100, "0")),
                List.of("ON_HIT", "RIGHT_CLICK"),
                List.of("получить заряд", "сработать способностью", "обновить визуальное состояние"),
                List.of("спящий", "заряженный", "критический"),
                List.of("DEFAULT"),
                List.of("обычная форма", "пробуждённая форма")
        );
    }

    private GeneratedVisualKit visualKit(String id, String name) {
        return new GeneratedVisualKit(
                new VisualIdentityDefinition(
                        "Единый облик предмета " + name,
                        "узнаваемый силуэт одного и того же предмета",
                        List.of("металл", "кристалл", "магическая пыль"),
                        List.of("синий", "фиолетовый", "серебряный"),
                        List.of("светящееся ядро", "следы энергии"),
                        List.of("не менять тип предмета", "не менять силуэт")
                ),
                "Заряд предмета усиливает свечение и добавляет пульсацию",
                List.of(
                        new VisualStateDefinition("base", "charge < 25", "тот же предмет, слабое свечение, русская fantasy-стилистика", "HANDHELD", null),
                        new VisualStateDefinition("charged", "charge >= 25 && charge < 75", "тот же предмет, яркий заряд, заметные светящиеся линии", "HANDHELD",
                                new VisualAnimationDefinition("PULSE", 4, 3, true, 1.0)),
                        new VisualStateDefinition("critical", "charge >= 75", "тот же предмет, критический заряд, сильная магическая пульсация", "HANDHELD",
                                new VisualAnimationDefinition("FAST_PULSE", 6, 2, true, 2.0))
                ),
                List.of(new VisualBindingDefinition("charge", "уровень заряда")),
                "иконка " + id,
                "модель в руке",
                "визуал снаряда при необходимости",
                "визуал зоны при необходимости",
                "визуал призыва при необходимости",
                "поставленная модель при необходимости"
        );
    }

    private String uniqueId(String prompt) {
        CRC32 crc = new CRC32();
        crc.update(prompt.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return "item_" + Long.toHexString(crc.getValue());
    }

    private boolean containsAny(String value, String... parts) {
        String lower = value.toLowerCase(Locale.ROOT);
        for (String part : parts) {
            if (lower.contains(part)) {
                return true;
            }
        }
        return false;
    }

    private String title(String value) {
        String base = shorten(value.replaceAll("\\s+", " "), 28);
        if (base.isBlank()) {
            return "Загадочный предмет";
        }
        return Character.toUpperCase(base.charAt(0)) + base.substring(1);
    }

    private String shorten(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max - 1).trim();
    }
}
