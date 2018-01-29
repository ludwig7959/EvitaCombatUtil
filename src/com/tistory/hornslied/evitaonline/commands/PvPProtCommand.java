package com.tistory.hornslied.evitaonline.commands;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.tistory.hornslied.evitaonline.combat.PvPManager;
import com.tistory.hornslied.evitaonline.utils.ChatTools;
import com.tistory.hornslied.evitaonline.utils.Resources;

public class PvPProtCommand implements CommandExecutor {
	private static ArrayList<String> output = new ArrayList<>();
	private static ArrayList<String> adminOutput = new ArrayList<>();

	static {
		output.add(ChatTools.formatTitle("/PvP보호"));
		output.add(ChatTools.formatCommand("", "/PvP보호", "해제", "현재 적용된 PvP 보호를 해제합니다."));
		adminOutput.add(ChatTools.formatCommand("관리자", "/PvP보호", "부여 <플레이어> <시간(초)>", "PvP 보호를 적용합니다."));
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (args.length > 0) {
			switch(args[0].toLowerCase()) {
			case "해제":
			case "expire":
				if(!(sender instanceof Player)) {
					sender.sendMessage(Resources.messageConsole);
					break;
				}
				
				PvPManager pvpManager = PvPManager.getInstance();
				Player player1 = (Player) sender;
				
				if(!pvpManager.isPvPProt(player1)) {
					sender.sendMessage(Resources.tagCombat + ChatColor.RED + "PvP 보호가 적용되어 있지 않습니다.");
					break;
				}
				
				pvpManager.expirePvPProt(player1);
				sender.sendMessage(Resources.tagCombat + ChatColor.AQUA + "PvP 보호가 해제되었습니다.");
				break;
			case "부여":
			case "add":
				if(!sender.hasPermission("evita.mod")) {
					sender.sendMessage(Resources.messagePermission);
					break;
				}
				
				if(args.length < 3) {
					sender.sendMessage(Resources.tagServer + ChatColor.RED + "명령어 사용 방법: /PvP보호 부여 <플레이어> <시간(초)>");
					break;
				}
				
				Player player2 = Bukkit.getPlayer(args[1]);
				
				if(player2 == null) {
					sender.sendMessage(Resources.messagePlayerNotExist);
					break;
				}
				
				if(PvPManager.getInstance().isPvPProt(player2)) {
					sender.sendMessage(Resources.tagServer + ChatColor.RED + "이미 PvP 보호가 적용된 플레이어입니다.");
					break;
				}
				
				int sec;
				try {
					sec = Integer.parseInt(args[2]);
				} catch(NumberFormatException e) {
					sender.sendMessage(Resources.tagServer + ChatColor.RED + "명령어 사용 방법: /PvP보호 부여 <플레이어> <시간(초)>");
					break;
				}
				
				PvPManager.getInstance().initPvPProt(player2, sec);
				break;
			default:
				for (String line : output) {
					sender.sendMessage(line);
				}

				if (sender.hasPermission("evita.mod")) {
					for (String line : adminOutput) {
						sender.sendMessage(line);
					}
				}
				break;
			}
		} else {
			for (String line : output) {
				sender.sendMessage(line);
			}

			if (sender.hasPermission("evita.mod")) {
				for (String line : adminOutput) {
					sender.sendMessage(line);
				}
			}
		}
		return true;
	}

}
