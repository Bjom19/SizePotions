package dk.bjom.sizePotions;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class SizePotions extends JavaPlugin {
    private static FileConfiguration config;

    public static final NamespacedKey originalScaleKey = new NamespacedKey("sizepotions", "original_scale");
    public static final NamespacedKey isScaleEffected = new NamespacedKey("sizepotions", "is_scale_effected");

    @Override
    public void onEnable() {
        // Config setup
        saveResource("config.yml", false);
        saveDefaultConfig();
        config = getConfig();

        //// Register event listeners
        getServer().getPluginManager().registerEvents(new EventListener(), this);
        new SizePotionRecipes(this);
        // Commands
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(CommandContainer.getCommand().build());
        });
    }

    @Override
    public void onDisable() {
    }

    public static FileConfiguration getPluginConfig() {
        return config;
    }

    public static double calcPlayerHealthFromScale(double scale) {
        int baseHealth = config.getInt("base_health", 20);
        int healthStep = config.getInt("health_step", 5);
        int minHealth = config.getInt("min_health", 6);

        double health = baseHealth + healthStep * Math.log(scale);
        return Math.floor(Math.max(minHealth, health));
    }
}
