package com.aicraft.aiworkbench.command;

import com.aicraft.aiworkbench.AIWorkbenchPlugin;
import com.aicraft.aiworkbench.gui.GuiManager;
import com.aicraft.aiworkbench.item.WorkbenchManager;
import com.aicraft.aiworkbench.repository.JsonItemRepository;
import com.aicraft.aiworkbench.resourcepack.ResourcePackManager;
import com.aicraft.aiworkbench.service.CreationService;
import com.aicraft.aiworkbench.util.Messages;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class AIWorkbenchCommand implements BasicCommand {
    private final AIWorkbenchPlugin plugin;
    private final Messages messages;
    private final WorkbenchManager workbenchManager;
    private final GuiManager guiManager;
    private final CreationService creationService;
    private final ResourcePackManager resourcePackManager;
    private final JsonItemRepository repository;

    public AIWorkbenchCommand(AIWorkbenchPlugin plugin, Messages messages, WorkbenchManager workbenchManager,
                              GuiManager guiManager, CreationService creationService, ResourcePackManager resourcePackManager,
                              JsonItemRepository repository) {
        this.plugin = plugin;
        this.messages = messages;
        this.workbenchManager = workbenchManager;
        this.guiManager = guiManager;
        this.creationService = creationService;
        this.resourcePackManager = resourcePackManager;
        this.repository = repository;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(messages.get("only-player"));
                return;
            }
            guiManager.openMain(player);
            return;
        }
        switch (args[0].toLowerCase()) {
            case "give" -> give(sender, args);
            case "create" -> create(sender);
            case "library" -> library(sender);
            case "reload" -> reload(sender);
            case "debug" -> sender.sendMessage(messages.get("debug", Map.of("value", String.valueOf(plugin.pluginConfig().debug()))));
            case "resourcepack" -> rebuild(sender, args);
            default -> sender.sendMessage(Component.text("/aiworkbench give|create|library|reload|debug|resourcepack rebuild"));
        }
    }

    private void create(CommandSender sender) {
        if (sender instanceof Player player) {
            creationService.awaitPrompt(player);
        } else {
            sender.sendMessage(messages.get("only-player"));
        }
    }

    private void library(CommandSender sender) {
        if (sender instanceof Player player) {
            guiManager.openLibrary(player);
        } else {
            sender.sendMessage(messages.get("only-player"));
        }
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("aiworkbench.reload")) {
            sender.sendMessage(messages.get("no-permission"));
            return;
        }
        plugin.reloadPlugin();
        sender.sendMessage(messages.get("reload"));
    }

    private void give(CommandSender sender, String[] args) {
        if (!sender.hasPermission("aiworkbench.give")) {
            sender.sendMessage(messages.get("no-permission"));
            return;
        }
        Player target = args.length >= 2 ? Bukkit.getPlayerExact(args[1]) : sender instanceof Player player ? player : null;
        if (target == null) {
            sender.sendMessage(Component.text("Игрок не найден."));
            return;
        }
        target.getInventory().addItem(workbenchManager.createWorkbenchItem());
        sender.sendMessage(messages.get("workbench-given-other", Map.of("player", target.getName())));
    }

    private void rebuild(CommandSender sender, String[] args) {
        if (args.length < 2 || !"rebuild".equalsIgnoreCase(args[1])) {
            sender.sendMessage(Component.text("/aiworkbench resourcepack rebuild"));
            return;
        }
        if (!sender.hasPermission("aiworkbench.resourcepack")) {
            sender.sendMessage(messages.get("no-permission"));
            return;
        }
        try {
            sender.sendMessage(messages.get("resourcepack-built", Map.of("file", resourcePackManager.rebuild(repository.items()).toString())));
        } catch (IOException exception) {
            plugin.getLogger().warning("Resource pack rebuild failed: " + exception.getMessage());
            sender.sendMessage(Component.text("Не удалось пересобрать ресурспак."));
        }
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (args.length == 1) {
            return List.of("give", "create", "library", "reload", "debug", "resourcepack").stream()
                    .filter(value -> value.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        if (args.length == 2 && "resourcepack".equalsIgnoreCase(args[0])) {
            return List.of("rebuild");
        }
        if (args.length == 2 && "give".equalsIgnoreCase(args[0])) {
            return new ArrayList<>(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        }
        return List.of();
    }
}
