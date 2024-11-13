package main.chatmanagerultimate;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;

public final class ChatManagerUltimate extends JavaPlugin implements Listener {

    private Set<String> blockedWords = new HashSet<>();
    private Set<UUID> mutedPlayers = new HashSet<>();
    private HashMap<UUID, BukkitRunnable> muteTasks = new HashMap<>();
    private Set<UUID> commandBlockedPlayers = new HashSet<>();
    private HashMap<UUID, Long> lastMessageTime = new HashMap<>();
    private FileConfiguration config;
    private String prefix;
    private String blockedWordMessage;
    private String mutedMessage;
    private String reloadSuccessMessage;
    private String noPermissionMessage;
    private String chatClearedMessage;
    private String chatDisabledMessage;
    private String chatEnabledMessage;
    private String chatDisabledNotification;
    private String muteSuccessMessage;
    private String muteEndMessage;
    private String cooldownMessage;

    private boolean isChatEnabled = true;
    private int chatCooldownSeconds;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        loadBlockedWords();
        loadMessages();
        Bukkit.getPluginManager().registerEvents(this, this);
        chatCooldownSeconds = config.getInt("chatCooldownSeconds", 5);
        getLogger().info("ChatManagerUltimate włączony!");
    }

    @Override
    public void onDisable() {
        getLogger().info("ChatManagerUltimate wyłączony!");
    }

    private void loadBlockedWords() {
        List<String> wordList = config.getStringList("blockedWords");
        blockedWords = new HashSet<>(wordList);
    }

    private void loadMessages() {
        prefix = colorize(config.getString("messages.prefix", "&b&lChatManager&3&lULTIMATE&7 ×&f"));
        blockedWordMessage = colorize(config.getString("messages.blockedWordMessage", "Ta wiadomość zawiera zablokowane słowo!"));
        mutedMessage = colorize(config.getString("messages.mutedMessage", "Jesteś wyciszony!"));
        reloadSuccessMessage = colorize(config.getString("messages.reloadSuccess", "Config został przeładowany!"));
        noPermissionMessage = colorize(config.getString("messages.noPermission", "Nie masz uprawnień do użycia tej komendy."));
        chatClearedMessage = colorize(config.getString("messages.chatCleared", "Czat został wyczyszczony przez administratora."));
        chatDisabledMessage = colorize(config.getString("messages.chatDisabled", "Czat został wyłączony przez administratora."));
        chatEnabledMessage = colorize(config.getString("messages.chatEnabled", "Czat został ponownie włączony."));
        chatDisabledNotification = colorize(config.getString("messages.chatDisabledNotification", "Czat jest obecnie wyłączony."));
        muteSuccessMessage = colorize(config.getString("messages.muteSuccess", "Gracz %player% został wyciszony na %time%s. Powód: %reason%"));
        muteEndMessage = colorize(config.getString("messages.muteEnd", "Twoje wyciszenie wygasło."));
        cooldownMessage = colorize(config.getString("messages.cooldownMessage", "Poczekaj %time% sekund przed ponownym wysłaniem wiadomości."));
    }

    @EventHandler
    public void onPlayerChat(PlayerChatEvent event) {
        Player player = event.getPlayer();

        if (!isChatEnabled && !player.hasPermission("cmu.bypass")) {
            event.setCancelled(true);
            player.sendMessage(prefix + chatDisabledNotification);
            return;
        }

        if (mutedPlayers.contains(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(prefix + mutedMessage);
            return;
        }

        long currentTime = System.currentTimeMillis();
        long lastTime = lastMessageTime.getOrDefault(player.getUniqueId(), 0L);
        long cooldownMillis = chatCooldownSeconds * 1000L;

        if (currentTime - lastTime < cooldownMillis) {
            long waitTime = (cooldownMillis - (currentTime - lastTime)) / 1000;
            player.sendMessage(prefix + cooldownMessage.replace("%time%", String.valueOf(waitTime)));
            event.setCancelled(true);
            return;
        }

        lastMessageTime.put(player.getUniqueId(), currentTime);

        String message = event.getMessage();
        String strippedMessage = stripColors(message).toLowerCase();

        for (String blockedWord : blockedWords) {
            String strippedBlockedWord = stripColors(blockedWord).toLowerCase();
            if (strippedMessage.contains(strippedBlockedWord)) {
                event.setCancelled(true);
                player.sendMessage(prefix + blockedWordMessage);
                return;
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        mutedPlayers.remove(player.getUniqueId());
        commandBlockedPlayers.remove(player.getUniqueId());
        lastMessageTime.remove(player.getUniqueId());
        BukkitRunnable task = muteTasks.remove(player.getUniqueId());
        if (task != null) task.cancel();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("cmu")) {
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("cmu.reload")) {
                    reloadConfig();
                    config = getConfig();
                    loadBlockedWords();
                    loadMessages();
                    chatCooldownSeconds = config.getInt("chatCooldownSeconds", 5);
                    sender.sendMessage(prefix + reloadSuccessMessage);
                } else {
                    sender.sendMessage(prefix + noPermissionMessage);
                }
                return true;
            }
        }
        return false;
    }

    public static String colorize(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    public static String stripColors(String input) {
        return ChatColor.stripColor(input);
    }
}
