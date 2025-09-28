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
    private static final Pattern ALIAS_PART_PATTERN = Pattern.compile("^[a-z0-9_\\-:.]{1,32}$", Pattern.CASE_INSENSITIVE);

    private final JavaPlugin plugin;
    private final Map<String, AliasRecord> aliases = new LinkedHashMap<>();

    public AliasManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isValidAlias(String alias) {
        List<String> tokens = tokenizeAlias(alias);
        return validateAliasTokens(tokens);
    }

    public synchronized boolean addAlias(String alias, String targetCommand) {
        List<String> aliasTokens = tokenizeAlias(alias);
        if (!validateAliasTokens(aliasTokens)) {
            return false;
        }

        String normalizedAlias = normalizeTokens(aliasTokens);
        if (aliases.containsKey(normalizedAlias)) {
            return false;
        }

        String sanitizedTargetCommand = sanitizeCommand(targetCommand);
        if (sanitizedTargetCommand.isEmpty()) {
            return false;
        }

        AliasRecord record = new AliasRecord(joinTokens(aliasTokens), aliasTokens, sanitizedTargetCommand);
        aliases.put(normalizedAlias, record);
        save();
        return true;
    }

    public synchronized Optional<AliasRecord> removeAlias(String alias) {
        List<String> aliasTokens = tokenizeAlias(alias);
        if (aliasTokens.isEmpty()) {
            return Optional.empty();
        }

        AliasRecord removed = aliases.remove(normalizeTokens(aliasTokens));
        if (removed != null) {
            save();
        }
        return Optional.ofNullable(removed);
    }

    public synchronized Optional<AliasRecord> getAlias(String alias) {
        List<String> aliasTokens = tokenizeAlias(alias);
        if (aliasTokens.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(aliases.get(normalizeTokens(aliasTokens)));
    }

    public synchronized Collection<AliasRecord> listAliases() {
        List<AliasRecord> records = new ArrayList<>(aliases.values());
        records.sort(Comparator.comparing(AliasRecord::alias, String.CASE_INSENSITIVE_ORDER));
        return Collections.unmodifiableList(records);
    }

    public synchronized Optional<String> resolveCommand(List<String> inputTokens) {
        if (aliases.isEmpty() || inputTokens.isEmpty()) {
            return Optional.empty();
        }

        List<String> tokens = new ArrayList<>(inputTokens);
        Set<String> visited = new HashSet<>();
        boolean matchedAny = false;

        while (true) {
            Match match = findLongestMatch(tokens);
            if (match == null) {
                return matchedAny ? Optional.of(joinTokens(tokens)) : Optional.empty();
            }

            matchedAny = true;
            AliasRecord record = match.record();
            if (!visited.add(record.normalizedAlias())) {
                return Optional.empty();
            }

            List<String> commandTokens = tokenizeCommand(record.command());
            commandTokens.addAll(match.remainingTokens());
            tokens = commandTokens;
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

            List<String> aliasTokens = tokenizeAlias(key);
            if (!validateAliasTokens(aliasTokens)) {
                continue;
            }

            String normalized = normalizeTokens(aliasTokens);
            if (aliases.containsKey(normalized)) {
                continue;
            }

            String sanitizedCommand = sanitizeCommand(value);
            if (sanitizedCommand.isEmpty()) {
                continue;
            }

            AliasRecord record = new AliasRecord(joinTokens(aliasTokens), aliasTokens, sanitizedCommand);
            aliases.put(normalized, record);
        }
    }

    public synchronized void save() {
        plugin.getConfig().set("aliases", null);
        for (AliasRecord record : aliases.values()) {
            plugin.getConfig().set("aliases." + record.alias(), record.command());
        }
        plugin.saveConfig();
    }

    private boolean validateAliasTokens(List<String> tokens) {
        if (tokens.isEmpty()) {
            return false;
        }

        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            if (!ALIAS_PART_PATTERN.matcher(token).matches()) {
                return false;
            }
            if (i == 0 && "alias".equalsIgnoreCase(token)) {
                return false;
            }
        }
        return true;
    }

    private Match findLongestMatch(List<String> tokens) {
        AliasRecord bestRecord = null;
        int bestLength = 0;

        outer:
        for (AliasRecord record : aliases.values()) {
            List<String> aliasTokens = record.aliasTokens();
            if (aliasTokens.size() > tokens.size()) {
                continue;
            }

            for (int i = 0; i < aliasTokens.size(); i++) {
                if (!aliasTokens.get(i).equalsIgnoreCase(tokens.get(i))) {
                    continue outer;
                }
            }

            if (aliasTokens.size() > bestLength) {
                bestRecord = record;
                bestLength = aliasTokens.size();
            }
        }

        if (bestRecord == null) {
            return null;
        }

        List<String> remaining = new ArrayList<>(tokens.subList(bestLength, tokens.size()));
        return new Match(bestRecord, remaining);
    }

    private List<String> tokenizeAlias(String alias) {
        if (alias == null) {
            return Collections.emptyList();
        }

        String prepared = stripLeadingSlash(alias);
        if (prepared.isEmpty()) {
            return Collections.emptyList();
        }

        String normalizedSpacing = collapseWhitespace(prepared);
        if (normalizedSpacing.isEmpty()) {
            return Collections.emptyList();
        }

        String[] parts = normalizedSpacing.split(" ");
        return new ArrayList<>(Arrays.asList(parts));
    }

    private List<String> tokenizeCommand(String command) {
        if (command == null || command.isBlank()) {
            return new ArrayList<>();
        }
        String normalizedSpacing = collapseWhitespace(command);
        return new ArrayList<>(Arrays.asList(normalizedSpacing.split(" ")));
    }

    private String sanitizeCommand(String command) {
        String withoutSlash = stripLeadingSlash(command);
        if (withoutSlash.isEmpty()) {
            return "";
        }
        return collapseWhitespace(withoutSlash);
    }

    private static String joinTokens(List<String> tokens) {
        return String.join(" ", tokens);
    }

    private static String normalizeTokens(List<String> tokens) {
        return joinTokens(tokens).toLowerCase(Locale.ROOT);
    }

    private String collapseWhitespace(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private String stripLeadingSlash(String command) {
        if (command == null) {
            return "";
        }

        String trimmed = command.trim();
        if (trimmed.startsWith("/")) {
            return trimmed.substring(1).trim();
        }
        return trimmed;
    }

    private record Match(AliasRecord record, List<String> remainingTokens) { }

    public static final class AliasRecord {
        private final String alias;
        private final List<String> aliasTokens;
        private final String normalizedAlias;
        private final String command;

        private AliasRecord(String alias, List<String> aliasTokens, String command) {
            this.alias = alias;
            this.aliasTokens = List.copyOf(aliasTokens);
            this.normalizedAlias = normalizeTokens(aliasTokens);
            this.command = command;
        }

        public String alias() {
            return alias;
        }

        public List<String> aliasTokens() {
            return aliasTokens;
        }

        public String normalizedAlias() {
            return normalizedAlias;
        }

        public String command() {
            return command;
        }
    }
}
