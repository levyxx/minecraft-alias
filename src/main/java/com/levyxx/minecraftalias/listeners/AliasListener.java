package com.levyxx.minecraftalias.listeners;

import com.levyxx.minecraftalias.AliasManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

public final class AliasListener implements Listener {
    private static final String PREFIX = ChatColor.GRAY + "[" + ChatColor.GREEN + "Alias" + ChatColor.GRAY + "] " + ChatColor.RESET;

    private final AliasManager aliasManager;
    private final Logger logger;

    public AliasListener(AliasManager aliasManager, Logger logger) {
        this.aliasManager = aliasManager;
        this.logger = logger;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        if (message == null || message.isBlank() || message.charAt(0) != '/') {
            return;
        }

        Player player = event.getPlayer();
        String raw = message.substring(1).trim();
        Optional<ResolvedCommand> resolved = resolve(raw, player);
        if (resolved.isEmpty()) {
            return;
        }

        event.setCancelled(true);
        String commandToDispatch = resolved.get().command();
        boolean success = player.performCommand(commandToDispatch);
        if (!success) {
            player.sendMessage(PREFIX + ChatColor.RED + "エイリアス先のコマンド実行に失敗しました。");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onServerCommand(ServerCommandEvent event) {
        String command = event.getCommand();
        if (command == null || command.isBlank()) {
            return;
        }

        Optional<ResolvedCommand> resolved = resolve(command.trim(), event.getSender());
        resolved.ifPresent(res -> {
            event.setCommand(res.command());
        });
    }

    private Optional<ResolvedCommand> resolve(String raw, CommandSender sender) {
        if (raw.isEmpty()) {
            return Optional.empty();
        }

        String[] parts = raw.split("\\s+");
        String label = parts[0];
        List<String> args = new ArrayList<>();
        if (parts.length > 1) {
            for (int i = 1; i < parts.length; i++) {
                args.add(parts[i]);
            }
        }

        Optional<String> resolved = aliasManager.resolveCommand(label, args);
        if (resolved.isEmpty()) {
            if (aliasManager.getAlias(label).isPresent()) {
                sender.sendMessage(PREFIX + ChatColor.RED + "エイリアスの解決に失敗しました。循環参照がないか確認してください。");
                logger.warning(() -> "Failed to resolve alias '/" + label + "' due to potential loop.");
            }
            return Optional.empty();
        }

        String commandLine = resolved.get();
        if (sender instanceof Player player) {
            commandLine = applyPlayerPlaceholders(commandLine, player);
        }

        return Optional.of(new ResolvedCommand(commandLine));
    }

    private String applyPlayerPlaceholders(String commandLine, Player player) {
        return commandLine
                .replace("%player%", player.getName())
                .replace("%displayname%", ChatColor.stripColor(player.getDisplayName()));
    }

    private record ResolvedCommand(String command) { }
}
