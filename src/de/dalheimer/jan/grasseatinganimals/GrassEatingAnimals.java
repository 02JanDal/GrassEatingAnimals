package de.dalheimer.jan.grasseatinganimals;

import java.lang.reflect.Method;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.craftbukkit.v1_4_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_4_R1.entity.CraftEntity;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

import de.dalheimer.jan.grasseatinganimals.GrassEatingCow;

public final class GrassEatingAnimals extends JavaPlugin implements Listener {
	@Override
	public void onEnable(){
		getServer().getPluginManager().registerEvents(this, this);

		try{
			@SuppressWarnings("rawtypes")
			Class[] args = new Class[3];
			args[0] = Class.class;
			args[1] = String.class;
			args[2] = int.class;

			Method a = net.minecraft.server.v1_4_R1.EntityTypes.class.getDeclaredMethod("a", args);
			a.setAccessible(true);

			a.invoke(a, GrassEatingCow.class, "Cow", 92);
		}catch (Exception e){
			e.printStackTrace();
			this.setEnabled(false);
		}
		if(this.isEnabled())
			getLogger().info("Cows eating grass registered");
	}

	@Override
	public void onDisable() {
		
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onCreatureSpawn(CreatureSpawnEvent event){
		if (event.isCancelled()) return;

		Location location = event.getLocation();
		org.bukkit.entity.Entity entity = event.getEntity();
		EntityType creatureType = event.getEntityType();
		World world = location.getWorld();

		net.minecraft.server.v1_4_R1.World mcWorld = ((CraftWorld) world).getHandle();
		net.minecraft.server.v1_4_R1.Entity mcEntity = (((CraftEntity) entity).getHandle());

		if (creatureType == EntityType.COW && mcEntity instanceof GrassEatingCow == false){
			GrassEatingCow grassEatingCow = new GrassEatingCow(mcWorld);

			grassEatingCow.setPosition(location.getX(), location.getY(), location.getZ());

			mcWorld.removeEntity((net.minecraft.server.v1_4_R1.EntityCow) mcEntity);
			mcWorld.addEntity(grassEatingCow, SpawnReason.CUSTOM);

			return;
		}
	}
}
