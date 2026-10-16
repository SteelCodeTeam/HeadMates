package es.rudahee.headmate.fabric;

import es.rudahee.headmate.HeadMate;
import es.rudahee.headmate.net.AlterPayloads;
import es.rudahee.headmate.server.AlterNetServer;
import es.rudahee.headmate.server.AlterSkinsServer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class HeadMateFabric implements ModInitializer {
    private static final long ANSWER_TIMEOUT_SECONDS = 10;
    private static final Set<ServerConfigurationPacketListenerImpl> PENDING = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
    private static final ScheduledExecutorService TIMEOUTS = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "headmate-timeouts");
        thread.setDaemon(true);
        return thread;
    });

    @Override
    public void onInitialize() {
        HeadMate.init();

        PayloadTypeRegistry.configurationS2C().register(AlterPayloads.Hello.TYPE, AlterPayloads.Hello.CODEC);
        PayloadTypeRegistry.configurationC2S().register(AlterPayloads.Select.TYPE, AlterPayloads.Select.CODEC);
        PayloadTypeRegistry.playC2S().register(AlterPayloads.Update.TYPE, AlterPayloads.Update.CODEC);
        PayloadTypeRegistry.playS2C().register(AlterPayloads.Alters.TYPE, AlterPayloads.Alters.CODEC);
        PayloadTypeRegistry.playC2S().register(AlterPayloads.SkinUpload.TYPE, AlterPayloads.SkinUpload.CODEC);
        PayloadTypeRegistry.playS2C().register(AlterPayloads.SkinData.TYPE, AlterPayloads.SkinData.CODEC);
        PayloadTypeRegistry.playS2C().register(AlterPayloads.AlterChat.TYPE, AlterPayloads.AlterChat.CODEC);

        // Con el mod en el servidor, el mod es obligatorio en el cliente. Se pregunta siempre:
        // los clientes NeoForge no anuncian sus canales de forma que Fabric los vea, así que
        // no basta con canSend(). Quien no conteste en unos segundos es que no tiene el mod.
        ServerConfigurationConnectionEvents.CONFIGURE.register((handler, server) -> {
            PENDING.add(handler);
            handler.addTask(new SelectAlterTask());
            TIMEOUTS.schedule(() -> server.execute(() -> {
                if (PENDING.remove(handler)) {
                    handler.disconnect(AlterNetServer.missingModMessage());
                }
            }), ANSWER_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        });

        ServerConfigurationNetworking.registerGlobalReceiver(AlterPayloads.Select.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    if (!PENDING.remove(context.networkHandler())) {
                        return;
                    }
                    AlterNetServer.onSelect(context.server(), context.networkHandler().getOwner(), payload)
                            .ifPresentOrElse(context.networkHandler()::disconnect,
                                    () -> context.networkHandler().completeTask(AlterPayloads.SELECT_TASK));
                }));

        ServerPlayNetworking.registerGlobalReceiver(AlterPayloads.Update.TYPE, (payload, context) ->
                AlterNetServer.onUpdate(context.player(), payload));
        ServerPlayNetworking.registerGlobalReceiver(AlterPayloads.SkinUpload.TYPE, (payload, context) ->
                AlterSkinsServer.onUpload(context.player(), payload));
    }

    /** Pregunta al cliente quién juega y espera la respuesta antes de crear el jugador. */
    private static final class SelectAlterTask implements ConfigurationTask {
        @Override
        public void start(Consumer<Packet<?>> sender) {
            sender.accept(ServerConfigurationNetworking.createS2CPacket(new AlterPayloads.Hello(AlterPayloads.PROTOCOL)));
        }

        @Override
        public Type type() {
            return AlterPayloads.SELECT_TASK;
        }
    }
}
