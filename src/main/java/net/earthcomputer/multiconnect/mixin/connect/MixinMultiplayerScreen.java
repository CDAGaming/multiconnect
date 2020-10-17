package net.earthcomputer.multiconnect.mixin.connect;

import net.earthcomputer.multiconnect.connect.ConnectionMode;
import net.earthcomputer.multiconnect.connect.ServersExt;
import net.earthcomputer.multiconnect.impl.DropDownWidget;
import net.earthcomputer.multiconnect.impl.Utils;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiplayerScreen.class)
public class MixinMultiplayerScreen extends Screen {
    @Unique private DropDownWidget<ConnectionMode> protocolSelector;
    @Unique private OrderedText forceProtocolLabel;
    @Unique private ServersExt.ServerExt currentGlobalData;

    protected MixinMultiplayerScreen(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void createButtons(CallbackInfo ci) {
        currentGlobalData = ServersExt.getInstance().getOrCreateServer("global");
        forceProtocolLabel = new TranslatableText("multiconnect.changeForcedProtocol").append(" ->").asOrderedText();
        protocolSelector = Utils.createVersionDropdown(this, ConnectionMode.byValue(currentGlobalData.forcedProtocol));
        children.add(0, protocolSelector);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void drawScreen(MatrixStack matrixStack, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        textRenderer.drawWithShadow(matrixStack, forceProtocolLabel, width - 85 - textRenderer.getWidth(forceProtocolLabel), 11, 0xFFFFFF);
        protocolSelector.render(matrixStack, mouseX, mouseY, delta);

        if (protocolSelector.getValue().getValue() != currentGlobalData.forcedProtocol) {
            currentGlobalData.forcedProtocol = protocolSelector.getValue().getValue();
            ServersExt.save();
        }
    }
}
