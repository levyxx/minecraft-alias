package com.levyxx.minecraftalias;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Persists, validates, and resolves command aliases.
 */
public final class AliasManager {
    private static final Pattern ALIAS_PATTERN = Pattern.compile("^[a-z0-9_\\-:.]{1,32}$", Pattern.CASE_INSENSITIVE);

    private final JavaPlugin plugin;
    private final Map<String, AliasRecord> aliases = new LinkedHashMap<>();

    public AliasManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isValidAlias(String alias) {
        return ALIAS_PATTERN.matcher(alias).matches()
                && !"alias".equalsIgnoreCase(alias)
                && !alias.contains(" ");
    }

    public synchronized boolean addAlias(String alias, String targetCommand) {
        String normalized = normalize(alias);
        if (!isValidAlias(alias) || aliases.containsKey(normalized)) {
            return false;
        }

        String sanitizedCommand = stripLeadingSlash(targetCommand);
        if (sanitizedCommand.isEmpty()) {
            return false;
        }

        aliases.put(normalized, new AliasRecord(alias, sanitizedCommand));
        save();
        return true;
    }

    public synchronized Optional<AliasRecord> removeAlias(String alias) {
        AliasRecord removed = aliases.remove(normalize(alias));
        if (removed != null) {
            save();
        }
        return Optional.ofNullable(removed);
    }

    public synchronized Optional<AliasRecord> getAlias(String alias) {
        return Optional.ofNullable(aliases.get(normalize(alias)));
    }

    public synchronized Collection<AliasRecord> listAliases() {
        List<AliasRecord> records = new ArrayList<>(aliases.values());
        records.sort(Comparator.comparing(AliasRecord::alias, String.CASE_INSENSITIVE_ORDER));
        return Collections.unmodifiableList(records);
    }

    public synchronized Optional<String> resolveCommand(String alias, List<String> trailingArgs) {
        if (aliases.isEmpty()) {
            return Optional.empty();
        }

        String current = normalize(alias);
        List<String> args = new ArrayList<>(trailingArgs);
        Set<String> visited = new HashSet<>();

        while (true) {
            if (!visited.add(current)) {
                return Optional.empty();
            }

            AliasRecord record = aliases.get(current);
            if (record == null) {
                return Optional.empty();
            }

            String command = record.command();
            if (command.isEmpty()) {
                return Optional.empty();
            }

            String[] parts = command.split("\\s+");
            String baseLabel = parts[0];
            List<String> baseArgs = new ArrayList<>();
            if (parts.length > 1) {
                baseArgs.addAll(Arrays.asList(parts).subList(1, parts.length));
            }
            baseArgs.addAll(args);

            String normalizedBase = normalize(baseLabel);
            if (!aliases.containsKey(normalizedBase)) {
                StringBuilder builder = new StringBuilder(baseLabel);
                if (!baseArgs.isEmpty()) {
                    builder.append(' ').append(String.join(" ", baseArgs));
                }
                return Optional.of(builder.toString());
            }

            current = normalizedBase;
            args = baseArgs;
        }
    }

    public synchronized int size() {
        return aliases.size();
    }

    public synchronized void reload() {
        aliases.clear();
        if (!plugin.getConfig().isConfigurationSection("aliases")) {
            return;
        }

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("aliases");
        if (section == null) {
            return;
        }

        Set<String> keys = section.getKeys(false);
        List<String> ordered = new ArrayList<>(keys);
        ordered.sort(String.CASE_INSENSITIVE_ORDER);

        for (String key : ordered) {
            String value = section.getString(key);
            if (value == null || value.isBlank()) {
                continue;
            }

            String aliasKey = normalize(key);
            if (!isValidAlias(key) || aliases.containsKey(aliasKey)) {
                continue;
            }

            aliases.put(aliasKey, new AliasRecord(key, stripLeadingSlash(value)));
        }
    }

    public synchronized void save() {
        plugin.getConfig().set("aliases", null);
        for (AliasRecord record : aliases.values()) {
            plugin.getConfig().set("aliases." + record.alias(), record.command());
        }
        plugin.saveConfig();
    }

    private String normalize(String alias) {
        return alias.trim().toLowerCase(Locale.ROOT);
    }

    private String stripLeadingSlash(String command) {
        if (command == null) {
            return "";
        }

        String trimmed = command.trim();
        if (trimmed.startsWith("/")) {
            return trimmed.substring(1);
        }
        return trimmed;
    }

    public record AliasRecord(String alias, String command) { }
}
