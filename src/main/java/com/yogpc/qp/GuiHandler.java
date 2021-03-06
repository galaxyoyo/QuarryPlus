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

package com.yogpc.qp;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import com.yogpc.qp.client.GuiEnchList;
import com.yogpc.qp.client.GuiMover;
import com.yogpc.qp.client.GuiP_List;
import com.yogpc.qp.client.GuiPlacer;

import cpw.mods.fml.common.network.IGuiHandler;

public class GuiHandler implements IGuiHandler {
  @Override
  public Object getClientGuiElement(final int ID, final EntityPlayer p, final World w, final int x,
      final int y, final int z) {
    switch (ID) {
      case QuarryPlus.guiIdMover:
        return new GuiMover(p, w, x, y, z);
      case QuarryPlus.guiIdFList:
        return new GuiEnchList((byte) 0, (TileBasic) w.getTileEntity(x, y, z));
      case QuarryPlus.guiIdSList:
        return new GuiEnchList((byte) 1, (TileBasic) w.getTileEntity(x, y, z));
      case QuarryPlus.guiIdPlacer:
        return new GuiPlacer(p.inventory, (TilePlacer) w.getTileEntity(x, y, z));
      case QuarryPlus.guiIdPump:
      case QuarryPlus.guiIdPump + 1:
      case QuarryPlus.guiIdPump + 2:
      case QuarryPlus.guiIdPump + 3:
      case QuarryPlus.guiIdPump + 4:
      case QuarryPlus.guiIdPump + 5:
        return new GuiP_List((byte) (ID - QuarryPlus.guiIdPump),
            (TilePump) w.getTileEntity(x, y, z));
    }

    return null;
  }

  @Override
  public Object getServerGuiElement(final int ID, final EntityPlayer p, final World w, final int x,
      final int y, final int z) {
    switch (ID) {
      case QuarryPlus.guiIdMover:
        return new ContainerMover(p.inventory, w, x, y, z);
      case QuarryPlus.guiIdFList:
      case QuarryPlus.guiIdSList:
        return new ContainerEnchList((TileBasic) w.getTileEntity(x, y, z));
      case QuarryPlus.guiIdPlacer:
        return new ContainerPlacer(p.inventory, (TilePlacer) w.getTileEntity(x, y, z));
    }
    return null;
  }
}
