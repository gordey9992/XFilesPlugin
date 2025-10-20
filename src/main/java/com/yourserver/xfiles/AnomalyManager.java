package com.yourserver.xfiles;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class AnomalyManager {
    
    private final XFilesPlugin plugin;
    private final ConfigManager config;
    private final Map<String, Anomaly> anomalies;
    private final Random random;
    private int nextId = 1;
    
    public AnomalyManager(XFilesPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.anomalies = new HashMap<>();
        this.random = new Random();
    }
    
    public static class Anomaly {
        public String id;
        public String type;
        public Location location;
        public int radius;
        public long createdTime;
        public long duration;
        
        public Anomaly(String id, String type, Location location, int radius, long duration) {
            this.id = id;
            this.type = type;
            this.location = location;
            this.radius = radius;
            this.createdTime = System.currentTimeMillis();
            this.duration = duration;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - createdTime > duration;
        }
    }
    
    public String createAnomaly(String type, Location location) {
        if (anomalies.size() >= config.getMaxAnomalies()) {
            return null;
        }
        
        String id = "anomaly-" + nextId++;
        int radius = config.getAnomalyEffectRadius();
        long duration = config.getAnomalyDuration() * 50L; // Конвертируем в миллисекунды
        
        Anomaly anomaly = new Anomaly(id, type, location, radius, duration);
        anomalies.put(id, anomaly);
        
        // Визуальные эффекты
        spawnAnomalyEffects(anomaly);
        
        // Автоматическое удаление
        new BukkitRunnable() {
            @Override
            public void run() {
                if (anomalies.containsKey(id)) {
                    removeAnomaly(id);
                }
            }
        }.runTaskLater(plugin, config.getAnomalyDuration());
        
        return id;
    }
    
    private void spawnAnomalyEffects(Anomaly anomaly) {
        Location loc = anomaly.location;
        World world = loc.getWorld();
        
        switch (anomaly.type) {
            case "GRAVITATIONAL":
                world.spawnParticle(Particle.REVERSE_PORTAL, loc, 100, 3, 3, 3);
                world.playSound(loc, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 0.5f);
                break;
            case "RADIATION":
                world.spawnParticle(Particle.ASH, loc, 50, 5, 5, 5);
                world.playSound(loc, Sound.BLOCK_BELL_USE, 1.0f, 0.1f);
                break;
            case "TEMPORAL":
                world.spawnParticle(Particle.ELECTRIC_SPARK, loc, 30, 2, 2, 2);
                world.playSound(loc, Sound.BLOCK_CONDUIT_AMBIENT, 1.0f, 2.0f);
                break;
            case "BIOLOGICAL":
                world.spawnParticle(Particle.HAPPY_VILLAGER, loc, 40, 3, 3, 3);
                world.playSound(loc, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1.0f, 0.5f);
                break;
            case "PSYCHIC":
                world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 30, 2, 2, 2);
                world.playSound(loc, Sound.ENTITY_ENDERMAN_STARE, 1.0f, 0.5f);
                break;
            default:
                world.spawnParticle(Particle.PORTAL, loc, 50, 3, 3, 3);
                world.playSound(loc, Sound.BLOCK_PORTAL_TRIGGER, 1.0f, 1.0f);
        }
    }
    
    public void checkAnomalyEffects(Player player, Location location) {
        for (Anomaly anomaly : anomalies.values()) {
            if (location.distance(anomaly.location) <= anomaly.radius) {
                applyAnomalyEffect(player, anomaly);
            }
        }
    }
    
    private void applyAnomalyEffect(Player player, Anomaly anomaly) {
        switch (anomaly.type) {
            case "GRAVITATIONAL":
                player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 100, 1));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 100, 0));
                break;
            case "RADIATION":
                player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 1));
                player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, 0));
                plugin.getRadiationManager().exposeToRadiation(player, 10);
                break;
            case "TEMPORAL":
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 2));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 2));
                break;
            case "BIOLOGICAL":
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1));
                player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 0));
                break;
            case "PSYCHIC":
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0));
                player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 200, 0));
                break;
            case "ELECTROMAGNETIC":
                player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 100, 0));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 1));
                break;
            case "QUANTUM":
                player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 60, 0));
                player.addPotionEffect(new PotionEffect(PotionEffectType.CONDUIT_POWER, 100, 0));
                break;
        }
        
        // Сообщение игроку
        String message = config.getMessage("anomaly-effects." + anomaly.type.toLowerCase() + ".message");
        player.sendMessage(message);
    }
    
    public boolean removeAnomaly(String id) {
        if (anomalies.containsKey(id)) {
            Anomaly anomaly = anomalies.remove(id);
            // Эффект исчезновения
            anomaly.location.getWorld().spawnParticle(Particle.CLOUD, anomaly.location, 20);
            anomaly.location.getWorld().playSound(anomaly.location, Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 1.0f);
            return true;
        }
        return false;
    }
    
    public void cleanup() {
        for (Anomaly anomaly : anomalies.values()) {
            removeAnomaly(anomaly.id);
        }
        anomalies.clear();
    }
    
    public boolean handleCommand(Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Используйте: /anomaly <create|list|remove|info>");
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "create":
                return handleCreate(player, args);
            case "list":
                return handleList(player);
            case "remove":
                return handleRemove(player, args);
            case "info":
                return handleInfo(player, args);
            default:
                player.sendMessage(ChatColor.RED + "Неизвестная подкоманда");
        }
        
        return true;
    }
    
    private boolean handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Используйте: /anomaly create <тип>");
            player.sendMessage(ChatColor.YELLOW + "Доступные типы: " + 
                             String.join(", ", config.getAnomalyTypes()));
            return true;
        }
        
        String type = args[1].toUpperCase();
        if (!config.getAnomalyTypes().contains(type)) {
            player.sendMessage(ChatColor.RED + "Неизвестный тип аномалии: " + type);
            player.sendMessage(ChatColor.YELLOW + "Доступные типы: " + 
                             String.join(", ", config.getAnomalyTypes()));
            return true;
        }
        
        String id = createAnomaly(type, player.getLocation());
        if (id == null) {
            player.sendMessage(config.getMessage("anomaly.max-reached"));
            return true;
        }
        
        Location loc = player.getLocation();
        String message = config.getMessage("anomaly.created")
            .replace("{type}", type)
            .replace("{x}", String.valueOf(loc.getBlockX()))
            .replace("{y}", String.valueOf(loc.getBlockY()))
            .replace("{z}", String.valueOf(loc.getBlockZ()));
        player.sendMessage(message);
        
        return true;
    }
    
    private boolean handleList(Player player) {
        if (anomalies.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "Активных аномалий нет");
            return true;
        }
        
        player.sendMessage(config.getMessage("anomaly.list"));
        for (Anomaly anomaly : anomalies.values()) {
            Location loc = anomaly.location;
            player.sendMessage(ChatColor.WHITE + "- " + anomaly.id + ": " + 
                             ChatColor.YELLOW + anomaly.type + ChatColor.WHITE + 
                             " в " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
        }
        
        return true;
    }
    
    private boolean handleRemove(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Используйте: /anomaly remove <id>");
            return true;
        }
        
        if (removeAnomaly(args[1])) {
            player.sendMessage(config.getMessage("anomaly.removed"));
        } else {
            player.sendMessage(config.getMessage("anomaly.not-found"));
        }
        
        return true;
    }
    
    private boolean handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Используйте: /anomaly info <id>");
            return true;
        }
        
        Anomaly anomaly = anomalies.get(args[1]);
        if (anomaly == null) {
            player.sendMessage(config.getMessage("anomaly.not-found"));
            return true;
        }
        
        long timeLeft = (anomaly.createdTime + anomaly.duration - System.currentTimeMillis()) / 1000;
        String message = config.getMessage("anomaly.info")
            .replace("{type}", anomaly.type)
            .replace("{radius}", String.valueOf(anomaly.radius))
            .replace("{time}", String.valueOf(timeLeft));
        player.sendMessage(message);
        
        return true;
    }
}
