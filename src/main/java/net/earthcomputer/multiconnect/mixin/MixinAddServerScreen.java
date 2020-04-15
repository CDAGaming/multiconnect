package net.earthcomputer.multiconnect.mixin;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.earthcomputer.multiconnect.impl.ConnectionMode;
import net.earthcomputer.multiconnect.impl.ServersExt;
import net.minecraft.client.gui.screen.AddServerScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AddServerScreen.class)
public abstract class MixinAddServerScreen extends Screen {

    @Shadow @Final private ServerData serverData;

    @Unique private ConnectionMode currentProtocol;
    @Unique private Button protocolSelector;

    protected MixinAddServerScreen(ITextComponent title) {
        super(title);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onConstructor(Screen parent, BooleanConsumer callback, ServerData server, CallbackInfo ci) {
        currentProtocol = ConnectionMode.byValue(ServersExt.getInstance().getForcedProtocol(server.serverIP));
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void createButtons(CallbackInfo ci) {
        protocolSelector = new Button(width - 80, 5, 70, 20, currentProtocol.getName(), (buttonWidget_1) ->
                currentProtocol = currentProtocol.next()
        );

        addButton(protocolSelector);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void drawScreen(int mouseX, int mouseY, float delta, CallbackInfo ci) {
        String label = I18n.format("multiconnect.changeForcedProtocol") + " ->";
        font.drawStringWithShadow(label, width - 85 - font.getStringWidth(label), 11, 0xFFFFFF);
        protocolSelector.setMessage(currentProtocol.getName());
    }

    @Inject(method = "onButtonServerAddPressed", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/booleans/BooleanConsumer;accept(Z)V", remap = false))
    private void onAddAndClose(CallbackInfo ci) {
        ServersExt.getInstance().getOrCreateServer(serverData.serverIP).forcedProtocol = currentProtocol.getValue();
    }

}
