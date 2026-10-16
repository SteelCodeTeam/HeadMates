package es.rudahee.headmate.fabric;

import es.rudahee.headmate.client.AlterNetClient;
import es.rudahee.headmate.client.HeadMateClient;
import es.rudahee.headmate.net.AlterPayloads;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class HeadMateFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HeadMateClient.init();
        ClientConfigurationNetworking.registerGlobalReceiver(AlterPayloads.Hello.TYPE, (payload, context) ->
                AlterNetClient.onHello(payload.protocol(), context.responseSender()::sendPacket));
        ClientPlayNetworking.registerGlobalReceiver(AlterPayloads.Alters.TYPE, (payload, context) ->
                AlterNetClient.onAlters(payload));
        ClientPlayNetworking.registerGlobalReceiver(AlterPayloads.SkinData.TYPE, (payload, context) ->
                AlterNetClient.onSkinData(payload));
        ClientPlayNetworking.registerGlobalReceiver(AlterPayloads.AlterChat.TYPE, (payload, context) ->
                AlterNetClient.onAlterChat(payload));
    }
}
