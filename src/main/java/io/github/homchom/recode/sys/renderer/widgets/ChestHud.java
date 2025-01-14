package io.github.homchom.recode.sys.renderer.widgets;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.homchom.recode.LegacyRecode;
import io.github.homchom.recode.mod.config.Config;
import io.github.homchom.recode.server.state.DF;
import io.github.homchom.recode.server.state.PlotMode;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.lwjgl.opengl.GL11;

import java.util.List;

// TODO remove?
public class ChestHud {
    public static void register() {
        ScreenEvents.AFTER_INIT.register(ChestHud::afterInit);
    }

    private static void afterInit(Minecraft client, Screen screen, int windowWidth, int windowHeight) {
        if (screen instanceof ContainerScreen) {
            ScreenEvents.afterRender(screen).register(ChestHud::afterContainerRender);
        }
    }


    private static void afterContainerRender(Screen screen, PoseStack matrices, int mouseX, int mouseY, float tickDelta) {
        if (DF.isInMode(DF.getCurrentDFState(), PlotMode.Dev) && Config.getBoolean("chestToolTip")) {
            if (Config.getBoolean("chestToolTipType")) {
                ItemStack item = LegacyRecode.MC.player.getInventory().getItem(17);

                int i = ((screen.width) / 2) + 85;
                int j = (screen.height) / 2 - 68;

                // check if block in dev area later.
                if (LegacyRecode.MC.getWindow().getGuiScaledWidth() >= 600) {
                    List<Component> lines = item.getTooltipLines(LegacyRecode.MC.player, TooltipFlag.Default.NORMAL);
                    GL11.glTranslatef(0f, 0f, -1f);
                    screen.renderTooltip(matrices, Lists.transform(lines, Component::getVisualOrderText), i, j);
                    GL11.glTranslatef(0f, 0f, 1f);
                }

            } else {
                ChestMenu handler = ((ContainerScreen) screen).getMenu();
                Minecraft mc = LegacyRecode.MC;
                LocalPlayer player = mc.player;

                Container inventory = player.getInventory();
                ItemStack item = inventory.getItem(17);

                int x = 20;
                int y = 20;

                // check if block in dev area later.
                for (Component text : item.getTooltipLines(player, TooltipFlag.Default.NORMAL)) {
                    y += 10;
                    Minecraft.getInstance().font.draw(matrices, text, x, y, 0x000fff);
                }
            }
        }
    }
}