package com.yourserver.xfiles;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ConfigManager {
    
    private final JavaPlugin plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    private File configFile;
    private File messagesFile;
    
    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfigs();
    }
    
    private void loadConfigs() {
        // Создаем файлы конфигурации, если их нет
        plugin.saveDefaultConfig();
        config = plugin.getConfig();
        
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }
    
    public void reloadConfigs() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }
    
    public String getMessage(String path) {
        return ChatColor.translateAlternateColorCodes('&', 
            messages.getString(path, "&cСообщение не найдено: " + path));
    }
    
    // Основные настройки
    public boolean isAutoAnomaliesEnabled() {
        return config.getBoolean("settings.enable-auto-anomalies", true);
    }
    
    public int getRandomAnomalyChance() {
        return config.getInt("settings.random-anomaly-chance", 20);
    }
    
    public long getAutoAnomalyInterval() {
        return config.getLong("settings.auto-anomaly-interval", 24000L);
    }
    
    // Настройки аномалий
    public int getMaxAnomalies() {
        return config.getInt("anomaly-settings.max-anomalies", 10);
    }
    
    public List<String> getAnomalyTypes() {
        return config.getStringList("anomaly-settings.types");
    }
    
    public int getAnomalyEffectRadius() {
        return config.getInt("anomaly-settings.effect-radius", 15);
    }
    
    public int getAnomalyDuration() {
        return config.getInt("anomaly-settings.duration", 12000);
    }
    
    // Настройки радиации
    public boolean isRadiationEnabled() {
        return config.getBoolean("radiation-settings.enable-radiation", true);
    }
    
    public int getMaxRadiationLevel() {
        return config.getInt("radiation-settings.max-radiation-level", 100);
    }
    
    public int getContaminationRadius() {
        return config.getInt("radiation-settings.contamination-radius", 10);
    }
    
    public List<String> getRadiationEffects() {
        return config.getStringList("radiation-settings.radiation-effects");
    }
    
    // Настройки реактора
    public int getMaxReactorTemperature() {
        return config.getInt("reactor-settings.max-temperature", 1000);
    }
    
    public int getCriticalTemperature() {
        return config.getInt("reactor-settings.critical-temperature", 800);
    }
    
    public int getMeltdownBlastRadius() {
        return config.getInt("reactor-settings.meltdown-blast-radius", 15);
    }
    
    public int getMeltdownRadiationRadius() {
        return config.getInt("reactor-settings.meltdown-radiation-radius", 50);
    }
}
