package es.rudahee.headmate.core;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;

import java.util.Optional;

/**
 * Decide qué alter usa una cuenta al conectarse a un servidor (integrado o dedicado).
 *
 * <p>Ahora solo existe {@link LocalSelection} (un jugador). Para soportar servidores en el
 * futuro basta con otro proveedor que devuelva lo que el cliente mandó en un paquete
 * durante la fase de configuración, antes de que se cree el {@code ServerPlayer}.</p>
 */
@FunctionalInterface
public interface AlterSelectionProvider {
    /** @return el alter elegido, o vacío si este proveedor no sabe nada de esa cuenta */
    Optional<AlterIdentity> resolve(MinecraftServer server, GameProfile profile);
}
