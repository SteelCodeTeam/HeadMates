package es.rudahee.headmate.server;

import com.mojang.brigadier.CommandDispatcher;
import es.rudahee.headmate.core.AlterIdentity;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

/** {@code /alters}: para operadores, qué cuenta hay detrás de cada alter conectado. */
public final class AltersCommand {
    private AltersCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("alters")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    int count = 0;
                    for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
                        AlterIdentity identity = AlterSessions.get(player.getUUID());
                        MutableComponent line = Component.literal(player.getGameProfile().getName()).withStyle(ChatFormatting.GRAY)
                                .append(Component.literal(" → "));
                        if (identity != null && identity.hasDisplayName()) {
                            line.append(Component.literal(identity.displayName()).withColor(identity.color()));
                            if (identity.hasSystemName()) {
                                line.append(Component.literal(" (" + identity.systemName() + ")").withStyle(ChatFormatting.GRAY));
                            }
                        } else {
                            line.append(Component.translatableWithFallback("headmate.command.account", "own account")
                                    .withStyle(ChatFormatting.DARK_GRAY));
                        }
                        source.sendSuccess(() -> line, false);
                        count++;
                    }
                    if (count == 0) {
                        source.sendSuccess(() -> Component.translatableWithFallback("headmate.command.none", "Nobody is online."), false);
                    }
                    return count;
                }));
    }
}
