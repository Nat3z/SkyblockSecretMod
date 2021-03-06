package com.natia.secretmod.features.dungeons;

import com.natia.secretmod.SecretUtils;
import com.natia.secretmod.config.SecretModConfig;
import com.natia.secretmod.utils.Location;
import com.natia.secretmod.utils.RenderUtils;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.vecmath.Vector3f;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class TerminalHighlight {

    private Minecraft mc = Minecraft.getMinecraft();

    Map<BlockPos, Boolean> highlights = new HashMap<>();
    private boolean startChecks = false;

    // [BOSS] Necron: Finally, I heard so much about you. The Eye likes you very much.
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onMessage(ClientChatReceivedEvent event) {
        if (!SecretModConfig.terminalHighlight) return;

        if (event.message.getUnformattedText().startsWith("[BOSS]")) {
            /* starting phase */
            if (event.message.getUnformattedText().startsWith("[BOSS] Necron: CRAP!! IT BROKE THE FLOOR!")) {
                startChecks = true;
            } else if (event.message.getUnformattedText().startsWith("[CHAT] [BOSS] Necron: LET'S GET TO THE FACTORY'S CORE!")) {
                startChecks = false;
            }
        }
    }

    @SubscribeEvent
    public void onLoad(WorldEvent.Load event) {
        startChecks = false;
    }

    @SubscribeEvent
    public void onBlockRender(DrawBlockHighlightEvent event) {
        if (!SecretModConfig.terminalHighlight) return;
        if (!startChecks) return;

        BlockPos playerPos = new BlockPos(mc.thePlayer.posX, Minecraft.getMinecraft().thePlayer.posY, Minecraft.getMinecraft().thePlayer.posZ);
        new Thread(() -> {
            SecretUtils.getBlocksInBox(mc.theWorld,
                    new BlockPos(playerPos.getX() + 20, playerPos.getY() + 10, playerPos.getZ() + 20),
                    new BlockPos(playerPos.getX() - 20, playerPos.getY() - 10, playerPos.getZ() - 20))
                    .forEach((block, pos) -> {
                        if (block.equals(Blocks.command_block)) {
                            for (EntityArmorStand armorStand : mc.theWorld.getEntitiesWithinAABB(EntityArmorStand.class,
                                    new AxisAlignedBB(pos.getX() + 3, pos.getY() + 1, pos.getZ() + 3,
                                            pos.getX() - 3, pos.getY() - 1, pos.getZ() - 3))) {
                                if (StringUtils.stripControlCodes(armorStand.getName()).contains("Inactive Terminal")) {
                                    highlights.putIfAbsent(pos, true);
                                    break;
                                } else if (StringUtils.stripControlCodes(armorStand.getName()).contains("Terminal Active")) {
                                    highlights.putIfAbsent(pos, false);
                                    break;
                                }
                            }
                        }
                    });
        }).start();

        /* if true, is inactive. if false, is active */
        highlights.forEach((pos, highlightType) -> {
            if (!highlightType)
                RenderUtils.highlightBlock(new Vector3f(pos.getX(), pos.getY(), pos.getZ()), 0.5f, event.partialTicks, new Color(SecretModConfig.terminalHighlightColorACTIVE));
            else
                RenderUtils.highlightBlock(new Vector3f(pos.getX(), pos.getY(), pos.getZ()), 0.5f, event.partialTicks, new Color(SecretModConfig.terminalHighlightColorINACTIVE));
        });
    }

    @SubscribeEvent
    public void worldLoad(WorldEvent.Load event) {
        highlights.clear();
    }
}
