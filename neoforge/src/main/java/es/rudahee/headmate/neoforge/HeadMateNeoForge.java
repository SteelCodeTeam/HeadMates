package es.rudahee.headmate.neoforge;

import es.rudahee.headmate.HeadMate;
import es.rudahee.headmate.client.AlterNetClient;
import es.rudahee.headmate.net.AlterPayloads;
import es.rudahee.headmate.server.AlterNetServer;
import es.rudahee.headmate.server.AlterSkinsServer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.configuration.ICustomConfigurationTask;
import net.neoforged.neoforge.network.event.RegisterConfigurationTasksEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.function.Consumer;

@Mod(HeadMate.MOD_ID)
public final class HeadMateNeoForge {
    public HeadMateNeoForge(IEventBus modBus) {
        HeadMate.init();
        modBus.addListener(HeadMateNeoForge::registerPayloads);
        modBus.addListener(HeadMateNeoForge::registerTasks);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        // optional(): un cliente con el mod puede entrar a servidores sin él. Que el servidor
        // exija el mod a sus clientes se comprueba en registerTasks.
        PayloadRegistrar registrar = event.registrar(String.valueOf(AlterPayloads.PROTOCOL)).optional();
        registrar.configurationToClient(AlterPayloads.Hello.TYPE, AlterPayloads.Hello.CODEC, (payload, context) ->
                AlterNetClient.onHello(payload.protocol(), context::reply));
        registrar.configurationToServer(AlterPayloads.Select.TYPE, AlterPayloads.Select.CODEC, (payload, context) -> {
            var listener = (ServerConfigurationPacketListenerImpl) context.listener();
            AlterNetServer.onSelect(ServerLifecycleHooks.getCurrentServer(), listener.getOwner(), payload)
                    .ifPresentOrElse(context::disconnect, () -> context.finishCurrentTask(AlterPayloads.SELECT_TASK));
        });
        registrar.playToServer(AlterPayloads.Update.TYPE, AlterPayloads.Update.CODEC, (payload, context) ->
                AlterNetServer.onUpdate((ServerPlayer) context.player(), payload));
        registrar.playToClient(AlterPayloads.Alters.TYPE, AlterPayloads.Alters.CODEC, (payload, context) ->
                AlterNetClient.onAlters(payload));
        registrar.playToServer(AlterPayloads.SkinUpload.TYPE, AlterPayloads.SkinUpload.CODEC, (payload, context) ->
                AlterSkinsServer.onUpload((ServerPlayer) context.player(), payload));
        registrar.playToClient(AlterPayloads.SkinData.TYPE, AlterPayloads.SkinData.CODEC, (payload, context) ->
                AlterNetClient.onSkinData(payload));
        registrar.playToClient(AlterPayloads.AlterChat.TYPE, AlterPayloads.AlterChat.CODEC, (payload, context) ->
                AlterNetClient.onAlterChat(payload));
    }

    private static void registerTasks(RegisterConfigurationTasksEvent event) {
        var listener = event.getListener();
        if (NetworkRegistry.hasChannel(listener, AlterPayloads.Hello.TYPE.id())) {
            event.register(new SelectAlterTask());
        } else {
            // Con el mod en el servidor, el mod es obligatorio en el cliente.
            listener.disconnect(AlterNetServer.missingModMessage());
        }
    }

    /** Pregunta al cliente quién juega y espera la respuesta antes de crear el jugador. */
    private static final class SelectAlterTask implements ICustomConfigurationTask {
        @Override
        public void run(Consumer<CustomPacketPayload> sender) {
            sender.accept(new AlterPayloads.Hello(AlterPayloads.PROTOCOL));
        }

        @Override
        public Type type() {
            return AlterPayloads.SELECT_TASK;
        }
    }
}
