package dev.fiststyle.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Small settings screen: one On/Off button per feature. */
public class AutoScreen extends Screen {
    public AutoScreen() {
        super(Component.translatable("gui.style.title"));
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height / 2 - 30;

        this.addRenderableWidget(Button.builder(ClientActions.label("gui.style.parry", ClientActions.parryOn()), button -> {
            ClientActions.setParry(!ClientActions.parryOn());
            button.setMessage(ClientActions.label("gui.style.parry", ClientActions.parryOn()));
        }).bounds(cx - 100, y, 200, 20).build());

        this.addRenderableWidget(Button.builder(ClientActions.label("gui.style.dodge", ClientActions.dodgeOn()), button -> {
            ClientActions.setDodge(!ClientActions.dodgeOn());
            button.setMessage(ClientActions.label("gui.style.dodge", ClientActions.dodgeOn()));
        }).bounds(cx - 100, y + 26, 200, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.style.done"), button -> this.onClose())
                .bounds(cx - 100, y + 58, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 60, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
