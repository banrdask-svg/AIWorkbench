package com.aicraft.aiworkbench;

import com.aicraft.aiworkbench.ability.AbilityRegistry;
import com.aicraft.aiworkbench.ai.AIService;
import com.aicraft.aiworkbench.ai.provider.OpenAICompatibleProvider;
import com.aicraft.aiworkbench.command.AIWorkbenchCommand;
import com.aicraft.aiworkbench.config.PluginConfig;
import com.aicraft.aiworkbench.gui.GuiManager;
import com.aicraft.aiworkbench.item.CustomItemFactory;
import com.aicraft.aiworkbench.item.WorkbenchManager;
import com.aicraft.aiworkbench.listener.AbilityListener;
import com.aicraft.aiworkbench.listener.CreationChatListener;
import com.aicraft.aiworkbench.listener.GuiListener;
import com.aicraft.aiworkbench.listener.WorkbenchListener;
import com.aicraft.aiworkbench.recipe.RecipeManager;
import com.aicraft.aiworkbench.repository.JsonItemRepository;
import com.aicraft.aiworkbench.resourcepack.ResourcePackManager;
import com.aicraft.aiworkbench.service.CreationService;
import com.aicraft.aiworkbench.service.RateLimitService;
import com.aicraft.aiworkbench.util.Messages;
import com.aicraft.aiworkbench.visual.DynamicModelController;
import com.aicraft.aiworkbench.visual.ItemStateService;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;

public final class AIWorkbenchPlugin extends JavaPlugin {
    private PluginConfig pluginConfig;
    private Messages messages;
    private JsonItemRepository repository;
    private RecipeManager recipeManager;
    private CustomItemFactory itemFactory;
    private WorkbenchManager workbenchManager;
    private GuiManager guiManager;
    private CreationService creationService;
    private ResourcePackManager resourcePackManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);
        reloadLocalConfig();

        repository = new JsonItemRepository(this);
        try {
            repository.load();
        } catch (IOException exception) {
            getLogger().severe("Could not load item repository: " + exception.getMessage());
        }

        itemFactory = new CustomItemFactory(this);
        ItemStateService itemStateService = itemFactory.stateService();
        DynamicModelController dynamicModelController = new DynamicModelController(this, itemStateService);
        itemFactory.setModelController(dynamicModelController);
        workbenchManager = new WorkbenchManager(this);
        recipeManager = new RecipeManager(this, itemFactory, repository);
        AbilityRegistry abilityRegistry = new AbilityRegistry(this);
        RateLimitService rateLimitService = new RateLimitService(pluginConfig);
        resourcePackManager = new ResourcePackManager(this, pluginConfig);

        OpenAICompatibleProvider provider = new OpenAICompatibleProvider(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(pluginConfig.timeoutSeconds())).build(),
                pluginConfig,
                getLogger()
        );
        AIService aiService = new AIService(this, provider, pluginConfig, getLogger());
        creationService = new CreationService(this, pluginConfig, messages, aiService, repository, itemFactory, recipeManager, resourcePackManager, rateLimitService);
        guiManager = new GuiManager(this, messages, creationService, repository, workbenchManager, resourcePackManager);
        creationService.setPreviewOpener(guiManager::openPreview);

        recipeManager.registerWorkbenchRecipe(workbenchManager.createWorkbenchItem());
        recipeManager.registerAll(repository.items());

        getServer().getPluginManager().registerEvents(new WorkbenchListener(this, workbenchManager, guiManager), this);
        getServer().getPluginManager().registerEvents(new GuiListener(guiManager), this);
        getServer().getPluginManager().registerEvents(new CreationChatListener(this, creationService), this);
        getServer().getPluginManager().registerEvents(new AbilityListener(itemFactory, repository, abilityRegistry, itemStateService, dynamicModelController), this);

        AIWorkbenchCommand command = new AIWorkbenchCommand(this, messages, workbenchManager, guiManager, creationService, resourcePackManager, repository);
        registerCommand("aiworkbench", "Main AIWorkbench command.", java.util.List.of("aiwb"), command);
    }

    @Override
    public void onDisable() {
        if (repository != null) {
            try {
                repository.save();
            } catch (IOException exception) {
                getLogger().severe("Could not save item repository: " + exception.getMessage());
            }
        }
    }

    public void reloadPlugin() {
        reloadConfig();
        reloadLocalConfig();
    }

    private void reloadLocalConfig() {
        pluginConfig = PluginConfig.from(getConfig());
        messages = new Messages(this);
    }

    public PluginConfig pluginConfig() {
        return pluginConfig;
    }
}
