package es.rudahee.headmate.server;

import com.mojang.authlib.GameProfile;
import es.rudahee.headmate.HeadMate;
import es.rudahee.headmate.core.AlterIdentity;
import es.rudahee.headmate.core.LegacyFolders;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;

/**
 * Datos de cada alter dentro de un mundo.
 *
 * <pre>
 * &lt;mundo&gt;/headmate/&lt;uuid de la cuenta&gt;/
 *     _legacy/        progreso que había antes de instalar el mod (se hereda una vez)
 *     &lt;id alter&gt;/
 *         player.dat          inventario, ender chest, vida, hambre, XP, efectos, posición...
 *         advancements.json   logros
 *         stats.json          estadísticas
 * </pre>
 *
 * Los datos vanilla (level.dat, playerdata/, advancements/, stats/) no se tocan.
 */
public final class AlterWorldStorage {
    public static final String ROOT_DIR = "headmate";
    public static final String LEGACY_DIR = "_legacy";
    public static final String PLAYER_FILE = "player.dat";
    public static final String ADVANCEMENTS_FILE = "advancements.json";
    public static final String STATS_FILE = "stats.json";

    private AlterWorldStorage() {
    }

    /** {@code <mundo>/headmate}, migrando la carpeta antigua {@code alterselector} si existe. */
    public static Path dataRoot(Path worldDir) {
        return LegacyFolders.migrate(worldDir, ROOT_DIR);
    }

    public static Path accountDir(MinecraftServer server, UUID account) {
        return dataRoot(server.getWorldPath(LevelResource.ROOT)).resolve(account.toString()).normalize();
    }

    public static Path slotDir(MinecraftServer server, UUID account, AlterIdentity identity) {
        return accountDir(server, account).resolve(identity.slot());
    }

    public static Path advancementsFile(MinecraftServer server, UUID account, AlterIdentity identity) {
        return slotDir(server, account, identity).resolve(ADVANCEMENTS_FILE);
    }

    public static Path statsFile(MinecraftServer server, UUID account, AlterIdentity identity) {
        return slotDir(server, account, identity).resolve(STATS_FILE);
    }

    /**
     * Se llama al empezar la sesión de una cuenta, antes de que se lea nada.
     * Guarda una copia del progreso previo al mod y, si toca, se lo pasa al alter.
     */
    static void prepare(MinecraftServer server, GameProfile profile, AlterIdentity identity) {
        UUID account = profile.getId();
        Path accountDir = accountDir(server, account);
        try {
            if (!Files.isDirectory(accountDir)) {
                snapshotLegacy(server, profile, accountDir.resolve(LEGACY_DIR));
            }
            Path slotDir = slotDir(server, account, identity);
            Path legacyDir = accountDir.resolve(LEGACY_DIR);
            if (identity.claimLegacy()
                    && !Files.exists(slotDir.resolve(PLAYER_FILE))
                    && Files.exists(legacyDir.resolve(PLAYER_FILE))) {
                Files.createDirectories(slotDir);
                for (String name : new String[]{PLAYER_FILE, ADVANCEMENTS_FILE, STATS_FILE}) {
                    Path from = legacyDir.resolve(name);
                    if (Files.exists(from)) {
                        Files.move(from, slotDir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
                    }
                }
                HeadMate.LOGGER.info("El alter {} hereda el progreso previo de {}", identity.slot(), profile.getName());
                if (!server.isSingleplayerOwner(profile)) {
                    // En servidores la cuenta sin alter usa playerdata/ vanilla: se aparta para que
                    // el inventario heredado no exista dos veces.
                    Path vanilla = server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(account + ".dat");
                    if (Files.isRegularFile(vanilla)) {
                        Files.move(vanilla, vanilla.resolveSibling(account + ".dat.headmate-claimed"),
                                StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
            Files.createDirectories(slotDir);
        } catch (IOException e) {
            HeadMate.LOGGER.error("No se pudo preparar la carpeta del alter {}", identity.slot(), e);
        }
    }

    private static void snapshotLegacy(MinecraftServer server, GameProfile profile, Path legacyDir) throws IOException {
        Files.createDirectories(legacyDir);
        UUID account = profile.getId();

        // En un jugador los datos del anfitrión viven en level.dat (ya actualizados por el datafixer).
        CompoundTag tag = null;
        if (server.isSingleplayerOwner(profile)) {
            CompoundTag loaded = server.getWorldData().getLoadedPlayerTag();
            if (loaded != null) {
                tag = loaded.copy();
                if (!tag.contains("DataVersion")) {
                    NbtUtils.addCurrentDataVersion(tag);
                }
            }
        }
        if (tag != null) {
            NbtIo.writeCompressed(tag, legacyDir.resolve(PLAYER_FILE));
        } else {
            copyIfExists(server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(account + ".dat"), legacyDir.resolve(PLAYER_FILE));
        }
        copyIfExists(server.getWorldPath(LevelResource.PLAYER_ADVANCEMENTS_DIR).resolve(account + ".json"), legacyDir.resolve(ADVANCEMENTS_FILE));
        copyIfExists(server.getWorldPath(LevelResource.PLAYER_STATS_DIR).resolve(account + ".json"), legacyDir.resolve(STATS_FILE));
    }

    private static void copyIfExists(Path from, Path to) throws IOException {
        if (Files.isRegularFile(from)) {
            Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** Equivalente a {@code PlayerDataStorage.load}, pero en la carpeta del alter. */
    public static Optional<CompoundTag> loadPlayer(MinecraftServer server, Player player, AlterIdentity identity) {
        Path dir = slotDir(server, player.getUUID(), identity);
        CompoundTag tag = read(dir.resolve(PLAYER_FILE));
        if (tag == null) {
            tag = read(dir.resolve(PLAYER_FILE + "_old"));
        }
        if (tag == null) {
            return Optional.empty();
        }
        int version = NbtUtils.getDataVersion(tag, -1);
        CompoundTag fixed = DataFixTypes.PLAYER.updateToCurrentVersion(server.getFixerUpper(), tag, version);
        player.load(fixed);
        return Optional.of(fixed);
    }

    private static @Nullable CompoundTag read(Path file) {
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            return NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
        } catch (Exception e) {
            HeadMate.LOGGER.warn("No se pudo leer {}", file, e);
            return null;
        }
    }

    /** Equivalente a {@code PlayerDataStorage.save}, pero en la carpeta del alter. */
    public static void savePlayer(MinecraftServer server, Player player, AlterIdentity identity) {
        try {
            Path dir = slotDir(server, player.getUUID(), identity);
            Files.createDirectories(dir);
            CompoundTag tag = player.saveWithoutId(new CompoundTag());
            Path temp = Files.createTempFile(dir, "player-", ".dat");
            NbtIo.writeCompressed(tag, temp);
            Util.safeReplaceFile(dir.resolve(PLAYER_FILE), temp, dir.resolve(PLAYER_FILE + "_old"));
        } catch (Exception e) {
            HeadMate.LOGGER.warn("No se pudieron guardar los datos del alter {}", identity.slot(), e);
        }
    }
}
