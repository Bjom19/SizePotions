package dk.bjom.sizePotions;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.kyori.adventure.text.Component;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import static dk.bjom.sizePotions.SizePotions.originalScaleKey;

public class CommandContainer {
    public static LiteralArgumentBuilder<CommandSourceStack> getCommand() {
        FileConfiguration config = SizePotions.getPluginConfig();
        double minScale = config.getDouble("min_scale", 0.1);
        double maxScale = config.getDouble("max_scale", 3.0);

        return Commands.literal("playersize")
                .requires(source -> source.getSender().hasPermission("sizepotions.playersize"))
                .then(Commands.argument("target", ArgumentTypes.player())
                    .then(Commands.argument("scale", DoubleArgumentType.doubleArg(minScale, maxScale))
                            .executes(CommandContainer::runPlayerSizeCommandLogic)));
    }

    private static int runPlayerSizeCommandLogic(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        final PlayerSelectorArgumentResolver targetResolver = ctx.getArgument("target", PlayerSelectorArgumentResolver.class);
        final Player target = targetResolver.resolve(ctx.getSource()).getFirst();

        double scale = DoubleArgumentType.getDouble(ctx, "scale");
        double newHealth = SizePotions.calcPlayerHealthFromScale(scale);

        AttributeInstance scaleAttr = target.getAttribute(Attribute.SCALE);
        if (scaleAttr != null) {
            scaleAttr.setBaseValue(scale);
            target.setHealthScale(newHealth);
            target.getPersistentDataContainer().set(originalScaleKey, PersistentDataType.DOUBLE, scale);

            target.sendMessage(Component.text("Your health scale has been set to " + newHealth));
        }

        return Command.SINGLE_SUCCESS;
    }
}
