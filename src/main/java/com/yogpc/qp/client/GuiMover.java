/*
 * Copyright (C) 2012,2013 yogpstop This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.yogpc.qp.client;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL11;

import com.yogpc.mc_lib.PacketHandler;
import com.yogpc.mc_lib.YogpstopPacket;
import com.yogpc.qp.ContainerMover;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiMover extends GuiContainer {
  private static final ResourceLocation gui = new ResourceLocation("yogpstop_qp",
      "textures/gui/mover.png");

  public GuiMover(final EntityPlayer player, final World world, final int x, final int y,
      final int z) {
    super(new ContainerMover(player.inventory, world, x, y, z));
  }

  @Override
  public void initGui() {
    super.initGui();
    final int i = this.width - this.xSize >> 1;
    final int j = this.height - this.ySize >> 1;
    this.buttonList.add(new GuiButton(32, i + 27, j + 20, 60, 20, StatCollector
        .translateToLocal("enchantment.digging") + ">"));
    this.buttonList.add(new GuiButton(33, i + 27, j + 50, 60, 20, StatCollector
        .translateToLocal("enchantment.untouching") + ">"));
    this.buttonList.add(new GuiButton(34, i + 89, j + 20, 60, 20, StatCollector
        .translateToLocal("enchantment.durability") + ">"));
    this.buttonList.add(new GuiButton(35, i + 89, j + 50, 60, 20, StatCollector
        .translateToLocal("enchantment.lootBonusDigger") + ">"));
  }

  @Override
  protected void drawGuiContainerForegroundLayer(final int par1, final int par2) {
    this.fontRendererObj.drawString(StatCollector.translateToLocal("tile.EnchantMover.name"), 8, 6,
        0x404040);
    this.fontRendererObj.drawString(StatCollector.translateToLocal("container.inventory"), 8, 72,
        0x404040);
  }

  @Override
  protected void drawGuiContainerBackgroundLayer(final float f, final int i, final int j) {
    GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    this.mc.getTextureManager().bindTexture(gui);
    drawTexturedModalRect(this.width - this.xSize >> 1, this.height - this.ySize >> 1, 0, 0,
        this.xSize, this.ySize);
  }

  @Override
  protected void actionPerformed(final GuiButton par1GuiButton) {
    if (!par1GuiButton.enabled)
      return;
    PacketHandler.sendPacketToServer(new YogpstopPacket(this.inventorySlots,
        new byte[] {(byte) par1GuiButton.id}));
  }

  @Override
  public void updateScreen() {
    super.updateScreen();
    ((GuiButton) this.buttonList.get(0)).enabled =
        (((ContainerMover) this.inventorySlots).avail & 1 << 0) != 0;
    ((GuiButton) this.buttonList.get(1)).enabled =
        (((ContainerMover) this.inventorySlots).avail & 1 << 1) != 0;
    ((GuiButton) this.buttonList.get(2)).enabled =
        (((ContainerMover) this.inventorySlots).avail & 1 << 2) != 0;
    ((GuiButton) this.buttonList.get(3)).enabled =
        (((ContainerMover) this.inventorySlots).avail & 1 << 3) != 0;
  }
}
