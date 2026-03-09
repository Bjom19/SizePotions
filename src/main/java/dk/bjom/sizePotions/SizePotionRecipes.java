package dk.bjom.sizePotions;

import io.papermc.paper.potion.PotionMix;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionType;

import java.util.EnumMap;
import java.util.Map;

public class SizePotionRecipes implements Listener {

    private static final Map<PotionType, PotionType> REDSTONE_EXTENSIONS = new EnumMap<>(PotionType.class);

    static {
        REDSTONE_EXTENSIONS.put(PotionType.NIGHT_VISION, PotionType.LONG_NIGHT_VISION);
        REDSTONE_EXTENSIONS.put(PotionType.INVISIBILITY, PotionType.LONG_INVISIBILITY);
        REDSTONE_EXTENSIONS.put(PotionType.LEAPING, PotionType.LONG_LEAPING);
        REDSTONE_EXTENSIONS.put(PotionType.FIRE_RESISTANCE, PotionType.LONG_FIRE_RESISTANCE);
        REDSTONE_EXTENSIONS.put(PotionType.SWIFTNESS, PotionType.LONG_SWIFTNESS);
        REDSTONE_EXTENSIONS.put(PotionType.SLOWNESS, PotionType.LONG_SLOWNESS);
        REDSTONE_EXTENSIONS.put(PotionType.WATER_BREATHING, PotionType.LONG_WATER_BREATHING);
        REDSTONE_EXTENSIONS.put(PotionType.POISON, PotionType.LONG_POISON);
        REDSTONE_EXTENSIONS.put(PotionType.REGENERATION, PotionType.LONG_REGENERATION);
        REDSTONE_EXTENSIONS.put(PotionType.STRENGTH, PotionType.LONG_STRENGTH);
        REDSTONE_EXTENSIONS.put(PotionType.WEAKNESS, PotionType.LONG_WEAKNESS);
        REDSTONE_EXTENSIONS.put(PotionType.SLOW_FALLING, PotionType.LONG_SLOW_FALLING);
        REDSTONE_EXTENSIONS.put(PotionType.TURTLE_MASTER, PotionType.LONG_TURTLE_MASTER);
    }

    public SizePotionRecipes(Plugin plugin) {
        registerPotionMixes(plugin);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void registerPotionMixes(Plugin plugin) {
        RecipeChoice potionChoice = new RecipeChoice.MaterialChoice(Material.POTION);
        RecipeChoice anyPotionChoice = new RecipeChoice.MaterialChoice(
                Material.POTION, Material.SPLASH_POTION, Material.LINGERING_POTION);

        // Recipe 1: Thick Potion/Splash/Lingering + Golden Carrot → Normalization Potion (no vanilla recipe for Thick+GoldenCarrot)
        plugin.getServer().getPotionBrewer().addPotionMix(new PotionMix(
                SizePotion.RECIPE_KEY,
                SizePotion.createPotion(),
                anyPotionChoice,
                new RecipeChoice.MaterialChoice(Material.GOLDEN_CARROT)
        ));

        // Recipe 3: Normalization Potion + Redstone → Extended Normalization Potion (no vanilla Thick+Redstone recipe)
        plugin.getServer().getPotionBrewer().addPotionMix(new PotionMix(
                SizePotion.EXTENDED_RECIPE_KEY,
                SizePotion.createExtendedPotion(),
                potionChoice,
                new RecipeChoice.MaterialChoice(Material.REDSTONE)
        ));
    }

    @EventHandler
    public void onBrew(BrewEvent event) {
        BrewerInventory contents = event.getContents();
        ItemStack ingredient = contents.getIngredient();
        if (ingredient == null) return;

        switch (ingredient.getType()) {
            case GOLDEN_CARROT -> handleGoldenCarrot(event, contents);
            case GUNPOWDER -> handleGunpowder(event, contents);
            case REDSTONE -> handleRedstone(event, contents);
        }
    }

    // Recipe 1: Thick Potion → Normalization Potion
    // Also preserves vanilla Awkward Potion → Night Vision passthrough
    // All other potions: cancelled (null result)
    private void handleGoldenCarrot(BrewEvent event, BrewerInventory contents) {
        for (int i = 0; i < 3; i++) {
            ItemStack input = contents.getItem(i);
            if (input == null) continue;
            Material type = input.getType();
            if (type != Material.POTION && type != Material.SPLASH_POTION && type != Material.LINGERING_POTION) continue;
            if (event.getResults().get(i) == null) continue;

            if (isThickPotion(input) && !SizePotion.isNormalizationPotion(input)) {
                ItemStack result = switch (type) {
                    case SPLASH_POTION -> SizePotion.createSplashPotion();
                    case LINGERING_POTION -> SizePotion.createLingeringPotion();
                    default -> SizePotion.createPotion();
                };
                event.getResults().set(i, result);
            } else if (isAwkwardPotion(input)) {
                event.getResults().set(i, createVanillaPotion(type, PotionType.NIGHT_VISION));
            } else {
                event.getResults().set(i, null);
            }
        }
    }

    // Recipes 2 & 4: vanilla POTION+Gunpowder fires the stand; we override the result for our potions
    // Extended Normalization Potion → Extended Splash Normalization Potion
    // Normalization Potion → Splash Normalization Potion
    // Other potions: leave vanilla splash result untouched
    private void handleGunpowder(BrewEvent event, BrewerInventory contents) {
        for (int i = 0; i < 3; i++) {
            ItemStack input = contents.getItem(i);
            if (input == null || input.getType() != Material.POTION) continue;
            if (event.getResults().get(i) == null) continue;

            if (SizePotion.isExtendedPotion(input)) {
                event.getResults().set(i, SizePotion.createExtendedSplashPotion());
            } else if (SizePotion.isNormalizationPotion(input)) {
                event.getResults().set(i, SizePotion.createSplashPotion());
            }
            // else: vanilla splash result is already correct, leave it
        }
    }

    // Recipe 3: Normalization Potion → Extended Normalization Potion
    // Also preserves vanilla POTION+Redstone extension via lookup table
    // Other potions: cancelled (null result)
    private void handleRedstone(BrewEvent event, BrewerInventory contents) {
        for (int i = 0; i < 3; i++) {
            ItemStack input = contents.getItem(i);
            if (input == null || input.getType() != Material.POTION) continue;
            if (event.getResults().get(i) == null) continue;

            if (SizePotion.isNormalizationPotion(input) && !SizePotion.isExtendedPotion(input)) {
                event.getResults().set(i, SizePotion.createExtendedPotion());
            } else {
                PotionType extended = getExtendedType(input);
                if (extended != null) {
                    event.getResults().set(i, createVanillaPotion(Material.POTION, extended));
                } else {
                    event.getResults().set(i, null);
                }
            }
        }
    }

    private PotionType getExtendedType(ItemStack item) {
        if (!item.hasItemMeta()) return null;
        if (!(item.getItemMeta() instanceof PotionMeta meta)) return null;
        PotionType type = meta.getBasePotionType();
        return type != null ? REDSTONE_EXTENSIONS.get(type) : null;
    }

    private boolean isThickPotion(ItemStack item) {
        if (!item.hasItemMeta()) return false;
        if (!(item.getItemMeta() instanceof PotionMeta meta)) return false;
        return meta.getBasePotionType() == PotionType.THICK;
    }

    private boolean isAwkwardPotion(ItemStack item) {
        if (!item.hasItemMeta()) return false;
        if (!(item.getItemMeta() instanceof PotionMeta meta)) return false;
        return meta.getBasePotionType() == PotionType.AWKWARD;
    }

    private ItemStack createVanillaPotion(Material material, PotionType type) {
        ItemStack potion = new ItemStack(material);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        meta.setBasePotionType(type);
        potion.setItemMeta(meta);
        return potion;
    }
}
