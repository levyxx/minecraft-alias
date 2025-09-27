package com.levyxx.minecraftalias.commands;

import com.levyxx.minecraftalias.AliasManager;
import com.levyxx.minecraftalias.AliasManager.AliasRecord;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class AliasCommand implements CommandExecutor, TabCompleter {
    private static final String PERMISSION = "minecraftalias.admin";
    private static final int COMMANDS_PER_PAGE = 10;
    private static final String PREFIX = ChatColor.GRAY + "[" + ChatColor.GREEN + "Alias" + ChatColor.GRAY + "] " + ChatColor.RESET;

    private final AliasManager aliasManager;

    public AliasCommand(AliasManager aliasManager) {
        this.aliasManager = aliasManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(PREFIX + ChatColor.RED + "権限がありません。");
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "add" -> handleAdd(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "list" -> handleList(sender, args);
            default -> {
                sender.sendMessage(PREFIX + ChatColor.RED + "不明なサブコマンドです。");
                sendUsage(sender, label);
            }
        }
        return true;
    }

    private void handleAdd(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(PREFIX + ChatColor.YELLOW + "/alias add <exec_command...> by <alias_command...>");
            return;
        }

        int byIndex = -1;
        for (int i = 1; i < args.length; i++) {
            if ("by".equalsIgnoreCase(args[i])) {
                byIndex = i;
                break;
            }
        }

        if (byIndex == -1) {
            sender.sendMessage(PREFIX + ChatColor.RED + "\"by\" 区切りが見つかりませんでした。");
            sender.sendMessage(ChatColor.GRAY + "例: /alias add gamemode creative by gm 1");
            return;
        }

        if (byIndex == 1) {
            sender.sendMessage(PREFIX + ChatColor.RED + "実行コマンドを指定してください。");
            return;
        }

        if (byIndex >= args.length - 1) {
            sender.sendMessage(PREFIX + ChatColor.RED + "エイリアス名を指定してください。");
            return;
        }

        List<String> execTokens = new ArrayList<>(Arrays.asList(args).subList(1, byIndex));
        List<String> aliasTokens = new ArrayList<>(Arrays.asList(args).subList(byIndex + 1, args.length));

        String execCommand = sanitizeExecutionCommand(execTokens);
        if (execCommand.isEmpty()) {
            sender.sendMessage(PREFIX + ChatColor.RED + "実行コマンドを正しく指定してください。");
            return;
        }

        String aliasName = sanitizeAliasInput(aliasTokens);
        if (aliasName.isEmpty()) {
            sender.sendMessage(PREFIX + ChatColor.RED + "エイリアス名を正しく指定してください。");
            return;
        }

        if (!aliasManager.isValidAlias(aliasName)) {
            sender.sendMessage(PREFIX + ChatColor.RED + "使用できないエイリアスです。各単語は英数字と _-:. のみ使用できます。(例: gm 1)");
            return;
        }

        boolean created = aliasManager.addAlias(aliasName, execCommand);
        if (!created) {
            sender.sendMessage(PREFIX + ChatColor.RED + "そのエイリアスは既に登録されています。");
            return;
        }

        sender.sendMessage(PREFIX + ChatColor.GREEN + String.format("/%s を実行すると /%s が実行されます。", aliasName, stripLeadingSlash(execCommand)));
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
        sender.sendMessage(PREFIX + ChatColor.YELLOW + "/alias remove <alias_command...>");
            return;
        }

        List<String> aliasTokens = new ArrayList<>(Arrays.asList(args).subList(1, args.length));
        String aliasName = sanitizeAliasInput(aliasTokens);
        if (aliasName.isEmpty()) {
            sender.sendMessage(PREFIX + ChatColor.RED + "エイリアス名を正しく指定してください。");
            return;
        }

        Optional<AliasRecord> removed = aliasManager.removeAlias(aliasName);
        if (removed.isPresent()) {
            sender.sendMessage(PREFIX + ChatColor.GREEN + String.format("/%s のエイリアスを削除しました。", removed.get().alias()));
        } else {
            sender.sendMessage(PREFIX + ChatColor.RED + "指定されたエイリアスは登録されていません。");
        }
    }

    private void handleList(CommandSender sender, String[] args) {
        int page = 1;
        if (args.length >= 2) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {
                sender.sendMessage(PREFIX + ChatColor.RED + "ページは数字で指定してください。");
                return;
            }
        }

        Collection<AliasRecord> aliases = aliasManager.listAliases();
        if (aliases.isEmpty()) {
            sender.sendMessage(PREFIX + ChatColor.YELLOW + "登録されているエイリアスはありません。");
            return;
        }

        List<AliasRecord> records = new ArrayList<>(aliases);
        int totalPages = (int) Math.ceil(records.size() / (double) COMMANDS_PER_PAGE);
        if (page > totalPages) {
            sender.sendMessage(PREFIX + ChatColor.YELLOW + String.format("ページ数は 1 〜 %d です。最後のページを表示します。", totalPages));
        }
        page = Math.max(1, Math.min(page, totalPages));

        int start = (page - 1) * COMMANDS_PER_PAGE;
        int end = Math.min(start + COMMANDS_PER_PAGE, records.size());

        sender.sendMessage(PREFIX + ChatColor.AQUA + String.format("エイリアス一覧 (%d/%d)", page, totalPages));
        for (int i = start; i < end; i++) {
            AliasRecord record = records.get(i);
            sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.GREEN + "/" + record.alias()
                    + ChatColor.GRAY + " -> " + ChatColor.WHITE + "/" + stripLeadingSlash(record.command()));
        }

        if (totalPages > 1 && page < totalPages) {
            sender.sendMessage(PREFIX + ChatColor.GRAY + String.format("次ページ: /alias list %d", page + 1));
        }
    }

    private void sendUsage(CommandSender sender, String label) {
        String base = "/" + label.toLowerCase(Locale.ROOT);
        sender.sendMessage(PREFIX + ChatColor.YELLOW + "使い方:");
        sender.sendMessage(ChatColor.GRAY + "- " + base + " add <exec_command...> by <alias_command...>");
        sender.sendMessage(ChatColor.GRAY + "- " + base + " remove <alias_command...>");
        sender.sendMessage(ChatColor.GRAY + "- " + base + " list [page]");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return partialMatches(args[0], List.of("add", "remove", "list"));
        }

        if (args.length == 2 && "remove".equalsIgnoreCase(args[0])) {
            return aliasManager.listAliases().stream()
                    .map(AliasRecord::alias)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        }

        if (args.length == 2 && "list".equalsIgnoreCase(args[0])) {
            int pages = Math.max(1, (int) Math.ceil(aliasManager.size() / (double) COMMANDS_PER_PAGE));
            List<String> pageNumbers = new ArrayList<>();
            for (int i = 1; i <= pages; i++) {
                pageNumbers.add(Integer.toString(i));
            }
            return partialMatches(args[1], pageNumbers);
        }

        if ("add".equalsIgnoreCase(args[0])) {
            boolean hasExec = false;
            for (int i = 1; i < args.length; i++) {
                if ("by".equalsIgnoreCase(args[i])) {
                    hasExec = true;
                    break;
                }
            }
            if (!hasExec) {
                return partialMatches(args[args.length - 1], List.of("by"));
            }
            return Collections.emptyList();
        }

        return Collections.emptyList();
    }

    private List<String> partialMatches(String token, List<String> candidates) {
        if (token == null || token.isEmpty()) {
            return candidates;
        }
        String lower = token.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String candidate : candidates) {
            if (candidate.toLowerCase(Locale.ROOT).startsWith(lower)) {
                matches.add(candidate);
            }
        }
        return matches;
    }

    private String sanitizeAliasInput(List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return "";
        }
        String joined = String.join(" ", tokens);
        String unquoted = stripWrappingQuotes(joined.trim());
        String withoutSlash = stripLeadingSlash(unquoted);
        return collapseWhitespace(withoutSlash);
    }

    private String sanitizeExecutionCommand(List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return "";
        }
        String joined = String.join(" ", tokens);
        String unquoted = stripWrappingQuotes(joined.trim());
        String withoutSlash = stripLeadingSlash(unquoted);
        return collapseWhitespace(withoutSlash);
    }

    private String stripWrappingQuotes(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() >= 2) {
            char first = trimmed.charAt(0);
            char last = trimmed.charAt(trimmed.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return trimmed.substring(1, trimmed.length() - 1);
            }
        }
        return trimmed;
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

    private String collapseWhitespace(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }
}
