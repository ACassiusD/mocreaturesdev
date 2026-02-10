/*
 * GNU GENERAL PUBLIC LICENSE Version 3
 */
package drzhark.mocreatures.client;

import drzhark.mocreatures.MoCConstants;
import drzhark.mocreatures.client.gui.MoCGUISettings;
import drzhark.mocreatures.entity.IMoCEntity;
import drzhark.mocreatures.network.MoCMessageHandler;
import drzhark.mocreatures.network.message.MoCMessageEntityDive;
import drzhark.mocreatures.network.message.MoCMessageEntityJump;
import drzhark.mocreatures.proxy.MoCProxyClient;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = MoCConstants.MOD_ID, value = Dist.CLIENT)
public class MoCKeyHandler {

    static KeyBinding diveBinding = new KeyBinding("MoCreatures Dive", 90, "key.categories.movement");
    static KeyBinding settingsBinding = new KeyBinding("key.mocreatures.settings", 79, "key.categories.mocreatures");
    private static boolean wasSettingsKeyDown;

    public MoCKeyHandler() {
        ClientRegistry.registerKeyBinding(diveBinding);
        ClientRegistry.registerKeyBinding(settingsBinding);
    }

    @SubscribeEvent
    public void onInput(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) {
            return;
        }
        if (e.player != null && e.player.world != null && e.player.world.isRemote) {
            if (!wasSettingsKeyDown && settingsBinding.isKeyDown()) {
                MoCProxyClient.mc.displayGuiScreen(new MoCGUISettings());
                wasSettingsKeyDown = true;
            }
            if (!settingsBinding.isKeyDown()) {
                wasSettingsKeyDown = false;
            }
        }

        boolean kbJump = MoCProxyClient.mc.gameSettings.keyBindJump.isKeyDown();
        boolean kbDive = diveBinding.isKeyDown();

        if (kbJump && e.player.getRidingEntity() != null && e.player.getRidingEntity() instanceof IMoCEntity) {
            // jump code needs to be executed client/server simultaneously to take
            ((IMoCEntity) e.player.getRidingEntity()).makeEntityJump();
            MoCMessageHandler.INSTANCE.sendToServer(new MoCMessageEntityJump());
        }

        if (kbDive && e.player.getRidingEntity() != null && e.player.getRidingEntity() instanceof IMoCEntity) {
            // jump code needs to be executed client/server simultaneously to take
            ((IMoCEntity) e.player.getRidingEntity()).makeEntityDive();
            MoCMessageHandler.INSTANCE.sendToServer(new MoCMessageEntityDive());
        }
    }
}
