package com.yourserver.xfiles;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class XFilesPlugin extends JavaPlugin implements Listener {

    private static XFilesPlugin instance;
    private ConfigManager configManager;
    private AnomalyManager anomalyManager;
    private RadiationManager radiationManager;
    private ReactorManager reactorManager;
    private Random random;

    @Override
    public void onEnable() {
        instance = this;
        this.configManager = new ConfigManager(this);
        this.anomalyManager = new AnomalyManager(this);
        this.radiationManager = new RadiationManager(this);
        this.reactorManager = new ReactorManager(this);
        this.random = new Random();

        // Регистрируем события
        getServer().getPluginManager().registerEvents(this, this);
        
        // Запускаем задачи
        startAnomalyScheduler();
        startRadiationCheck();

        getLogger().info("Система аномалий активирована. Обнаружены нестабильные зоны...");
        Bukkit.broadcastMessage(ChatColor.BLUE + "[Секретные Материалы] " + 
                               ChatColor.WHITE + "Система аномалий и радиации активирована!");
    }

    @Override
    public void onDisable() {
        anomalyManager.cleanup();
        radiationManager.cleanup();
        getLogger().info("Система аномалий деактивирована.");
    }

    public static XFilesPlugin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public AnomalyManager getAnomalyManager() {
        return anomalyManager;
    }

    public RadiationManager getRadiationManager() {
        return radiationManager;
    }

    public ReactorManager getReactorManager() {
        return reactorManager;
    }

    // Автоматическое создание аномалий
    private void startAnomalyScheduler() {
        if (!configManager.isAutoAnomaliesEnabled()) return;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (random.nextInt(100) < configManager.getRandomAnomalyChance()) {
                    createRandomAnomaly();
                }
            }
        }.runTaskTimer(this, configManager.getAutoAnomalyInterval(), 
                      configManager.getAutoAnomalyInterval());
    }

    // Проверка радиации
    private void startRadiationCheck() {
        new BukkitRunnable() {
            @Override
            public void run() {
                radiationManager.updateRadiationEffects();
            }
        }.runTaskTimer(this, 100L, 100L); // Каждые 5 секунд
    }

    private void createRandomAnomaly() {
        List<String> types = configManager.getAnomalyTypes();
        String randomType = types.get(random.nextInt(types.size()));
        World world = Bukkit.getWorlds().get(0);
        
        // Случайное место в мире
        Location location = world.getSpawnLocation().clone().add(
            random.nextInt(1000) - 500,
            0,
            random.nextInt(1000) - 500
        );
        
        // Находим поверхность
        location.setY(world.getHighestBlockYAt(location) + 1);
        
        anomalyManager.createAnomaly(randomType, location);
        
        String message = configManager.getMessage("auto-events.anomaly-spawn")
            .replace("{type}", randomType);
        Bukkit.broadcastMessage(message);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location location = player.getLocation();
        
        // Проверка аномалий
        anomalyManager.checkAnomalyEffects(player, location);
        
        // Проверка радиации
        radiationManager.checkRadiationExposure(player, location);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(configManager.getMessage("main.player-only"));
            return true;
        }

        Player player = (Player) sender;
        
        switch (command.getName().toLowerCase()) {
            case "anomaly":
                return anomalyManager.handleCommand(player, args);
            case "reactor":
                return reactorManager.handleCommand(player, args);
            case "radiation":
                return radiationManager.handleCommand(player, args);
            case "xfiles":
                return handleXFilesCommand(player, args);
        }
        
        return false;
    }

    private boolean handleXFilesCommand(Player player, String[] args) {
        if (args.length == 0) {
            showHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "investigate":
                player.sendMessage(ChatColor.BLUE + "[Расследование] " + 
                                 ChatColor.WHITE + "Начато расследование аномальной активности...");
                break;
            case "help":
                showHelp(player);
                break;
            default:
                player.sendMessage(ChatColor.RED + "Неизвестная подкоманда. Используйте /xfiles help");
        }
        return true;
    }

    private void showHelp(Player player) {
        player.sendMessage(ChatColor.BLUE + "=== The X-Files - Система Аномалий ===");
        player.sendMessage(ChatColor.GREEN + "/anomaly create <тип> " + 
                          ChatColor.WHITE + "- Создать аномалию");
        player.sendMessage(ChatColor.GREEN + "/anomaly list " + 
                          ChatColor.WHITE + "- Список активных аномалий");
        player.sendMessage(ChatColor.GREEN + "/reactor start " + 
                          ChatColor.WHITE + "- Запустить ядерный реактор");
        player.sendMessage(ChatColor.RED + "/reactor meltdown " + 
                          ChatColor.WHITE + "- Расплавить реактор (опасно!)");
        player.sendMessage(ChatColor.GREEN + "/radiation check " + 
                          ChatColor.WHITE + "- Проверить уровень радиации");
        player.sendMessage(ChatColor.YELLOW + "Типы аномалий: " + 
                          ChatColor.WHITE + "GRAVITATIONAL, RADIATION, TEMPORAL, etc.");
    }
}
