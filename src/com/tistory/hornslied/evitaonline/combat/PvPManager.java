package com.tistory.hornslied.evitaonline.combat;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;

import com.palmergames.bukkit.towny.event.TownyTeleportEvent;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.tistory.hornslied.evitaonline.core.EvitaCoreMain;
import com.tistory.hornslied.evitaonline.events.SpawnEvent;
import com.tistory.hornslied.evitaonline.utils.Resources;

public class PvPManager implements Listener {
	private volatile static PvPManager instance;
	private final EvitaCombatUtilMain evitaCombat;

	private HashMap<Player, Integer> epCooldown;
	private HashMap<Player, Integer> combatTag;
	private HashMap<Player, Integer> pvpProt;
	private HashMap<Player, Integer> tpDelay;
	private HashMap<Player, BukkitRunnable> epCounter;
	private HashMap<Player, BukkitRunnable> combatCounter;
	private HashMap<Player, BukkitRunnable> pvpCounter;
	private HashMap<Player, BukkitRunnable> tpCounter;

	private HashMap<Player, BukkitRunnable> messageCounter;

	private PvPManager() {
		evitaCombat = EvitaCombatUtilMain.getInstance();
		epCooldown = new HashMap<>();
		combatTag = new HashMap<>();
		pvpProt = new HashMap<>();
		tpDelay = new HashMap<>();
		epCounter = new HashMap<>();
		combatCounter = new HashMap<>();
		pvpCounter = new HashMap<>();
		tpCounter = new HashMap<>();

		messageCounter = new HashMap<>();

		Bukkit.getPluginManager().registerEvents(this, evitaCombat);
	}

	public static PvPManager getInstance() {
		if (instance == null) {
			synchronized (PvPManager.class) {
				if (instance == null) {
					instance = new PvPManager();
				}
			}
		}

		return instance;
	};

	public int getEpCooldown(Player player) {
		return epCooldown.get(player);
	}

	public int getCombatTag(Player player) {
		return combatTag.get(player);
	}

	public int getPvPProt(Player player) {
		return pvpProt.get(player);
	}

	public int getTpDelay(Player player) {
		return tpDelay.get(player);
	}
	
	public void cancelTpDelay(Player player) {
		tpCounter.get(player).cancel();
		tpCounter.remove(player);
		tpDelay.remove(player);
		player.sendMessage(Resources.tagMove + ChatColor.RED + "텔레포트가 취소되었습니다.");
	}

	public void initEpCounter(Player player) {
		epCooldown.put(player, 15);
		BukkitRunnable counter = new BukkitRunnable() {
			@Override
			public void run() {
				if (epCooldown.get(player) == 0) {
					epCooldown.remove(player);
					player.sendMessage(Resources.tagCombat + ChatColor.GREEN + "이제 엔더 진주 사용이 가능합니다.");
					epCounter.remove(player);
					cancel();
				} else {
					epCooldown.put(player, epCooldown.get(player) - 1);
				}
			}
		};
		epCounter.put(player, counter);
		counter.runTaskTimer(evitaCombat, 20, 20);
	}

	public void initCombatCounter(Player player) {
		combatTag.put(player, 15);
		BukkitRunnable counter = new BukkitRunnable() {
			@Override
			public void run() {
				if (combatTag.get(player) == 0) {
					combatTag.remove(player);
					player.sendMessage(Resources.tagCombat + ChatColor.GREEN + "당신은 전투상태에서 벗어났습니다!");
					combatCounter.remove(player);
					cancel();
				} else {
					combatTag.put(player, combatTag.get(player) - 1);
				}
			}
		};
		combatCounter.put(player, counter);
		counter.runTaskTimer(evitaCombat, 20, 20);
	}

	public void initPvPProt(Player player, int time) {
		pvpProt.put(player, time);
		BukkitRunnable counter = new BukkitRunnable() {
			@Override
			public void run() {
				if (pvpProt.get(player) == 0) {
					player.sendMessage(Resources.tagCombat + ChatColor.GREEN + "PvP 보호가 종료되었습니다!");

					pvpProt.remove(player);
					pvpCounter.remove(player);
					cancel();
				} else {
					WorldCoord coord = WorldCoord.parseWorldCoord(player.getLocation());

					try {
						if (!coord.hasTownBlock() || !coord.getTownBlock().hasTown()
								|| !coord.getTownBlock().getTown().isAC())
							pvpProt.put(player, pvpProt.get(player) - 1);
					} catch (NotRegisteredException e) {
						pvpProt.put(player, pvpProt.get(player) - 1);
					}
				}
			}
		};
		pvpCounter.put(player, counter);
		counter.runTaskTimer(evitaCombat, 20, 20);
	}

	public void expirePvPProt(Player player) {
		pvpCounter.get(player).cancel();
		pvpCounter.remove(player);
		pvpProt.remove(player);
	}

	public void initTpCounter(Player player, Location loc) {
		int sec = (int) (player.getLocation().distance(loc) / 100);

		if (sec > 5) {
			tpDelay.put(player, sec);
		} else {
			tpDelay.put(player, 5);
		}

		BukkitRunnable counter = new BukkitRunnable() {
			@Override
			public void run() {
				if (tpDelay.get(player) == 0) {
					tpDelay.remove(player);
					player.sendMessage(Resources.tagMove + ChatColor.GOLD + "텔레포트 중...");
					player.teleport(loc, TeleportCause.PLUGIN);
					tpCounter.remove(player);
					cancel();
				} else {
					tpDelay.put(player, tpDelay.get(player) - 1);
				}
			}
		};
		tpCounter.put(player, counter);
		counter.runTaskTimer(evitaCombat, 20, 20);
		player.sendMessage(
				Resources.tagMove + ChatColor.GOLD + tpDelay.get(player) + "초 후 텔레포트 됩니다. 움직이거나 데미지를 받으면 안됩니다.");
	}

	public boolean isPvPProt(Player player) {
		return pvpProt.containsKey(player);
	}

	public boolean isEpCoolDown(Player player) {
		return epCooldown.containsKey(player);
	}

	public boolean isCombatTag(Player player) {
		return combatTag.containsKey(player);
	}

	public boolean isTpDelay(Player player) {
		return tpDelay.containsKey(player);
	}

	private void dropLoot(Player player) {
		ItemStack[] mainContents = player.getInventory().getContents();
		ItemStack[] extraContents = player.getInventory().getExtraContents();

		if (mainContents != null) {
			for (int i = 0; i < mainContents.length; i++) {
				if (mainContents[i] != null) {
					Random rd = new Random();

					if (rd.nextInt(5) == 4) {
						player.getWorld().dropItem(player.getLocation(), mainContents[i]);
						mainContents[i] = null;
					}
				}
			}
		}

		if (extraContents != null) {
			for (int i = 0; i < extraContents.length; i++) {
				if (extraContents[i] != null) {
					Random rd = new Random();

					if (rd.nextInt(5) == 4) {
						player.getWorld().dropItem(player.getLocation(), extraContents[i]);
						extraContents[i] = null;
					}
				}
			}
		}

		if (mainContents != null)
			player.getInventory().setContents(mainContents);
		if (extraContents != null)
			player.getInventory().setExtraContents(extraContents);
	}

	public void savePvPProt(Player player) {
		if (pvpProt.containsKey(player)) {
			pvpCounter.get(player).cancel();
			pvpCounter.remove(player);

			EvitaCoreMain.getInstance().getDB().query("UPDATE playerinfo SET pvpprot = " + pvpProt.get(player)
					+ " WHERE uuid = '" + player.getUniqueId() + "';");
		}
	}

	// Listeners

	@EventHandler
	public void onTownyTeleport(TownyTeleportEvent e) {
		Player player = e.getPlayer();

		if (!player.hasPermission("evita.tp.bypass")) {
			if (!e.getTo().getWorld().equals(player.getWorld())) {
				e.setCancelled(true);
				player.sendMessage(Resources.tagMove + ChatColor.RED + "다른 월드에서는 텔레포트 할수 없습니다. 중앙부에 스폰 표지판을 타서 본 월드로 돌아가 주세요.");
			} else if (combatTag.containsKey(player)) {
				e.setCancelled(true);
				player.sendMessage(Resources.tagMove + ChatColor.RED + "전투 상태에서는 텔레포트할수 없습니다!");
			} else if (tpDelay.containsKey(player)) {
				e.setCancelled(true);
				player.sendMessage(Resources.tagMove + ChatColor.RED + "이미 텔레포트 대기 중입니다.");
			} else {
				Town town;
				TownBlock from = TownyUniverse.getTownBlock(e.getFrom());

				if (from != null) {
					try {
						town = TownyUniverse.getTownBlock(e.getFrom()).getTown();
					} catch (NotRegisteredException e1) {
						town = null;
					}
				} else {
					town = null;
				}

				if (town == null || !town.isAC()) {
					e.setCancelled(true);

					initTpCounter(player, e.getTo());
				}
			}
		}
	}

	@EventHandler
	public void onSpawn(SpawnEvent e) {
		Player player = e.getPlayer();
		Location spawn = new Location(Bukkit.getWorld("Evita"), 935, 72, -795);

		if (player.hasPermission("evita.tp.bypass")) {
			player.teleport(spawn);
		} else {
			if (combatTag.containsKey(player)) {
				player.sendMessage(Resources.tagMove + ChatColor.RED + "전투 상태에서는 텔레포트할수 없습니다!");
			} else if (tpDelay.containsKey(player)) {
				player.sendMessage(Resources.tagMove + ChatColor.RED + "이미 텔레포트 대기 중입니다.");
			} else {
				if (!EvitaCoreMain.getInstance().getEconomy().has(player, 3000)) {
					player.sendMessage(Resources.tagMove + ChatColor.RED + "고대 도시 이동 비용으로 3000페론이 필요합니다.");
					return;
				}

				tpDelay.put(player, 20);

				BukkitRunnable counter = new BukkitRunnable() {
					@Override
					public void run() {
						if (tpDelay.get(player) == 0) {
							if (EvitaCoreMain.getInstance().getEconomy().has(player, 3000)) {
								EvitaCoreMain.getInstance().getEconomy().withdrawPlayer(player, 3000);
								tpDelay.remove(player);
								player.sendMessage(Resources.tagMove + ChatColor.GOLD + "텔레포트 중...");
								player.teleport(spawn, TeleportCause.PLUGIN);
								tpCounter.remove(player);
								cancel();
							} else {
								player.sendMessage(Resources.tagMove + ChatColor.RED + "고대 도시 이동 비용으로 3000페론이 필요합니다.");
								tpDelay.remove(player);
								tpCounter.remove(player);
								cancel();
							}
						} else {
							tpDelay.put(player, tpDelay.get(player) - 1);
						}
					}
				};
				tpCounter.put(player, counter);
				counter.runTaskTimer(evitaCombat, 20, 20);
				player.sendMessage(Resources.tagMove + ChatColor.GOLD + tpDelay.get(player)
						+ "초 후 텔레포트 됩니다. 움직이거나 데미지를 받으면 안됩니다.");
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onAttackPvPProt(EntityDamageByEntityEvent e) {
		Entity damager = e.getDamager();
		Entity damaged = e.getEntity();

		if (damager instanceof Player && damaged instanceof Player && !(damager.equals(damaged))) {

			if (pvpProt.containsKey(damaged)) {
				e.setCancelled(true);
				damager.sendMessage(Resources.tagCombat + ChatColor.RED + "PvP 보호가 걸린 유저입니다.");
				return;
			}

			if (pvpProt.containsKey(damager)) {
				e.setCancelled(true);
				damager.sendMessage(Resources.tagCombat + ChatColor.RED + "PvP 보호중에는 타인을 공격할 수 없습니다.");
				return;
			}
		} else if (damager instanceof Projectile && ((Projectile) damager).getShooter() instanceof Player
				&& damaged instanceof Player && !((Projectile) damager).getShooter().equals(damaged)) {
			Player shooter = ((Player) ((Projectile) damager).getShooter());

			if (pvpProt.containsKey(damaged)) {
				e.setCancelled(true);
				shooter.sendMessage(Resources.tagCombat + ChatColor.RED + "PvP 보호가 걸린 유저입니다.");
				return;
			}

			if (pvpProt.containsKey(shooter)) {
				e.setCancelled(true);
				shooter.sendMessage(Resources.tagCombat + ChatColor.RED + "PvP 보호중에는 타인을 공격할 수 없습니다.");
				return;
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerInteract(PlayerInteractEvent e) {
		Player player = e.getPlayer();

		if (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) {
			if (player.getInventory().getItemInMainHand().getType().equals(Material.ENDER_PEARL)) {

				if (epCooldown.containsKey(player)) {
					if (!(messageCounter.containsKey(player))) {
						player.sendMessage(Resources.tagCombat + "엔더 진주 재사용 대기시간: " + ChatColor.GREEN
								+ epCooldown.get(player) + "초");
						messageCounter.put(player, new BukkitRunnable() {
							public void run() {
								messageCounter.remove(player);
								cancel();
							}
						});

						messageCounter.get(player).runTaskLater(evitaCombat, 20);
					}
					e.setCancelled(true);
				} else {
					initEpCounter(player);
				}

			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void setCombatTag(EntityDamageByEntityEvent e) {
		Entity damager = e.getDamager();
		Entity damaged = e.getEntity();
		if (damager instanceof Player && damaged instanceof Player && !(damager.equals(damaged))) {
			if (!damager.hasMetadata("NPC")) {
				if (combatTag.containsKey(damager)) {
					combatTag.put((Player) damager, 15);
				} else {
					damager.sendMessage(Resources.tagCombat + ChatColor.RED + "전투상태에 돌입합니다!");
					initCombatCounter((Player) damager);
				}
			}

			if (!damaged.hasMetadata("NPC")) {
				if (combatTag.containsKey(damaged)) {
					combatTag.put((Player) damaged, 15);
				} else {
					damaged.sendMessage(Resources.tagCombat + ChatColor.RED + "전투상태에 돌입합니다!");
					initCombatCounter((Player) damaged);
				}
			}
		} else if (damager instanceof Projectile && ((Projectile) damager).getShooter() instanceof Player
				&& damaged instanceof Player && !((Projectile) damager).getShooter().equals(damaged)) {
			Player shooter = (Player) ((Projectile) damager).getShooter();

			if (!shooter.hasMetadata("NPC")) {
				if (combatTag.containsKey(shooter)) {
					combatTag.put(shooter, 15);
				} else {
					shooter.sendMessage(Resources.tagCombat + ChatColor.RED + "전투상태에 돌입합니다!");
					initCombatCounter(shooter);
				}
			}

			if (!damaged.hasMetadata("NPC")) {
				if (combatTag.containsKey(damaged)) {
					combatTag.put((Player) damaged, 15);
				} else {
					damaged.sendMessage(Resources.tagCombat + ChatColor.RED + "전투상태에 돌입합니다!");
					initCombatCounter((Player) damaged);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onDeathLoot(PlayerDeathEvent e) {
		Player player = e.getEntity();
		e.setKeepInventory(true);
		WorldCoord coord = WorldCoord.parseWorldCoord(player.getLocation());

		try {
			if (!coord.hasTownBlock() || !coord.getTownBlock().getTown().isAC()) {
				dropLoot(player);
			}
		} catch (NotRegisteredException e1) {
			dropLoot(player);
		} finally {
			e.setKeepInventory(true);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void deathWithCombatTag(PlayerDeathEvent e) {
		Player player = e.getEntity();

		if (combatTag.containsKey(player)) {
			combatCounter.get(player).cancel();
			combatCounter.remove(player);
			combatTag.remove(player);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onCooltimeCheck(PlayerQuitEvent e) {
		Player player = e.getPlayer();

		if (combatTag.containsKey(player)) {
			dropLoot(player);
			player.setHealth(0);
			Bukkit.broadcastMessage(Resources.tagDeath + ChatColor.DARK_RED + player.getName() + ChatColor.RED
					+ " 님이 전투 중에 퇴장하여 사망하셨습니다.");
			player.getWorld().strikeLightningEffect(player.getLocation());
		}
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onQuitWithPvPProt(PlayerQuitEvent e) {
		savePvPProt(e.getPlayer());
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onJoinWithPvPProt(PlayerJoinEvent e) {
		Player player = e.getPlayer();

		if (player.hasPlayedBefore()) {
			ResultSet rs = EvitaCoreMain.getInstance().getDB()
					.select("SELECT pvpprot FROM playerinfo WHERE uuid = '" + player.getUniqueId() + "';");

			try {
				if (rs.next() && rs.getInt("pvpprot") > 0) {

					initPvPProt(player, rs.getInt("pvpprot"));
					EvitaCoreMain.getInstance().getDB()
							.query("UPDATE playerinfo SET pvpprot = NULL WHERE uuid ='" + player.getUniqueId() + "';");
				}

				rs.close();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
		} else {
			initPvPProt(player, 14400);
			player.sendMessage(
					Resources.tagCombat + ChatColor.GREEN + "처음 1시간 동안은 PvP 보호가 적용됩니다,	/PvP보호 해제 로 해제할 수 있습니다.");
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onMove(PlayerMoveEvent e) {
		Location to = e.getTo();
		Location from = e.getFrom();
		if (!(from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY()
				&& from.getBlockZ() == to.getBlockZ() && from.getWorld().equals(to.getWorld()))) {
			Player player = e.getPlayer();

			if (isTpDelay(player))
				cancelTpDelay(player);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onInteract(PlayerInteractEvent e) {
		if (!(e.getAction().equals(Action.RIGHT_CLICK_AIR) || e.getAction().equals(Action.LEFT_CLICK_AIR))) {
			Player player = e.getPlayer();

			if (isTpDelay(player))
				cancelTpDelay(player);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onAttack(EntityDamageByEntityEvent e) {
		Entity damager = e.getDamager();
		if (damager instanceof Player) {
			if (isTpDelay((Player) damager))
				cancelTpDelay((Player) damager);
		} else if (damager instanceof Projectile) {
			ProjectileSource shooter = ((Projectile) damager).getShooter();
			if (shooter instanceof Player) {
				if (isTpDelay((Player) shooter))
					cancelTpDelay((Player) shooter);
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onDamaged(EntityDamageEvent e) {
		if (!e.isCancelled()) {
			Entity damaged = e.getEntity();

			if (damaged instanceof Player && isTpDelay((Player) damaged)) {
				cancelTpDelay((Player) damaged);
			}
		}
	}
}
