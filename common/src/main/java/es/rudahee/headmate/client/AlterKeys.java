package es.rudahee.headmate.client;

import com.mojang.blaze3d.platform.InputConstants;
import es.rudahee.headmate.client.screen.ContactsScreen;
import es.rudahee.headmate.client.screen.DiaryScreen;
import es.rudahee.headmate.client.screen.SystemScreen;
import es.rudahee.headmate.mixin.client.KeyMappingAccessor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.apache.commons.lang3.ArrayUtils;
import org.lwjgl.glfw.GLFW;

import java.util.Map;

/**
 * Teclas del mod (configurables en Opciones → Controles):
 * K abre la ficha y el sistema, J abre el diario.
 */
public final class AlterKeys {
    public static final String CATEGORY = "key.categories.headmate";
    public static final KeyMapping OPEN_SYSTEM = new KeyMapping("key.headmate.system", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, CATEGORY);
    public static final KeyMapping OPEN_DIARY = new KeyMapping("key.headmate.diary", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, CATEGORY);

    private static boolean pendingContacts;

    private AlterKeys() {
    }

    /** Se llama desde el constructor de Options, antes de leer las teclas guardadas. */
    public static KeyMapping[] register(KeyMapping[] existing) {
        Map<String, Integer> order = KeyMappingAccessor.headmate$categoryOrder();
        if (!order.containsKey(CATEGORY)) {
            order.put(CATEGORY, order.values().stream().mapToInt(Integer::intValue).max().orElse(0) + 1);
        }
        KeyMapping[] result = existing;
        for (KeyMapping mapping : new KeyMapping[]{OPEN_SYSTEM, OPEN_DIARY}) {
            if (!ArrayUtils.contains(result, mapping)) {
                result = ArrayUtils.add(result, mapping);
            }
        }
        return result;
    }

    /** Mostrar la nota de contactos en cuanto se pueda (al entrar a un mundo o servidor). */
    public static void showContactsSoon() {
        pendingContacts = true;
    }

    public static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            pendingContacts = false;
            return;
        }
        if (pendingContacts && mc.screen == null && mc.getOverlay() == null) {
            pendingContacts = false;
            AlterProfile minor = Protection.activeMinor();
            if (minor != null && minor.hasContactsNote()) {
                mc.setScreen(new ContactsScreen(null, minor));
            }
        }
        while (OPEN_SYSTEM.consumeClick()) {
            if (mc.screen == null && ClientState.inWorld()) {
                mc.setScreen(SystemScreen.open(null));
            }
        }
        while (OPEN_DIARY.consumeClick()) {
            if (mc.screen == null && ClientState.inWorld()) {
                mc.setScreen(new DiaryScreen(null));
            }
        }
    }
}
