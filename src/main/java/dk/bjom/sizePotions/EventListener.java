package dk.bjom.sizePotions;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import static dk.bjom.sizePotions.SizePotions.isScaleEffected;
import static dk.bjom.sizePotions.SizePotions.originalScaleKey;

public class EventListener implements Listener {
    private final FileConfiguration config;

    public EventListener() {
        this.config = SizePotions.getPluginConfig();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        AttributeInstance scaleAttr = player.getAttribute(Attribute.SCALE);
        if (scaleAttr == null) return;
        player.setHealthScale(SizePotions.calcPlayerHealthFromScale(scaleAttr.getValue()));
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.getPersistentDataContainer().has(isScaleEffected, PersistentDataType.BOOLEAN)) {
            restoreOriginalScale(entity);
        }
    }

    @EventHandler
    public void onDrinkPotion(PlayerItemConsumeEvent event) {
        if (!SizePotion.isNormalizationPotion(event.getItem())) return;
        applyNormalization(event.getPlayer(), event.getItem());
    }

    @EventHandler
    public void onSplashPotion(PotionSplashEvent event) {
        ThrownPotion thrownPotion = event.getPotion();
        ItemStack potionItem = thrownPotion.getItem();
        if (!SizePotion.isNormalizationPotion(potionItem)) return;

        for (LivingEntity entity : event.getAffectedEntities()) {
            if (event.getIntensity(entity) <= 0) continue;
            applyNormalization(entity, potionItem);
        }
    }

    private void applyNormalization(LivingEntity entity, ItemStack potionItem) {
        AttributeInstance scaleAttr = entity.getAttribute(Attribute.SCALE);
        if (scaleAttr == null) return;

        double currentScale = scaleAttr.getValue();

        // Save original scale (only if not already under the effect)
        boolean alreadyAffected = entity.getPersistentDataContainer().getOrDefault(isScaleEffected, PersistentDataType.BOOLEAN, false);
        if (!alreadyAffected) {
            entity.getPersistentDataContainer().set(originalScaleKey, PersistentDataType.DOUBLE, currentScale);
        }

        // Determine duration based on extended flag
        int duration = SizePotion.isExtendedPotion(potionItem)
                ? SizePotion.EXTENDED_DURATION_TICKS
                : SizePotion.DURATION_TICKS;

        // Reset to normal size and apply the LUCK effect with the correct duration
        scaleAttr.setBaseValue(1.0);
        entity.getPersistentDataContainer().set(isScaleEffected, PersistentDataType.BOOLEAN, true);

        // Apply LUCK effect — addPotionEffect replaces any existing effect of the same type,
        // firing a CHANGED action (not REMOVED), so the restore listener won't trigger.
        entity.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, duration, 0, false, true, true));

        if (entity instanceof Player player) {
            // Reset health scale to default for players
            player.setHealthScale(config.getInt("base_health", 20));
            player.sendActionBar(Component.text("Your size has been reset to normal temporarily.", NamedTextColor.GREEN));
        }
    }

    @EventHandler
    public void onPotionEffectChange(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        if (event.getModifiedType() != PotionEffectType.LUCK) return;

        // Only act when the LUCK effect is removed (expired, milk, death, etc.)
        if (event.getAction() != EntityPotionEffectEvent.Action.REMOVED
                && event.getAction() != EntityPotionEffectEvent.Action.CLEARED) return;

        // Only restore if the normalization effect was actually active (guards against
        // LUCK from unrelated sources, and prevents a double-restore after death)
        if (!entity.getPersistentDataContainer().has(isScaleEffected, PersistentDataType.BOOLEAN)) return;

        restoreOriginalScale(entity);
        if (entity instanceof Player player) {
            player.sendActionBar(Component.text("Your original size has been restored.", NamedTextColor.YELLOW));
        }
    }

    private void restoreOriginalScale(LivingEntity entity) {
        Double originalScale = entity.getPersistentDataContainer().get(originalScaleKey, PersistentDataType.DOUBLE);
        if (originalScale == null) return;

        AttributeInstance scaleAttr = entity.getAttribute(Attribute.SCALE);
        if (scaleAttr == null) return;

        scaleAttr.setBaseValue(originalScale);
        entity.getPersistentDataContainer().remove(isScaleEffected);
        // Remove originalScaleKey so a subsequent CLEARED event (e.g. death clearing effects)
        // doesn't trigger a second restore
        entity.getPersistentDataContainer().remove(originalScaleKey);

        if (entity instanceof Player player) {
            double newHealth = SizePotions.calcPlayerHealthFromScale(originalScale);
            player.setHealthScale(newHealth);
        }
    }

}
