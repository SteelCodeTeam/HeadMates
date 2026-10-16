package es.rudahee.headmate.mixin.client;

import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/** Para registrar la categoría "HeadMates" en la pantalla de controles. */
@Mixin(KeyMapping.class)
public interface KeyMappingAccessor {
    @Accessor("CATEGORY_SORT_ORDER")
    static Map<String, Integer> headmate$categoryOrder() {
        throw new AssertionError();
    }
}
