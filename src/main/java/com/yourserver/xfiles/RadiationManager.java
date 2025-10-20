package com.yourserver.xfiles;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class RadiationManager {
    
    private final XFilesPlugin plugin;
    private final ConfigManager config;
    private final Map<UUID, Integer> playerRadiation;
    private final Map<Location, Integer> radiationZones;
    private final Random random;
    
    public RadiationManager(XFilesPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.playerRadiation = new HashMap<>();
        this.radiationZones = new HashMap<>();
        this.random = new Random();
    }
    
    public void exposeToRadiation(Player player, int amount) {
        UUID playerId = player.getUniqueId();
        int current = playerRadiation.getOrDefault(playerId, 0);
        int newLevel = Math.min(current + amount, config.getMaxRadiationLevel());
        playerRadiation.put(playerId, newLevel);
        
        // Визуальные эффекты
        if (newLevel > 0) {
            player.getWorld().spawnParticle(Particle.ASH, player.getLocation(), 10, 1, 1, 1);
        }
    }
    
    public void reduceRadiation(Player player, int amount) {
        UUID playerId = player.getUniqueId();
        int current = playerRadiation.getOrDefault(playerId, 0);
        int newLevel = Math.max(current - amount, 0);
        playerRadiation.put(playerId, newLevel);
    }
    
    public void checkRadiationExposure(Player player, Location location) {
        // Проверка радиационных зон
        for (Map.Entry<Location, Integer> entry : radiationZones.entrySet()) {
            if (location.distance(entry.getKey()) <= config.getContaminationRadius()) {
                exposeToRadiation(player, 1);
            }
        }
    }
    
    public void updateRadiationEffects() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            int level = playerRadiation.getOrDefault(player.getUniqueId(), 0);
            
            if (level > 0) {
                applyRadiationEffects(player, level);
                showRadiationWarning(player, level);
                
                // Постепенное уменьшение радиации
                if (random.nextInt(100) < 10) { // 10% шанс уменьшения
                    reduceRadiation(player, 1);
                }
            }
        }
    }
    
    private void applyRadiationEffects(Player player, int level) {
        List<String> effects = config.getRadiationEffects();
        
        for (String effectName : effects) {
            PotionEffectType effectType = PotionEffectType.getByName(effectName);
            if (effectType != null) {
                int amplifier = level / 25; // Усиление эффекта с уровнем радиации
                player.addPotionEffect(new PotionEffect(effectType, 100, amplifier));
            }
        }
        
        // Дополнительные эффекты при высоком уровне
        if (level > 75) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 1));
        }
        if (level > 90) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0));
        }
    }
    
    private void showRadiationWarning(Player player, int level) {
        String message;
        if (level < 25) {
            message = config.getMessage("radiation.normal");
        } else if (level < 50) {
            message = config.getMessage("radiation.low");
        } else if (level < 75) {
            message = config.getMessage("radiation.medium");
        } else if (level < 90) {
            message = config.getMessage("radiation.high");
        } else {
            message = config.getMessage("radiation.deadly");
        }
        
        message = message.replace("{level}", String.valueOf(level));
        
        // Показываем предупреждение только иногда, чтобы не спамить
        if (random.nextInt(100) < 20) { // 20% шанс показать сообщение
            player.sendMessage(message);
        }
        
        // Звуковое предупреждение при высоком уровне
        if (level > 75 && random.nextInt(100) < 10) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1.0f, 0.5f);
        }
    }
    
    public void createRadiationZone(Location location, int level) {
        radiationZones.put(location, level);
        
        // Автоматическое удаление через 10 минут
        new BukkitRunnable() {
            @Override
            public void run() {
                radiationZones.remove(location);
            }
        }.runTaskLater(plugin, 12000L);
    }
    
    public void cleanup() {
        playerRadiation.clear();
        radiationZones.clear();
    }
    
    public boolean handleCommand(Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Используйте: /radiation <check|cleanup|map>");
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "check":
                return handleCheck(player);
            case "cleanup":
                return handleCleanup(player);
            case "map":
                return handleMap(player);
            default:
                player.sendMessage(ChatColor.RED + "Неизвестная подкоманда");
        }
        
        return true;
    }
    
    private boolean handleCheck(Player player) {
        int level = playerRadiation.getOrDefault(player.getUniqueId(), 0);
        showRadiationWarning(player, level);
        return true;
    }
    
    private boolean handleCleanup(Player player) {
        playerRadiation.put(player.getUniqueId(), 0);
        player.removePotionEffect(PotionEffectType.POISON);
        player.removePotionEffect(PotionEffectType.NAUSEA);
        player.removePotionEffect(PotionEffectType.WITHER);
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        
        player.sendMessage(config.getMessage("radiation.decontaminated"));
        player.getWorld().spawnParticle(Particle.HEART, player.getLocation(), 10);
        player.playSound(player.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 1.0f, 1.0f);
        
        return true;
    }
    
    private boolean handleMap(Player player) {
        int radius = 30;
        Location center = player.getLocation();
        
        player.sendMessage(config.getMessage("radiation.map").replace("{radius}", String.valueOf(radius)));
        
        for (int x = -radius; x <= radius; x += 10) {
            for (int z = -radius; z <= radius; z += 10) {
                Location checkLoc = center.clone().add(x, 0, z);
                int radiationLevel = getRadiationLevelAt(checkLoc);
                
                if (radiationLevel > 0) {
                    ChatColor color = getRadiationColor(radiationLevel);
                    player.sendMessage(color + "☢ " + radiationLevel + " мкЗв " + 
                                     ChatColor.WHITE + "в " + 
                                     checkLoc.getBlockX() + ", " + checkLoc.getBlockZ());
                }
            }
        }
        
        return true;
    }
    
    private int getRadiationLevelAt(Location location) {
        int totalRadiation = 0;
        
        for (Map.Entry<Location, Integer> entry : radiationZones.entrySet()) {
            double distance = location.distance(entry.getKey());
            if (distance <= config.getContaminationRadius()) {
                totalRadiation += entry.getValue();
            }
        }
        
        return Math.min(totalRadiation, config.getMaxRadiationLevel());
    }
    
    private ChatColor getRadiationColor(int level) {
        if (level < 25) return ChatColor.GREEN;
        if (level < 50) return ChatColor.YELLOW;
        if (level < 75) return ChatColor.GOLD;
        if (level < 90) return ChatColor.RED;
        return ChatColor.DARK_RED;
    }
}
