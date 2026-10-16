package es.rudahee.headmate.mixin;

import com.mojang.brigadier.CommandDispatcher;
import es.rudahee.headmate.server.AltersCommand;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Registra /alters sin depender de la API de comandos de cada loader. */
@Mixin(Commands.class)
public abstract class CommandsMixin {
    @Shadow
    @Final
    private CommandDispatcher<CommandSourceStack> dispatcher;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void headmate$register(Commands.CommandSelection selection, CommandBuildContext context, CallbackInfo ci) {
        AltersCommand.register(this.dispatcher);
    }
}
