package es.rudahee.headmate.net;

import es.rudahee.headmate.HeadMate;
import es.rudahee.headmate.core.AlterIdentity;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.network.ConfigurationTask;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Paquetes del mod. Viajan por la red en servidores, y también en un jugador (por el canal
 * en memoria del servidor integrado).
 */
public final class AlterPayloads {
    /** Se sube si cambia el formato de los paquetes; cliente y servidor deben coincidir. */
    public static final int PROTOCOL = 2;

    /** Tarea de la fase de configuración que espera la elección del cliente. */
    public static final ConfigurationTask.Type SELECT_TASK = new ConfigurationTask.Type(HeadMate.MOD_ID + ":select_alter");

    private AlterPayloads() {
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> id(String path) {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(HeadMate.MOD_ID, path));
    }

    // ---------------------------------------------------------------- Identidad en el cable

    static void writeIdentity(FriendlyByteBuf buf, AlterIdentity identity) {
        buf.writeUtf(identity.slot(), 64);
        buf.writeUtf(identity.displayName() == null ? "" : identity.displayName(), 128);
        buf.writeInt(identity.color());
        writeNullable(buf, identity.pronouns());
        writeNullable(buf, identity.systemName());
        buf.writeBoolean(identity.claimLegacy());
    }

    /** Devuelve {@code null} si los datos no son válidos. */
    static @Nullable AlterIdentity readIdentity(FriendlyByteBuf buf) {
        String slot = buf.readUtf(64);
        String name = buf.readUtf(128);
        int color = buf.readInt();
        String pronouns = readNullable(buf);
        String system = readNullable(buf);
        boolean claim = buf.readBoolean();
        return AlterIdentity.sanitized(slot, name, color, pronouns, system, claim);
    }

    private static void writeNullable(FriendlyByteBuf buf, @Nullable String text) {
        buf.writeBoolean(text != null);
        if (text != null) {
            buf.writeUtf(text, 128);
        }
    }

    private static @Nullable String readNullable(FriendlyByteBuf buf) {
        return buf.readBoolean() ? buf.readUtf(128) : null;
    }

    // ---------------------------------------------------------------- Paquetes

    /** Servidor → cliente (configuración): "tengo el mod, ¿quién eres?". */
    public record Hello(int protocol) implements CustomPacketPayload {
        public static final Type<Hello> TYPE = id("hello");
        public static final StreamCodec<FriendlyByteBuf, Hello> CODEC = StreamCodec.of(
                (buf, p) -> buf.writeVarInt(p.protocol), buf -> new Hello(buf.readVarInt()));

        @Override
        public Type<Hello> type() {
            return TYPE;
        }
    }

    /**
     * Cliente → servidor (configuración): alter elegido, o vacío para jugar con la cuenta
     * (no hay sistema creado). {@code valid == false} si llegó algo que no se pudo leer.
     */
    public record Select(int protocol, Optional<AlterIdentity> identity, boolean valid) implements CustomPacketPayload {
        public static final Type<Select> TYPE = id("select");
        public static final StreamCodec<FriendlyByteBuf, Select> CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeVarInt(p.protocol);
                    buf.writeBoolean(p.identity.isPresent());
                    p.identity.ifPresent(identity -> writeIdentity(buf, identity));
                },
                buf -> {
                    int protocol = buf.readVarInt();
                    if (!buf.readBoolean()) {
                        return new Select(protocol, Optional.empty(), true);
                    }
                    AlterIdentity identity = readIdentity(buf);
                    return new Select(protocol, Optional.ofNullable(identity), identity != null);
                });

        public static Select of(@Nullable AlterIdentity identity) {
            return new Select(PROTOCOL, Optional.ofNullable(identity), true);
        }

        @Override
        public Type<Select> type() {
            return TYPE;
        }
    }

    /** Cliente → servidor (juego): la ficha del alter que juega se ha editado. */
    public record Update(@Nullable AlterIdentity identity) implements CustomPacketPayload {
        public static final Type<Update> TYPE = id("update");
        public static final StreamCodec<FriendlyByteBuf, Update> CODEC = StreamCodec.of(
                (buf, p) -> writeIdentity(buf, p.identity), buf -> new Update(readIdentity(buf)));

        @Override
        public Type<Update> type() {
            return TYPE;
        }
    }

    /** Servidor → cliente (juego): qué alter es cada jugador conectado. */
    public record Alters(boolean replaceAll, List<Entry> entries) implements CustomPacketPayload {
        public static final Type<Alters> TYPE = id("alters");
        public static final StreamCodec<FriendlyByteBuf, Alters> CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeBoolean(p.replaceAll);
                    buf.writeVarInt(p.entries.size());
                    for (Entry entry : p.entries) {
                        buf.writeUUID(entry.player);
                        buf.writeBoolean(entry.identity.isPresent());
                        entry.identity.ifPresent(identity -> writeIdentity(buf, identity));
                    }
                },
                buf -> {
                    boolean replace = buf.readBoolean();
                    int count = Math.min(buf.readVarInt(), 4096);
                    List<Entry> entries = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) {
                        UUID player = buf.readUUID();
                        Optional<AlterIdentity> identity = buf.readBoolean() ? Optional.ofNullable(readIdentity(buf)) : Optional.empty();
                        entries.add(new Entry(player, identity));
                    }
                    return new Alters(replace, entries);
                });

        @Override
        public Type<Alters> type() {
            return TYPE;
        }
    }

    /** Tamaño máximo de una skin (un PNG de 64×64 suele ocupar entre 1 y 10 KB). */
    public static final int MAX_SKIN_BYTES = 64 * 1024;

    /** Cliente → servidor (juego): la skin del alter que juega (vacía = usa la de la cuenta). */
    public record SkinUpload(Optional<byte[]> png, boolean slim, boolean hideCape) implements CustomPacketPayload {
        public static final Type<SkinUpload> TYPE = id("skin_upload");
        public static final StreamCodec<FriendlyByteBuf, SkinUpload> CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeBoolean(p.png.isPresent());
                    p.png.ifPresent(buf::writeByteArray);
                    buf.writeBoolean(p.slim);
                    buf.writeBoolean(p.hideCape);
                },
                buf -> new SkinUpload(buf.readBoolean() ? Optional.of(buf.readByteArray(MAX_SKIN_BYTES)) : Optional.empty(),
                        buf.readBoolean(), buf.readBoolean()));

        @Override
        public Type<SkinUpload> type() {
            return TYPE;
        }
    }

    /** Servidor → cliente (juego): la skin de alter de un jugador (vacía = la de su cuenta). */
    public record SkinData(UUID player, Optional<byte[]> png, boolean slim, boolean hideCape) implements CustomPacketPayload {
        public static final Type<SkinData> TYPE = id("skin_data");
        public static final StreamCodec<FriendlyByteBuf, SkinData> CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeUUID(p.player);
                    buf.writeBoolean(p.png.isPresent());
                    p.png.ifPresent(buf::writeByteArray);
                    buf.writeBoolean(p.slim);
                    buf.writeBoolean(p.hideCape);
                },
                buf -> new SkinData(buf.readUUID(), buf.readBoolean() ? Optional.of(buf.readByteArray(MAX_SKIN_BYTES)) : Optional.empty(),
                        buf.readBoolean(), buf.readBoolean()));

        @Override
        public Type<SkinData> type() {
            return TYPE;
        }
    }

    /**
     * Servidor → cliente (juego): mensaje de chat de un alter, con quién lo envió para que el
     * cliente pueda aplicar la protección de alters menores.
     */
    public record AlterChat(UUID sender, Component message) implements CustomPacketPayload {
        public static final Type<AlterChat> TYPE = id("chat");
        public static final StreamCodec<RegistryFriendlyByteBuf, AlterChat> CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, AlterChat::sender,
                ComponentSerialization.TRUSTED_STREAM_CODEC, AlterChat::message,
                AlterChat::new);

        @Override
        public Type<AlterChat> type() {
            return TYPE;
        }
    }

    /** Un jugador y su alter; vacío = ese jugador ya no juega como alter (o se fue). */
    public record Entry(UUID player, Optional<AlterIdentity> identity) {
    }
}
