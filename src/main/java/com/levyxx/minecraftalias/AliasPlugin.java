package com.levyxx.minecraftalias;

import com.levyxx.minecraftalias.commands.AliasCommand;
import com.levyxx.minecraftalias.listeners.AliasListener;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class AliasPlugin extends JavaPlugin {
    private AliasManager aliasManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        aliasManager = new AliasManager(this);
        aliasManager.reload();

    AliasCommand aliasCommand = new AliasCommand(aliasManager);
        PluginCommand command = getCommand("alias");
        if (command == null) {
            getLogger().severe("Failed to register /alias command. Check plugin.yml configuration.");
        } else {
            command.setExecutor(aliasCommand);
            command.setTabCompleter(aliasCommand);
        }

        getServer().getPluginManager().registerEvents(new AliasListener(aliasManager, getLogger()), this);
        getLogger().info(() -> String.format("Loaded %d custom alias(es).", aliasManager.size()));
    }

    @Override
    public void onDisable() {
        if (aliasManager != null) {
            aliasManager.save();
        }
    }

    public AliasManager getAliasManager() {
        return aliasManager;
    }
}
