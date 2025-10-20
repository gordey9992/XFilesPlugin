package com.yourserver.xfiles;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class ReactorManager {
    
    private final XFilesPlugin plugin;
    private final ConfigManager config;
    private final RadiationManager radiationManager;
    private final Random random;
    
    private boolean reactorActive = false;
    private int reactorTemperature = 0;
    private int reactorPower = 0;
    private Location reactorLocation = null;
    private BukkitRunnable reactorTask;
    
    public ReactorManager(XFilesPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.radiationManager = plugin.getRadiationManager();
        this.random = new Random();
    }
    
    public void startReactor(Player player) {
        if (reactorActive) {
            player.sendMessage(ChatColor.YELLOW + "[Реактор] Реактор уже запущен");
            return;
        }
        
        reactorActive = true;
        reactorTemperature = 100;
        reactorPower = 50;
        reactorLocation = player.getLocation();
        
        player.sendMessage(config.getMessage("reactor.started").replace("{temp}", String.valueOf(reactorTemperature)));
        
        // Запускаем задачу обновления реактора
        reactorTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!reactorActive) {
                    cancel();
                    return;
                }
                
                updateReactor();
            }
        };
        reactorTask.runTaskTimer(plugin, 100L, 100L); // Каждые 5 секунд
    }
    
    public void stopReactor(Player player) {
        if (!reactorActive) {
            player.sendMessage(ChatColor.YELLOW + "[Реактор] Реактор не активен");
            return;
        }
        
        reactorActive = false;
        reactorTemperature = 0;
        reactorPower = 0;
        
        if (reactorTask != null) {
            reactorTask.cancel();
        }
        
        player.sendMessage(config.getMessage("reactor.stopped"));
        
        // Эффекты остановки
        if (reactorLocation != null) {
            reactorLocation.getWorld().spawnParticle(Particle.CLOUD, reactorLocation, 50);
            reactorLocation.getWorld().playSound(reactorLocation, Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 1.0f);
        }
    }
    
    private void updateReactor() {
        if (!reactorActive) return;
        
        // Случайные колебания температуры
        int tempChange = random.nextInt(20) - 5; // -5 до +15
        reactorTemperature += tempChange;
        
        // Охлаждение при низкой мощности
        if (reactorPower < 30) {
            reactorTemperature -= 3;
        }
        
        // Перегрев при высокой мощности
        if (reactorPower > 70) {
            reactorTemperature += 5;
        }
        
        // Ограничение температуры
        reactorTemperature = Math.max(0, Math.min(reactorTemperature, config.getMaxReactorTemperature()));
        
        // Визуальные эффекты
        if (reactorLocation != null) {
            World world = reactorLocation.getWorld();
            
            // Частицы в зависимости от температуры
            if (reactorTemperature > 300) {
                world.spawnParticle(Particle.LAVA, reactorLocation, 10);
            }
            if (reactorTemperature > 500) {
                world.spawnParticle(Particle.FLAME, reactorLocation, 5);
            }
            if (reactorTemperature > 700) {
                world.spawnParticle(Particle.SOUL_FIRE_FLAME, reactorLocation, 3);
            }
            
            // Звуки
            float pitch = 0.5f + (reactorTemperature / config.getMaxReactorTemperature()) * 0.5f;
            world.playSound(reactorLocation, Sound.BLOCK_FURNACE_FIRE_CRACKLE, 0.5f, pitch);
        }
        
        // Проверка критической температуры
        if (reactorTemperature >= config.getCriticalTemperature()) {
            triggerMeltdownWarning();
        }
        
        // Случайные события
        if (random.nextInt(100) < 5) { // 5% шанс события
            triggerReactorEvent();
        }
    }
    
    private void triggerMeltdownWarning() {
        String message = config.getMessage("reactor.critical").replace("{temp}", String.valueOf(reactorTemperature));
        Bukkit.broadcastMessage(message);
        
        // Звуковое предупреждение
        if (reactorLocation != null) {
            reactorLocation.getWorld().playSound(reactorLocation, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.5f);
        }
        
        // 30% шанс расплавления при критической температуре
        if (random.nextInt(100) < 30) {
            initiateMeltdown();
        }
    }
    
    public void initiateMeltdown() {
        if (!reactorActive) return;
        
        Bukkit.broadcastMessage(config.getMessage("reactor.meltdown"));
        
        // Взрыв
        if (reactorLocation != null) {
            World world = reactorLocation.getWorld();
            
            // Большой взрыв
            world.createExplosion(reactorLocation, config.getMeltdownBlastRadius(), true);
            
            // Огненный шар
            world.spawnParticle(Particle.EXPLOSION, reactorLocation, 50, 3, 3, 3);
            
            // Радиационное заражение
            radiationManager.createRadiationZone(reactorLocation, 80);
            
            // Звуки
            world.playSound(reactorLocation, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
            
            // Остановка реактора
            reactorActive = false;
            if (reactorTask != null) {
                reactorTask.cancel();
            }
            
            // Сообщение о взрыве
            Bukkit.broadcastMessage(config.getMessage("reactor.explosion"));
        }
    }
    
    private void triggerReactorEvent() {
        if (reactorLocation == null) return;
        
        String message = config.getMessage("auto-events.reactor-unstable");
        Bukkit.broadcastMessage(message);
        
        // Визуальные эффекты нестабильности
        World world = reactorLocation.getWorld();
        world.spawnParticle(Particle.ELECTRIC_SPARK, reactorLocation, 20);
        world.playSound(reactorLocation, Sound.BLOCK_CONDUIT_AMBIENT, 1.0f, 2.0f);
        
        // Всплеск радиации
        radiationManager.createRadiationZone(reactorLocation, 20);
    }
    
    public boolean handleCommand(Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Используйте: /reactor <start|stop|meltdown|status>");
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "start":
                startReactor(player);
                break;
            case "stop":
                stopReactor(player);
                break;
            case "meltdown":
                initiateMeltdown();
                player.sendMessage(ChatColor.RED + "[Реактор] Инициировано расплавление!");
                break;
            case "status":
                showStatus(player);
                break;
            default:
                player.sendMessage(ChatColor.RED + "Неизвестная подкоманда");
        }
        
        return true;
    }
    
    private void showStatus(Player player) {
        String status = reactorActive ? 
            (reactorTemperature >= config.getCriticalTemperature() ? "&cКРИТИЧЕСКИЙ" : "&aАктивен") : 
            "&cНеактивен";
        
        String message = config.getMessage("reactor.status")
            .replace("{temp}", String.valueOf(reactorTemperature))
            .replace("{status}", status);
        
        player.sendMessage(message);
        
        if (reactorActive) {
            if (reactorTemperature < 300) {
                player.sendMessage(ChatColor.GREEN + "Температура в норме");
            } else if (reactorTemperature < 500) {
                player.sendMessage(ChatColor.YELLOW + "Температура повышена");
            } else if (reactorTemperature < 700) {
                player.sendMessage(ChatColor.GOLD + "Температура высокая");
            } else {
                player.sendMessage(ChatColor.RED + "КРИТИЧЕСКАЯ ТЕМПЕРАТУРА!");
            }
        }
    }
}
