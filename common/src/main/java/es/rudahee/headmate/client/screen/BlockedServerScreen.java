package es.rudahee.headmate.client.screen;

import es.rudahee.headmate.client.AlterProfile;
import es.rudahee.headmate.client.Protection;
import es.rudahee.headmate.client.SystemStore;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** Un alter menor intenta entrar a un servidor que no está en su lista. */
public final class BlockedServerScreen extends Screen {
    private final Screen parent;
    private final AlterProfile minor;
    private final String address;
    private final Runnable proceed;
    private MultiLineLabel message = MultiLineLabel.EMPTY;

    public BlockedServerScreen(Screen parent, AlterProfile minor, String address, Runnable proceed) {
        super(Component.translatable("headmate.blocked.title"));
        this.parent = parent;
        this.minor = minor;
        this.address = address;
        this.proceed = proceed;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int y = height / 2;
        message = MultiLineLabel.create(font, Component.translatable("headmate.blocked.message",
                Component.literal(minor.name).withColor(minor.color), Protection.normalizeAddress(address)), 300);
        addRenderableWidget(Button.builder(Component.translatable("headmate.blocked.allow_once"),
                        b -> Auth.asAdult(this, adult -> proceed.run()))
                .bounds(cx - 154, y + 10, 150, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("headmate.blocked.add"),
                        b -> Auth.asAdult(this, adult -> {
                            minor.allowedServers().add(Protection.normalizeAddress(address));
                            SystemStore.get().upsert(minor);
                            proceed.run();
                        }))
                .bounds(cx + 4, y + 10, 150, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, b -> onClose())
                .bounds(cx - 75, y + 36, 150, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int cx = width / 2;
        graphics.drawCenteredString(font, title, cx, height / 2 - 60, 0xFFFF55);
        message.renderCentered(graphics, cx, height / 2 - 40);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
