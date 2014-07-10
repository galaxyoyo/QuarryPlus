/*
 * Copyright (C) 2012,2013 yogpstop
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the
 * GNU Lesser General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.yogpc.qp;

import java.util.ArrayList;
import java.util.List;

import com.google.common.io.ByteArrayDataInput;

import cpw.mods.fml.common.network.FMLOutboundHandler;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.FMLOutboundHandler.OutboundTarget;
import cpw.mods.fml.relauncher.Side;
import static buildcraft.BuildCraftCore.actionOn;
import static buildcraft.BuildCraftCore.actionOff;
import buildcraft.api.gates.IAction;
import buildcraft.api.gates.IActionReceptor;
import buildcraft.core.EntityLaser;
import buildcraft.core.IMachine;
import buildcraft.core.triggers.ActionMachineControl;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.ForgeDirection;

public class TileLaser extends APowerTile implements IActionReceptor, IMachine, IEnchantableTile {
	public static class Position {
		public double x, y, z;
		public int l;
	}

	public Position[] lasers;
	private final List<Object> laserTargets = new ArrayList<Object>();
	private ActionMachineControl.Mode lastMode = ActionMachineControl.Mode.Unknown;

	protected byte unbreaking;
	protected byte fortune;
	protected byte efficiency;
	protected boolean silktouch;
	private double pa;

	private long from = 38669;

	public TileLaser() {
		PowerManager.configureL(this, this.efficiency, this.unbreaking);
	}

	@Override
	public void updateEntity() {
		super.updateEntity();
		if (this.worldObj.isRemote) return;

		if (this.lastMode == ActionMachineControl.Mode.Off) {
			removeLaser();
			return;
		}

		if (!isValidTable() && (this.worldObj.getWorldTime() % 100) == (this.from % 100)) {
			findTable();
		}

		if (!isValidTable() || this.getStoredEnergy() == 0) {
			removeLaser();
			return;
		}

		if (!isValidLaser()) {
			for (int i = 0; i < this.lasers.length; i++) {
				this.lasers[i] = new Position();
				this.from = this.worldObj.getWorldTime();
			}
		}

		if (isValidLaser() && (this.worldObj.getWorldTime() % 10) == (this.from % 10)) {
			for (int i = 0; i < this.laserTargets.size(); i++) {
				this.lasers[i].x = ILaserTargetHelper.getXCoord(this.laserTargets.get(i)) + 0.475 + (this.worldObj.rand.nextFloat() - 0.5) / 5F;
				this.lasers[i].y = ILaserTargetHelper.getYCoord(this.laserTargets.get(i)) + 9F / 16F;
				this.lasers[i].z = ILaserTargetHelper.getZCoord(this.laserTargets.get(i)) + 0.475 + (this.worldObj.rand.nextFloat() - 0.5) / 5F;
			}
		}

		double power = PowerManager.useEnergyL(this, this.unbreaking, this.fortune, this.silktouch, this.efficiency);
		for (Object lt : this.laserTargets)
			ILaserTargetHelper.receiveLaserEnergy(lt, (float) (power / this.laserTargets.size()));
		pushPower(power / this.laserTargets.size());
		if ((this.worldObj.getWorldTime() % 20) == 7) {
			PacketHandler.channels.get(Side.SERVER).attr(FMLOutboundHandler.FML_MESSAGETARGET).set(OutboundTarget.ALLAROUNDPOINT);
			PacketHandler.channels.get(Side.SERVER).attr(FMLOutboundHandler.FML_MESSAGETARGETARGS)
					.set(new NetworkRegistry.TargetPoint(this.getWorldObj().provider.dimensionId, this.xCoord, this.yCoord, this.zCoord, 256));
			PacketHandler.channels.get(Side.SERVER).writeOutbound(PacketHandler.getPacketFromNBT(this));
		}
	}

	protected boolean isValidLaser() {
		if (this.lasers == null) return false;
		for (Position laser : this.lasers)
			if (laser == null) return false;
		return true;
	}

	protected boolean isValidTable() {
		if (this.laserTargets.size() == 0) return false;
		for (Object lt : this.laserTargets)
			if (lt == null || !ILaserTargetHelper.isValid(lt)) return false;
		return true;
	}

	protected void findTable() {
		removeLaser();
		int meta = this.worldObj.getBlockMetadata(this.xCoord, this.yCoord, this.zCoord);

		int minX = this.xCoord - 5 * (this.fortune + 1);
		int minY = this.yCoord - 5 * (this.fortune + 1);
		int minZ = this.zCoord - 5 * (this.fortune + 1);
		int maxX = this.xCoord + 5 * (this.fortune + 1);
		int maxY = this.yCoord + 5 * (this.fortune + 1);
		int maxZ = this.zCoord + 5 * (this.fortune + 1);

		switch (ForgeDirection.values()[meta]) {
		case WEST:
			maxX = this.xCoord;
			break;
		case EAST:
			minX = this.xCoord;
			break;
		case DOWN:
			maxY = this.yCoord;
			break;
		case UP:
			minY = this.yCoord;
			break;
		case NORTH:
			maxZ = this.zCoord;
			break;
		default:
		case SOUTH:
			minZ = this.zCoord;
			break;
		}

		this.laserTargets.clear();

		for (int x = minX; x <= maxX; ++x) {
			for (int y = minY; y <= maxY; ++y) {
				for (int z = minZ; z <= maxZ; ++z) {
					TileEntity tile = this.worldObj.getTileEntity(x, y, z);
					if (ILaserTargetHelper.isInstance(tile)) {
						if (ILaserTargetHelper.isValid(tile)) {
							this.laserTargets.add(tile);
						}
					}
				}
			}
		}
		if (this.laserTargets.isEmpty()) return;
		if (!this.silktouch) {
			Object laserTarget = this.laserTargets.get(this.worldObj.rand.nextInt(this.laserTargets.size()));
			this.laserTargets.clear();
			this.laserTargets.add(laserTarget);
		}
		this.lasers = new Position[this.laserTargets.size()];
	}

	protected void removeLaser() {
		if (this.lasers != null) for (int i = 0; i < this.lasers.length; i++)
			this.lasers[i] = null;
		if (!this.worldObj.isRemote) {
			PacketHandler.channels.get(Side.SERVER).attr(FMLOutboundHandler.FML_MESSAGETARGET).set(OutboundTarget.ALLAROUNDPOINT);
			PacketHandler.channels.get(Side.SERVER).attr(FMLOutboundHandler.FML_MESSAGETARGETARGS)
					.set(new NetworkRegistry.TargetPoint(this.getWorldObj().provider.dimensionId, this.xCoord, this.yCoord, this.zCoord, 256));
			PacketHandler.channels.get(Side.SERVER).writeOutbound(PacketHandler.getPacketFromNBT(this));
		}
	}

	private final double[] tp = new double[100];
	private int pi = 0;

	private void pushPower(double received) {
		this.pa -= this.tp[this.pi];
		this.pa += received;
		this.tp[this.pi] = received;
		this.pi++;

		if (this.pi == this.tp.length) {
			this.pi = 0;
		}
	}

	public ResourceLocation getTexture() {
		double avg = this.pa / 100;

		if (avg <= 1.0) {
			return EntityLaser.LASER_TEXTURES[0];
		} else if (avg <= 2.0) {
			return EntityLaser.LASER_TEXTURES[1];
		} else if (avg <= 3.0) {
			return EntityLaser.LASER_TEXTURES[2];
		} else {
			return EntityLaser.LASER_TEXTURES[3];
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbttc) {
		super.readFromNBT(nbttc);
		this.fortune = nbttc.getByte("fortune");
		this.efficiency = nbttc.getByte("efficiency");
		this.unbreaking = nbttc.getByte("unbreaking");
		this.silktouch = nbttc.getBoolean("silktouch");
		PowerManager.configureL(this, this.efficiency, this.unbreaking);
		this.pa = nbttc.getDouble("pa");
		NBTTagList nbttl = nbttc.getTagList("lasers", 10);
		if (this.lasers == null || this.lasers.length != nbttl.tagCount()) this.lasers = new Position[nbttl.tagCount()];
		for (int i = 0; i < nbttl.tagCount(); i++) {
			if (this.lasers[i] == null) this.lasers[i] = new Position();
			NBTTagCompound lc = nbttl.getCompoundTagAt(i);
			this.lasers[i].x = lc.getDouble("x");
			this.lasers[i].y = lc.getDouble("y");
			this.lasers[i].z = lc.getDouble("z");
		}
	}

	@Override
	public void writeToNBT(NBTTagCompound nbttc) {
		super.writeToNBT(nbttc);
		nbttc.setByte("fortune", this.fortune);
		nbttc.setByte("efficiency", this.efficiency);
		nbttc.setByte("unbreaking", this.unbreaking);
		nbttc.setBoolean("silktouch", this.silktouch);
		nbttc.setDouble("pa", this.pa);
		NBTTagList nbttl = new NBTTagList();
		if (this.lasers != null) {
			for (Position l : this.lasers) {
				if (l != null) {
					NBTTagCompound lc = new NBTTagCompound();
					lc.setDouble("x", l.x);
					lc.setDouble("y", l.y);
					lc.setDouble("z", l.z);
					nbttl.appendTag(lc);
				}
			}
		}
		nbttc.setTag("lasers", nbttl);
	}

	@Override
	public void invalidate() {
		super.invalidate();
		removeLaser();
	}

	@Override
	public boolean isActive() {
		return isValidTable();
	}

	@Override
	public boolean manageFluids() {
		return false;
	}

	@Override
	public boolean manageSolids() {
		return false;
	}

	@Override
	public boolean allowAction(IAction action) {
		return action == actionOn || action == actionOff;
	}

	@Override
	public void actionActivated(IAction action) {
		if (action == actionOn) {
			this.lastMode = ActionMachineControl.Mode.On;
		} else if (action == actionOff) {
			this.lastMode = ActionMachineControl.Mode.Off;
		}
	}

	@Override
	public byte getEfficiency() {
		return this.efficiency;
	}

	@Override
	public byte getFortune() {
		return this.fortune;
	}

	@Override
	public byte getUnbreaking() {
		return this.unbreaking;
	}

	@Override
	public boolean getSilktouch() {
		return this.silktouch;
	}

	@Override
	public void set(byte pefficiency, byte pfortune, byte punbreaking, boolean psilktouch) {
		this.efficiency = pefficiency;
		this.fortune = pfortune;
		this.unbreaking = punbreaking;
		this.silktouch = psilktouch;
	}

	@Override
	public void G_reinit() {
		PowerManager.configureL(this, this.efficiency, this.unbreaking);
	}

	@Override
	void S_recievePacket(byte pattern, ByteArrayDataInput data, EntityPlayer ep) {}

	@Override
	void C_recievePacket(byte pattern, ByteArrayDataInput data, EntityPlayer ep) {}
}