/*
 * Copyright (c) 2012 Jan Dalheimer <jan@dalheimer.de>
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package de.dalheimer.jan.grasseatinganimals;

import net.minecraft.server.v1_4_R1.*;

import java.util.Random;

import org.bukkit.EntityEffect;
import org.bukkit.Location;

/*
 * Max food level is 1000
 * Per tick the food goes down 0-2, per second an average of 20, on average it takes 50s before a cow dies
 * Food levels above 800 can only be reached using wheat, above 600 only using high grass
 * High grass gives 100 food levels, normal grass gives 50
 * Giving milk takes 100 food levels, breeding takes 200 food levels
 * After the food level has reached 0, the cow will take one point of damage each 20 ticks
 */

//TODO make cows try to find grass when hungry
//TODO take minimum level into account for breeding (which function)

public class GrassEatingCow extends EntityCow {

	private double foodLevel = 500; //Initial food level
	private int currentTick = 0;
	private Random rand = new Random();

	static private double maxLevel = 1000;
	static private double maxWithNormalGrass = 600;
	static private double maxWithHighGrass = 800;
	static private double normalGrassFoodLevel = 50;
	static private double highGrassFoodLevel = 100;
	static private double milkingFoodLevel = 100;
	static private double breedingFoodLevel = 200;
	static private double milkingMinFoodLevel = milkingFoodLevel + 100;
	static private double breedingMinFoodLevel = breedingFoodLevel + 100;

	public GrassEatingCow(World world) {
		super(world);

		if(this.getEquipment(0) == null)
			this.setEquipment(0, new ItemStack(0,0,0));
		else
			foodLevel = this.getEquipment(0).getData();
	}

	public void c(){
		double oldFoodLevel = this.foodLevel;

		if(this.foodLevel != 0)
			this.foodLevel -= rand.nextDouble() % 2;
		if(this.foodLevel < 0)
			this.foodLevel = 0;

		int i = MathHelper.floor(this.locX);
		int j = MathHelper.floor(this.locY);
		int k = MathHelper.floor(this.locZ);

		if(rand.nextInt() % (isBaby() ? 30 : 60) == 1){            
			if(this.world.getTypeId(i, j, k) == Block.LONG_GRASS.id && this.foodLevel < maxWithHighGrass){
				//HACK
				//if(!CraftEventFactory.callEntityChangeBlockEvent(this.getBukkitEntity(), this.world.getWorld().getBlockAt(i, j, k), Material.AIR).isCancelled()){
				this.world.triggerEffect(2001, i, j, k, Block.LONG_GRASS.id + 4096);
				this.world.setTypeId(i, j, k, 0);
				this.aH();
				this.foodLevel += highGrassFoodLevel;
				//}
			}
			else if(this.world.getTypeId(i, j - 1, k) == Block.GRASS.id && this.foodLevel < maxWithNormalGrass){
				//HACK
				//if (!CraftEventFactory.callEntityChangeBlockEvent(this.getBukkitEntity(), this.world.getWorld().getBlockAt(i, j - 1, k), Material.EARTH).isCancelled()) {
				this.world.triggerEffect(2001, i, j - 1, k, Block.GRASS.id);
				this.world.setTypeId(i, j - 1, k, Block.DIRT.id);
				this.aH();
				this.foodLevel += normalGrassFoodLevel;
				//}
			}
			else{
				Location closestEatable = findClosestEatable();
				
				//HACK don't teleport!!!
				if(closestEatable.getPitch() != -1)
					this.setLocation(closestEatable.getX(), closestEatable.getY(), closestEatable.getZ(), 0, 0);
			}
		}

		if(this.foodLevel > maxLevel)
			this.foodLevel = maxLevel;

		if((oldFoodLevel != 0 && this.foodLevel == 0) || (oldFoodLevel == 0 && this.foodLevel != 0)){
			this.currentTick = 0;
		}
		this.currentTick++;
		if(this.currentTick == 20){
			if(this.foodLevel == 0){
				this.setHealth(this.getHealth() - 1);
				this.world.broadcastEntityEffect(this, EntityEffect.HURT.getData());
			}
			else
				this.setHealth(this.getHealth() + 1);
			this.currentTick = 0;
			if(this.getHealth() <= 0)
				this.die();
		}

		super.c();
	}

	public boolean a(EntityHuman entityhuman){
		ItemStack itemstack = entityhuman.inventory.getItemInHand();
		if(itemstack != null && itemstack.id == Item.BUCKET.id
				&& foodLevel >= milkingMinFoodLevel && super.a(entityhuman) == true){
			foodLevel -= milkingFoodLevel;
			return true;
		}
		return false;
	}

	public boolean isEatable(int x, int y, int z){
		if((this.world.getTypeId(x, y, z) == Block.LONG_GRASS.id && foodLevel < maxWithHighGrass)
				|| (this.world.getTypeId(x, y -1, z) == Block.GRASS.id && foodLevel < maxWithNormalGrass))
			return true;
		return false;
	}

	public Location findClosestEatable(){
		int currentX = MathHelper.floor(this.locX);
		int currentY = MathHelper.floor(this.locY);
		int currentZ = MathHelper.floor(this.locZ);

		
		//TODO QUESTION: how big should the searched area be
		//TODO Search all accessible places (not only on the current level)
		for(int x = currentX-1; x < currentX+1; x++){
			for(int y = currentY-1; y < currentY+1; y++){
				if(isEatable(x, y, currentZ))
					return new Location(this.world.getWorld(), x, y, currentZ, 0, 0);
			}
		}

		//A pitch of -1 means there is no eatable block
		return new Location(this.world.getWorld(), 0, 0, 0, 0, -1);
	}

	public double getFoodLevel() {
		return foodLevel;
	}

	public void setFoodLevel(double foodLevel) {
		this.foodLevel = foodLevel;
	}
}
