package ru.zhanbyrbay.zhantitles;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.io.File;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ZhanTitles extends JavaPlugin implements CommandExecutor, Listener {

    private org.bukkit.configuration.file.YamlConfiguration data;
    private File dataFile;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        dataFile = new File(getDataFolder(), "players.yml");
        data = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(dataFile);

        getCommand("zhantitles").setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);

        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayer(player);
        }

        getLogger().info("ZhanTitles enabled!");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        updatePlayer(event.getPlayer());
    }

    private String color(String text) {
        Matcher matcher = Pattern.compile("&#([A-Fa-f0-9]{6})").matcher(text);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String hex = matcher.group(1);
            String replacement = "§x";
            for (char c : hex.toCharArray()) {
                replacement += "§" + c;
            }
            matcher.appendReplacement(result, replacement);
        }

        matcher.appendTail(result);
        return ChatColor.translateAlternateColorCodes('&', result.toString());
    }

    private String getTitle(Player player) {
        String key = data.getString(player.getUniqueId().toString());
        if (key == null) return "";
        return getConfig().getString("titles." + key, "");
    }

    private void saveData() {
        try {
            data.save(dataFile);
        } catch (Exception e) {
            getLogger().severe("Could not save players.yml");
        }
    }

    private void updatePlayer(Player player) {
        String title = getTitle(player);
        String prefix = title.isEmpty() ? "" : color(title) + " ";

        player.setDisplayName(prefix + player.getName());
        player.setPlayerListName(prefix + player.getName());

        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = "zt" + player.getUniqueId().toString().substring(0, 8);

        Team team = board.getTeam(teamName);

        if (!title.isEmpty()) {
            if (team == null) {
                team = board.registerNewTeam(teamName);
            }

            team.prefix(LegacyComponentSerializer.legacySection()
                    .deserialize(color(title) + " "));
            team.addEntry(player.getName());
        } else if (team != null) {
            team.removeEntry(player.getName());
            team.unregister();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {

        if (!sender.hasPermission("zhantitles.admin")) {
            sender.sendMessage("§cНет прав!");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§e/zhantitles give <игрок> <титул>");
            sender.sendMessage("§e/zhantitles remove <игрок>");
            sender.sendMessage("§e/zhantitles list");
            sender.sendMessage("§e/zhantitles reload");
            return true;
        }

        if (args[0].equalsIgnoreCase("give") && args.length >= 3) {
            Player target = Bukkit.getPlayerExact(args[1]);

            if (target == null) {
                sender.sendMessage("§cИгрок не найден!");
                return true;
            }

            String key = args[2].toLowerCase();

            if (!getConfig().contains("titles." + key)) {
                sender.sendMessage("§cТакого титула нет!");
                return true;
            }

            data.set(target.getUniqueId().toString(), key);
            saveData();
            updatePlayer(target);

            sender.sendMessage("§aТитул выдан игроку " + target.getName());
            return true;
        }

        if (args[0].equalsIgnoreCase("remove") && args.length >= 2) {
            Player target = Bukkit.getPlayerExact(args[1]);

            if (target == null) {
                sender.sendMessage("§cИгрок не найден!");
                return true;
            }

            data.set(target.getUniqueId().toString(), null);
            saveData();
            updatePlayer(target);

            sender.sendMessage("§aТитул снят!");
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            sender.sendMessage("§6Доступные титулы:");

            if (getConfig().getConfigurationSection("titles") != null) {
                for (String key : getConfig()
                        .getConfigurationSection("titles").getKeys(false)) {
                    sender.sendMessage("§e- " + key);
                }
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            reloadConfig();

            for (Player player : Bukkit.getOnlinePlayers()) {
                updatePlayer(player);
            }

            sender.sendMessage("§aZhanTitles перезагружен!");
            return true;
        }

        return true;
    }
}
