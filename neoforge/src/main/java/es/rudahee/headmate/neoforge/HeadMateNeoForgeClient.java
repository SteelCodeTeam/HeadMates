package es.rudahee.headmate.neoforge;

import es.rudahee.headmate.HeadMate;
import es.rudahee.headmate.client.HeadMateClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

@Mod(value = HeadMate.MOD_ID, dist = Dist.CLIENT)
public final class HeadMateNeoForgeClient {
    public HeadMateNeoForgeClient() {
        HeadMateClient.init();
    }
}
