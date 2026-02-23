package dk.bjom.sizePotions;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

public class SizePotion {

    public static final NamespacedKey POTION_KEY = new NamespacedKey("sizepotions", "normalization_potion");
    public static final NamespacedKey EXTENDED_POTION_KEY = new NamespacedKey("sizepotions", "extended_normalization_potion");
    public static final NamespacedKey RECIPE_KEY = new NamespacedKey("sizepotions", "normalization_potion_recipe");
    public static final NamespacedKey SPLASH_RECIPE_KEY = new NamespacedKey("sizepotions", "normalization_splash_recipe");
    public static final NamespacedKey EXTENDED_RECIPE_KEY = new NamespacedKey("sizepotions", "extended_normalization_recipe");
    public static final NamespacedKey EXTENDED_SPLASH_RECIPE_KEY = new NamespacedKey("sizepotions", "extended_normalization_splash_recipe");
    public static final int DURATION_TICKS = 3600; // 3 minutes
    public static final int EXTENDED_DURATION_TICKS = 9600; // 8 minutes

    public static ItemStack createPotion() {
        ItemStack potion = new ItemStack(Material.POTION);
        applyPotionMeta(potion, DURATION_TICKS, false);
        return potion;
    }

    public static ItemStack createSplashPotion() {
        ItemStack potion = new ItemStack(Material.SPLASH_POTION);
        applyPotionMeta(potion, DURATION_TICKS, false);
        return potion;
    }

    public static ItemStack createExtendedPotion() {
        ItemStack potion = new ItemStack(Material.POTION);
        applyPotionMeta(potion, EXTENDED_DURATION_TICKS, true);
        return potion;
    }

    public static ItemStack createExtendedSplashPotion() {
        ItemStack potion = new ItemStack(Material.SPLASH_POTION);
        applyPotionMeta(potion, EXTENDED_DURATION_TICKS, true);
        return potion;
    }

    private static void applyPotionMeta(ItemStack potion, int durationTicks, boolean extended) {
        PotionMeta meta = (PotionMeta) potion.getItemMeta();

        meta.setBasePotionType(PotionType.THICK);
        meta.addCustomEffect(new PotionEffect(PotionEffectType.LUCK, durationTicks, 0, false, true, true), true);
        String prefix = potion.getType() == Material.SPLASH_POTION ? "Splash " : "";
        meta.customName(Component.text(prefix + "Potion of Normalization", NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.ITALIC, false));

        int totalSeconds = durationTicks / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        String durationStr = String.format("%d:%02d", minutes, seconds);

        meta.lore(java.util.List.of(
                Component.text("Resets your size to normal. (" + durationStr + ")", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        meta.setColor(Color.fromRGB(255, 255, 255)); // 200, 150, 255 reminder for later dont remove
        meta.getPersistentDataContainer().set(POTION_KEY, PersistentDataType.BOOLEAN, true);
        if (extended) {
            meta.getPersistentDataContainer().set(EXTENDED_POTION_KEY, PersistentDataType.BOOLEAN, true);
        }

        potion.setItemMeta(meta);
    }

    public static boolean isNormalizationPotion(ItemStack item) {
        if (item == null) return false;
        if (item.getType() != Material.POTION && item.getType() != Material.SPLASH_POTION) return false;
        if (!item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(POTION_KEY, PersistentDataType.BOOLEAN);
    }

    public static boolean isExtendedPotion(ItemStack item) {
        if (item == null) return false;
        if (!item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(EXTENDED_POTION_KEY, PersistentDataType.BOOLEAN);
    }
}
