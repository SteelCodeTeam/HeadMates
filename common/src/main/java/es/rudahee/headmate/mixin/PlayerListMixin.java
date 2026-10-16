package es.rudahee.headmate.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import es.rudahee.headmate.core.AlterIdentity;
import es.rudahee.headmate.server.AlterNetServer;
import es.rudahee.headmate.server.AlterSessions;
import es.rudahee.headmate.server.AlterWorldStorage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.PlayerDataStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

/** Redirige los datos del jugador (NBT, logros y estadísticas) a la carpeta del alter. */
@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    @Shadow
    @Final
    private MinecraftServer server;

    @Inject(method = "load", at = @At("HEAD"), cancellable = true)
    private void headmate$load(ServerPlayer player, CallbackInfoReturnable<Optional<CompoundTag>> cir) {
        AlterIdentity identity = AlterSessions.resolve(this.server, player.getGameProfile());
        if (identity != null) {
            cir.setReturnValue(AlterWorldStorage.loadPlayer(this.server, player, identity));
        }
    }

    @WrapOperation(method = "save", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/storage/PlayerDataStorage;save(Lnet/minecraft/world/entity/player/Player;)V"))
    private void headmate$save(PlayerDataStorage storage, Player player, Operation<Void> original) {
        AlterIdentity identity = AlterSessions.get(player.getUUID());
        if (identity != null) {
            AlterWorldStorage.savePlayer(this.server, player, identity);
        } else {
            original.call(storage, player);
        }
    }

    @ModifyExpressionValue(method = "getPlayerStats", at = @At(value = "NEW",
            target = "(Ljava/io/File;Ljava/lang/String;)Ljava/io/File;", ordinal = 0))
    private File headmate$statsFile(File original, @Local(argsOnly = true) Player player) {
        AlterIdentity identity = AlterSessions.resolve(this.server, player.getGameProfile());
        return identity == null ? original : AlterWorldStorage.statsFile(this.server, player.getUUID(), identity).toFile();
    }

    @ModifyExpressionValue(method = "getPlayerAdvancements", at = @At(value = "INVOKE",
            target = "Ljava/nio/file/Path;resolve(Ljava/lang/String;)Ljava/nio/file/Path;"))
    private Path headmate$advancementsFile(Path original, @Local(argsOnly = true) ServerPlayer player) {
        AlterIdentity identity = AlterSessions.resolve(this.server, player.getGameProfile());
        return identity == null ? original : AlterWorldStorage.advancementsFile(this.server, player.getUUID(), identity);
    }

    @Inject(method = "remove", at = @At("TAIL"))
    private void headmate$endSession(ServerPlayer player, CallbackInfo ci) {
        boolean hadAlter = AlterSessions.get(player.getUUID()) != null;
        AlterSessions.end(player.getUUID());
        if (hadAlter) {
            AlterNetServer.onLeave(this.server, player.getUUID());
        }
    }

    @Inject(method = "placeNewPlayer", at = @At("TAIL"))
    private void headmate$syncOnJoin(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
        AlterNetServer.onJoin(this.server, player);
    }
}
