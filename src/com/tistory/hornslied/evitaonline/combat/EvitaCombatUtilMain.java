package com.tistory.hornslied.evitaonline.combat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.tistory.hornslied.evitaonline.commands.PvPProtCommand;
import com.tistory.hornslied.evitaonline.listeners.CombatRuleListener;

public class EvitaCombatUtilMain extends JavaPlugin {
	private static EvitaCombatUtilMain instance;
	
	public static EvitaCombatUtilMain getInstance() {
		return instance;
	}
	
	@Override
	public void onEnable() {
		instance = this;
		PvPManager.getInstance();
		
		registerEvents();
		initCommands();
	}
	
	@Override
	public void onDisable() {
		PvPManager pvpManager = PvPManager.getInstance();
		
		for (Player p : Bukkit.getOnlinePlayers()) {
			pvpManager.savePvPProt(p);
		}
	}
	
	private void registerEvents() {
		PluginManager pm = Bukkit.getPluginManager();
		
		pm.registerEvents(new CombatRuleListener(), this);
	}
	
	private void initCommands() {
		getCommand("pvpprot").setExecutor(new PvPProtCommand());
	}
}
