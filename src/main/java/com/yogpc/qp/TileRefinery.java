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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.yogpc.mc_lib.APowerTile;
import com.yogpc.mc_lib.PacketHandler;
import com.yogpc.mc_lib.YogpstopPacket;
import com.yogpc.qp.bc.RefineryRecipeHelper;

public class TileRefinery extends APowerTile implements IFluidHandler, IEnchantableTile {
  public FluidStack res;
  public final FluidStack[] src = new FluidStack[2];
  public double rem_energy;
  public long rem_time;
  public FluidStack cached;

  public float animationSpeed = 1;
  private int animationStage = 0;

  protected byte unbreaking;
  protected byte fortune;
  protected boolean silktouch;
  protected byte efficiency;

  public int buf;

  @Override
  public void G_reinit() {
    PowerManager.configureR(this, this.efficiency, this.unbreaking);
    this.buf = (int) (FluidContainerRegistry.BUCKET_VOLUME * 4 * Math.pow(1.3, this.fortune));
  }

  @Override
  public void readFromNBT(final NBTTagCompound nbttc) {
    super.readFromNBT(nbttc);
    this.silktouch = nbttc.getBoolean("silktouch");
    this.fortune = nbttc.getByte("fortune");
    this.efficiency = nbttc.getByte("efficiency");
    this.unbreaking = nbttc.getByte("unbreaking");
    final NBTTagList srcl = nbttc.getTagList("src", 10);
    for (int i = 0; i < srcl.tagCount(); ++i) {
      final NBTTagCompound srct = srcl.getCompoundTagAt(i);
      final int j = srct.getByte("Slot") & 255;
      if (j >= 0 && j < this.src.length)
        this.src[j] = FluidStack.loadFluidStackFromNBT(srct);
    }
    this.res = FluidStack.loadFluidStackFromNBT(nbttc.getCompoundTag("res"));
    this.cached = FluidStack.loadFluidStackFromNBT(nbttc.getCompoundTag("cached"));
    this.rem_energy = nbttc.getDouble("rem_energy");
    this.rem_time = nbttc.getLong("rem_time");
    this.animationSpeed = nbttc.getFloat("animationSpeed");
    this.animationStage = nbttc.getInteger("animationStage");
    this.buf = (int) (FluidContainerRegistry.BUCKET_VOLUME * 4 * Math.pow(1.3, this.fortune));
    PowerManager.configureR(this, this.efficiency, this.unbreaking);
  }

  @Override
  public void writeToNBT(final NBTTagCompound nbttc) {
    super.writeToNBT(nbttc);
    nbttc.setBoolean("silktouch", this.silktouch);
    nbttc.setByte("fortune", this.fortune);
    nbttc.setByte("efficiency", this.efficiency);
    nbttc.setByte("unbreaking", this.unbreaking);
    final NBTTagList srcl = new NBTTagList();
    for (int i = 0; i < this.src.length; ++i)
      if (this.src[i] != null) {
        final NBTTagCompound srct = new NBTTagCompound();
        srct.setByte("Slot", (byte) i);
        this.src[i].writeToNBT(srct);
        srcl.appendTag(srct);
      }
    nbttc.setTag("src", srcl);
    if (this.res != null)
      nbttc.setTag("res", this.res.writeToNBT(new NBTTagCompound()));
    if (this.cached != null)
      nbttc.setTag("cached", this.cached.writeToNBT(new NBTTagCompound()));
    nbttc.setDouble("rem_energy", this.rem_energy);
    nbttc.setLong("rem_time", this.rem_time);
    nbttc.setFloat("animationSpeed", this.animationSpeed);
    nbttc.setInteger("animationStage", this.animationStage);
  }

  @Override
  public void updateEntity() {
    super.updateEntity();
    if (this.worldObj.isRemote) {
      simpleAnimationIterate();
      return;
    }
    if (this.worldObj.getWorldTime() % 20 == 7)
      PacketHandler.sendPacketToAround(new YogpstopPacket(this),
          this.worldObj.provider.dimensionId, this.xCoord, this.yCoord, this.zCoord);
    if (this.cached == null) {
      decreaseAnimation();
      return;
    }
    this.rem_time--;
    this.rem_energy -=
        PowerManager.useEnergyR(this, this.rem_energy, this.unbreaking, this.efficiency);
    increaseAnimation();
    if (this.rem_time > 0 || this.rem_energy > 0.01)
      return;
    if (this.res == null)
      this.res = this.cached.copy();
    else
      this.res.amount += this.cached.amount;
    this.cached = null;
    decreaseAnimation();
    RefineryRecipeHelper.get(this);
    return;
  }

  public int getAnimationStage() {
    return this.animationStage;
  }

  private void simpleAnimationIterate() {
    if (this.animationSpeed > 1) {
      this.animationStage += this.animationSpeed;

      if (this.animationStage > 300)
        this.animationStage = 100;
    } else if (this.animationStage > 0)
      this.animationStage--;
  }

  private void sendNowPacket() {
    try {
      final ByteArrayOutputStream bos = new ByteArrayOutputStream();
      final DataOutputStream dos = new DataOutputStream(bos);
      dos.writeFloat(this.animationSpeed);
      PacketHandler.sendPacketToAround(new YogpstopPacket(bos.toByteArray(), this,
          PacketHandler.StC_NOW), this.worldObj.provider.dimensionId, this.xCoord, this.yCoord,
          this.zCoord);
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void increaseAnimation() {
    final float prev = this.animationSpeed;
    if (this.animationSpeed < 2)
      this.animationSpeed = 2;
    else if (this.animationSpeed <= 5)
      this.animationSpeed += 0.1;

    this.animationStage += this.animationSpeed;

    if (this.animationStage > 300)
      this.animationStage = 100;
    if (this.animationSpeed != prev)
      sendNowPacket();
  }

  private void decreaseAnimation() {
    final float prev = this.animationSpeed;
    if (this.animationSpeed >= 1) {
      this.animationSpeed -= 0.1;

      this.animationStage += this.animationSpeed;

      if (this.animationStage > 300)
        this.animationStage = 100;
    } else if (this.animationStage > 0)
      this.animationStage--;
    if (this.animationSpeed != prev)
      sendNowPacket();
  }

  @Override
  protected void S_recievePacket(final byte id, final byte[] data, final EntityPlayer ep) {

  }

  @Override
  protected void C_recievePacket(final byte id, final byte[] data, final EntityPlayer ep) {
    final ByteArrayDataInput badi = ByteStreams.newDataInput(data);
    switch (id) {
      case PacketHandler.StC_NOW:
        this.animationSpeed = badi.readFloat();
        break;
    }
  }

  @Override
  public int fill(final ForgeDirection from, final FluidStack resource, final boolean doFill) {
    for (final FluidStack s : this.src) {
      if (!resource.isFluidEqual(s))
        continue;
      final int ret = Math.min(this.buf - s.amount, resource.amount);
      if (doFill)
        s.amount += ret;
      RefineryRecipeHelper.get(this);
      return ret;
    }
    for (int i = this.src.length - 1; i >= 0; i--) {
      if (this.src[i] != null)
        continue;
      final int ret = Math.min(this.buf, resource.amount);
      if (doFill) {
        this.src[i] = resource.copy();
        this.src[i].amount = ret;
      }
      RefineryRecipeHelper.get(this);
      return ret;
    }
    return 0;
  }

  @Override
  public FluidStack drain(final ForgeDirection from, final FluidStack resource,
      final boolean doDrain) {
    if (resource == null)
      return null;
    if (resource.isFluidEqual(this.res))
      return drain(from, resource.amount, doDrain);
    for (int i = this.src.length - 1; i >= 0; i--) {
      if (!resource.isFluidEqual(this.src[i]))
        continue;
      final FluidStack ret = this.src[i].copy();
      ret.amount = Math.min(resource.amount, ret.amount);
      if (doDrain) {
        this.src[i].amount -= ret.amount;
        if (this.src[i].amount == 0)
          this.src[i] = null;
      }
      RefineryRecipeHelper.get(this);
      return ret;
    }
    return null;
  }

  @Override
  public FluidStack drain(final ForgeDirection from, final int maxDrain, final boolean doDrain) {
    if (this.res == null)
      return null;
    final FluidStack ret = this.res.copy();
    ret.amount = Math.min(maxDrain, ret.amount);
    if (doDrain) {
      this.res.amount -= ret.amount;
      if (this.res.amount == 0)
        this.res = null;
    }
    RefineryRecipeHelper.get(this);
    return ret;
  }

  @Override
  public boolean canFill(final ForgeDirection from, final Fluid fluid) {
    return true;
  }

  @Override
  public boolean canDrain(final ForgeDirection from, final Fluid fluid) {
    return true;
  }

  @Override
  public FluidTankInfo[] getTankInfo(final ForgeDirection from) {
    final FluidTankInfo[] ret = new FluidTankInfo[this.src.length + 1];
    ret[0] = new FluidTankInfo(this.res, this.buf);
    for (int i = this.src.length - 1; i >= 0; i--)
      ret[i + 1] = new FluidTankInfo(this.src[i], this.buf);
    return ret;
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
  public void set(final byte pefficiency, final byte pfortune, final byte punbreaking,
      final boolean psilktouch) {
    this.efficiency = pefficiency;
    this.fortune = pfortune;
    this.unbreaking = punbreaking;
    this.silktouch = psilktouch;
  }
}
