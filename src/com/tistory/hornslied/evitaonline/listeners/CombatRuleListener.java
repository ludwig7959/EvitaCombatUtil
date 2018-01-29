package com.tistory.hornslied.evitaonline.listeners;

import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.codingforcookies.armorequip.ArmorEquipEvent;
import com.tistory.hornslied.evitaonline.combat.EvitaCombatUtilMain;
import com.tistory.hornslied.evitaonline.utils.Resources;

public class CombatRuleListener implements Listener {

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onHeadShot(EntityDamageByEntityEvent e) {
		Entity damager = e.getDamager();
		Entity damaged = e.getEntity();
		if (damager instanceof Arrow && ((Arrow) damager).getShooter() instanceof Player && damaged instanceof Player) {
			double arrowHeight = damager.getLocation().getY();
			double damagedHeight = damaged.getLocation().getY();
			if (arrowHeight - damagedHeight > 1.55d) {
				e.setDamage(e.getDamage() * 1.5);
				((Player) ((Arrow) damager).getShooter()).sendMessage(Resources.tagCombat + ChatColor.GREEN + "헤드샷!");
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onVehicleEnter(VehicleEnterEvent e) {
		Vehicle vehicle = e.getVehicle();
		Entity entity = e.getEntered();

		if (vehicle instanceof Horse && entity instanceof Player) {
			if (((Horse) vehicle).isTamed()) {
				Player player = ((Player) entity);
				((Horse) vehicle).addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, 4),
						true);
				player.sendMessage(Resources.tagCombat + ChatColor.GOLD + "기사 클래스가 활성화되었습니다.");
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onVehicleExit(VehicleExitEvent e) {
		Vehicle vehicle = e.getVehicle();
		Entity entity = e.getExited();

		if (vehicle instanceof Horse && entity instanceof Player) {
			if (((Horse) vehicle).isTamed()) {
				Player player = ((Player) entity);
				((Horse) vehicle).removePotionEffect(PotionEffectType.REGENERATION);
				player.sendMessage(Resources.tagCombat + ChatColor.GOLD + "기사 클래스가 비활성화되었습니다.");
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onEntityTame(EntityTameEvent e) {
		Entity entity = e.getEntity();
		Player tamer = (Player) e.getOwner();

		if (entity instanceof Horse) {
			((Horse) entity).addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, 4),
					true);
			tamer.sendMessage(Resources.tagCombat + ChatColor.GOLD + "기사 클래스가 활성화되었습니다.");
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onEquip(ArmorEquipEvent e) {
		new BukkitRunnable() {

			@Override
			public void run() {
				int speed = 200 * (100 - (int) e.getPlayer().getAttribute(Attribute.GENERIC_ARMOR).getValue());

				e.getPlayer().setWalkSpeed((float) speed / 100000);
			}
		}.runTaskLater(EvitaCombatUtilMain.getInstance(), 1);
	}
}
