package dk.bjom.sizePotions;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionType;

public class SizePotionRecipes implements Listener {

    public SizePotionRecipes(Plugin plugin) {
        registerRecipes();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void registerRecipes() {
        // Regular potion: thick potion + golden carrot
        ShapelessRecipe recipe = new ShapelessRecipe(SizePotion.RECIPE_KEY, SizePotion.createPotion());
        recipe.addIngredient(new RecipeChoice.MaterialChoice(Material.POTION));
        recipe.addIngredient(Material.GOLDEN_CARROT);
        Bukkit.addRecipe(recipe);

        // Splash potion: normalization potion + gunpowder
        ShapelessRecipe splashRecipe = new ShapelessRecipe(SizePotion.SPLASH_RECIPE_KEY, SizePotion.createSplashPotion());
        splashRecipe.addIngredient(new RecipeChoice.MaterialChoice(Material.POTION));
        splashRecipe.addIngredient(Material.GUNPOWDER);
        Bukkit.addRecipe(splashRecipe);

        // Extended potion: normalization potion + redstone dust
        ShapelessRecipe extendedRecipe = new ShapelessRecipe(SizePotion.EXTENDED_RECIPE_KEY, SizePotion.createExtendedPotion());
        extendedRecipe.addIngredient(new RecipeChoice.MaterialChoice(Material.POTION));
        extendedRecipe.addIngredient(Material.REDSTONE);
        Bukkit.addRecipe(extendedRecipe);

        // Extended splash potion: extended potion + gunpowder
        ShapelessRecipe extendedSplashRecipe = new ShapelessRecipe(SizePotion.EXTENDED_SPLASH_RECIPE_KEY, SizePotion.createExtendedSplashPotion());
        extendedSplashRecipe.addIngredient(new RecipeChoice.MaterialChoice(Material.POTION));
        extendedSplashRecipe.addIngredient(Material.GUNPOWDER);
        Bukkit.addRecipe(extendedSplashRecipe);
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        Recipe recipe = event.getRecipe();
        if (!(recipe instanceof ShapelessRecipe shapeless)) return;

        NamespacedKey key = shapeless.getKey();
        boolean isOurRecipe = key.equals(SizePotion.RECIPE_KEY)
                || key.equals(SizePotion.SPLASH_RECIPE_KEY)
                || key.equals(SizePotion.EXTENDED_RECIPE_KEY)
                || key.equals(SizePotion.EXTENDED_SPLASH_RECIPE_KEY);
        if (!isOurRecipe) return;

        ItemStack potionIngredient = null;
        for (ItemStack item : event.getInventory().getMatrix()) {
            if (item != null && item.getType() == Material.POTION) {
                potionIngredient = item;
                break;
            }
        }

        if (potionIngredient == null) {
            event.getInventory().setResult(null);
            return;
        }

        if (key.equals(SizePotion.RECIPE_KEY)) {
            // Ingredient must be a plain thick potion, not a normalization potion
            if (!isThickPotion(potionIngredient) || SizePotion.isNormalizationPotion(potionIngredient)) {
                event.getInventory().setResult(null);
            }
        } else if (key.equals(SizePotion.EXTENDED_RECIPE_KEY)) {
            // Ingredient must be a (non-extended) normalization potion
            if (!SizePotion.isNormalizationPotion(potionIngredient) || SizePotion.isExtendedPotion(potionIngredient)) {
                event.getInventory().setResult(null);
            }
        } else if (key.equals(SizePotion.SPLASH_RECIPE_KEY) || key.equals(SizePotion.EXTENDED_SPLASH_RECIPE_KEY)) {
            // Both gunpowder recipes share the same ingredient types (POTION + GUNPOWDER), so Bukkit
            // always matches one of them regardless of which potion is in the grid. We resolve this by
            // checking the actual potion and setting the correct result ourselves.
            if (SizePotion.isNormalizationPotion(potionIngredient) && SizePotion.isExtendedPotion(potionIngredient)) {
                event.getInventory().setResult(SizePotion.createExtendedSplashPotion());
            } else if (SizePotion.isNormalizationPotion(potionIngredient)) {
                event.getInventory().setResult(SizePotion.createSplashPotion());
            } else {
                event.getInventory().setResult(null);
            }
        }
    }

    private boolean isThickPotion(ItemStack item) {
        if (!item.hasItemMeta()) return false;
        if (!(item.getItemMeta() instanceof PotionMeta meta)) return false;
        return meta.getBasePotionType() == PotionType.THICK;
    }
}
