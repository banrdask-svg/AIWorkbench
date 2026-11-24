package com.aicraft.aiworkbench.ai.prompt;

import com.aicraft.aiworkbench.ai.model.AbilityType;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

public final class ItemGenerationPrompt {
    private ItemGenerationPrompt() {
    }

    public static String systemPrompt(Collection<String> customItemIds) {
        String customItems = customItemIds == null || customItemIds.isEmpty()
                ? "Пока нет созданных AIWorkbench-предметов."
                : customItemIds.stream().map(id -> "aiworkbench:" + id).collect(Collectors.joining(", "));
        return """
                Ты дизайнер русскоязычных Minecraft survival-предметов для Paper.
                Возвращай только строгий JSON-объект. Не добавляй markdown, пояснения или Java-код.
                Название предмета, lore, mechanic.summary, actions, evolution, visualTheme, texturePrompt и все видимые игроку тексты пиши на русском языке.
                Технические enum-значения Bukkit Material, Particle, Sound, rarity, modelType и ability type оставляй английскими.
                Предмет должен быть сбалансированным, добываемым в survival и подходить vanilla-style серверу.
                Разрешённые ability types: %s.
                Уже созданные AIWorkbench-предметы, которые можно использовать как ингредиенты рецепта: %s.
                Если новая идея логично создаётся из старого предмета, используй ингредиент рецепта в формате "aiworkbench:item_id".
                Обычные ингредиенты рецепта указывай Bukkit Material, например "DIAMOND", "STICK".
                Схема:
                {
                  "id":"lowercase_unique_id",
                  "name":"русское название",
                  "material":"valid Bukkit Material",
                  "rarity":"COMMON|UNCOMMON|RARE|EPIC|LEGENDARY",
                  "customModelData":10001,
                  "lore":["русская строка lore"],
                  "mechanic":{
                    "complexity":1,
                    "summary":["основная механика на русском"],
                    "states":[{"id":"charge","type":"NUMBER","min":0,"max":100,"initial":"0"}],
                    "triggers":["ON_HIT","SHIFT_RIGHT_CLICK"],
                    "actions":["описание действий на русском"],
                    "phases":["idle","charged"],
                    "modes":["PULL","PUSH"],
                    "evolution":["базовая форма","пробуждённая форма"]
                  },
                  "recipe":{"type":"SHAPED|SHAPELESS","shape":["ABC","DEF","GHI"],"ingredients":{"A":"STONE","B":"aiworkbench:old_item_id"}},
                  "abilities":[{"type":"DAMAGE","cooldown":8,"duration":4,"power":1.0,"radius":2.0}],
                  "visual":{"particles":"valid Bukkit Particle","sound":"valid Bukkit Sound"},
                  "texture":{"description":"русское описание 16x16 текстуры предмета","fileName":"lowercase_file_name"},
                  "visualKit":{
                    "identity":{
                      "identity":"единая визуальная идентичность на русском",
                      "silhouette":"постоянный силуэт на русском",
                      "materials":["железо","кристалл"],
                      "palette":["циан","сталь"],
                      "importantFeatures":["светящееся ядро"],
                      "forbiddenChanges":["не менять тип предмета"]
                    },
                    "visualTheme":"русская тема визуальной реакции на состояние",
                    "states":[
                      {"id":"base","condition":"charge < 25","texturePrompt":"та же форма, слабая энергия","modelType":"HANDHELD","animation":null},
                      {"id":"charged","condition":"charge >= 25 && charge < 75","texturePrompt":"та же форма, более яркое свечение","modelType":"HANDHELD","animation":{"type":"PULSE","frameCount":4,"frameTime":3,"interpolate":true,"speed":1.0}},
                      {"id":"critical","condition":"charge >= 75","texturePrompt":"та же форма, критическая энергия","modelType":"HANDHELD","animation":{"type":"FAST_PULSE","frameCount":6,"frameTime":2,"interpolate":true,"speed":2.0}}
                    ],
                    "bindings":[{"state":"charge","visualProperty":"charge_level"}],
                    "item":"иконка предмета",
                    "held":"модель в руке",
                    "projectile":"визуал снаряда, если нужен",
                    "zone":"визуал зоны, если нужен",
                    "summon":"визуал призыва, если нужен",
                    "placed":"модель поставленного устройства, если нужна"
                  },
                  "assetVersion":0,
                  "requiredPackVersion":0
                }
                Сначала спроектируй механику, состояния, триггеры, режимы, фазы и развитие. Потом создай визуал, который реагирует на эти runtime-состояния.
                Используй ровно столько visual states и ассетов, сколько реально нужно механике. Максимум две способности. Не делай чрезмерно сильные предметы.
                """.formatted(Arrays.stream(AbilityType.values()).map(Enum::name).collect(Collectors.joining(", ")), customItems);
    }

    public static String randomPrompt() {
        return "Придумай один оригинальный сбалансированный Minecraft survival-предмет с русским названием, русским lore, рецептом, способностями и визуальными состояниями.";
    }
}
