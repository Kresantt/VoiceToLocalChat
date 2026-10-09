package com.divinitysmp.voicetolocalchat;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Safe integration foundation. Voice decoding and transcription are not implemented. */
public final class VoiceToLocalChat extends JavaPlugin implements CommandExecutor, TabCompleter, Listener {
    private final Set<UUID> optedIn = ConcurrentHashMap.newKeySet();

    @Override public void onEnable() {
        saveDefaultConfig();
        Objects.requireNonNull(getCommand("voicetext")).setExecutor(this);
        Objects.requireNonNull(getCommand("voicetext")).setTabCompleter(this);
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().warning("Voice recognition is NOT implemented in this build. No audio is accessed.");
    }

    @Override public void onDisable() { optedIn.clear(); }

    @EventHandler public void onQuit(PlayerQuitEvent event) {
        // Opt-in is intentionally session-only: explicit permission is required on every login.
        optedIn.remove(event.getPlayer().getUniqueId());
    }

    private boolean masterEnabled() { return getConfig().getBoolean("transcription.enabled", false); }

    /** Returns -1 when local chat is unavailable; never falls back to global broadcasting. */
    public int localRadius() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("SimpleGlobChat");
        if (!(plugin instanceof JavaPlugin chat) || !plugin.isEnabled()) return -1;
        FileConfiguration config = chat.getConfig();
        if (!config.getBoolean("chat.local.toggle", false)) return -1;
        int radius = config.getInt("chat.local.radius", -1);
        return radius > 0 ? radius : -1;
    }

    /** Future transcription delivery must only call this on the server thread. */
    public void deliverLocal(Player speaker, String transcript) {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Local delivery must run on server thread");
        if (!masterEnabled() || !optedIn.contains(speaker.getUniqueId())) return;
        int radius = localRadius();
        if (radius < 1 || transcript == null || transcript.isBlank()) return;
        String safe = transcript.replaceAll("[\\p{Cntrl}\\u00A7]", "").trim();
        if (safe.isEmpty()) return;
        String format = getConfig().getString("local-chat.format", "&a[Voice] &f%player%&7: &f%message%");
        String message = ChatColor.translateAlternateColorCodes('&', format.replace("%player%", speaker.getName()).replace("%message%", safe));
        double distanceSquared = (double) radius * radius;
        for (Player recipient : speaker.getWorld().getPlayers()) {
            if (recipient.getLocation().distanceSquared(speaker.getLocation()) <= distanceSquared) {
                recipient.sendMessage(message);
            }
        }
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Players only."); return true; }
        if (!player.hasPermission("voicetolocalchat.use")) { player.sendMessage("No permission."); return true; }
        String action = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        switch (action) {
            case "on" -> {
                if (!masterEnabled()) player.sendMessage("Voice transcription is disabled by the server administrator.");
                else player.sendMessage("Transcription is not available in this development build; no audio is monitored.");
            }
            case "off" -> {
                optedIn.remove(player.getUniqueId());
                player.sendMessage("Voice transcription is off.");
            }
            case "status" -> player.sendMessage("Voice transcription: unavailable (speech recognition not implemented).");
            default -> player.sendMessage("Usage: /" + label + " <on|off|status|help>");
        }
        return true;
    }

    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return List.of();
        return List.of("on", "off", "status", "help").stream()
            .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
    }
}
