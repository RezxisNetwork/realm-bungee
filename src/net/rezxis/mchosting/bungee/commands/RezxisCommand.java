package net.rezxis.mchosting.bungee.commands;

import java.util.ArrayList;
import java.util.UUID;

import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.rezxis.mchosting.bungee.Bungee;
import net.rezxis.mchosting.database.object.player.DBIP;
import net.rezxis.mchosting.database.object.player.DBPIP;
import net.rezxis.mchosting.database.object.player.DBPlayer;
import net.rezxis.mchosting.database.object.server.DBServer;

public class RezxisCommand extends Command {

	public RezxisCommand() {
		super("rezxis", "rezxis.admin");
	}

	@SuppressWarnings("deprecation")
	@Override
	public void execute(CommandSender sender, String[] args) {
		if (args[0].equalsIgnoreCase("ban")) {
			//Usage : /rezxis ban <target> <reason>
			if (args.length != 3) {
				sender.sendMessage(ChatColor.RED+"Usage /rezxis ban <target> <reason>");
			} else {
				UUID uuid;
				ProxiedPlayer player = BungeeCord.getInstance().getPlayer(args[1]);
				if (player == null) {
					uuid = Bungee.instance.uTable.get(args[1]).getUuid();
				} else if (player.isConnected()) {
					uuid = player.getUniqueId();
					player.disconnect(ChatColor.RED+"Banned!");
				} else {
					uuid = Bungee.instance.uTable.get(args[1]).getUuid();
				}
				DBPlayer dp = Bungee.instance.pTable.get(uuid);
				dp.setBan(true);
				dp.setReason(args[2]);
				dp.update();
			}
		} else if (args[0].equalsIgnoreCase("ipban")) {
			if (args.length != 3) {
				sender.sendMessage(ChatColor.RED+"Usage /rezxis ipban <target> <reason>");
			} else {
				ProxiedPlayer player = BungeeCord.getInstance().getPlayer(args[1]);
				UUID uuid = null;
				if (player == null) {
					uuid = Bungee.instance.uTable.get(args[1]).getUuid();
				} else if (player.isConnected()) {
					uuid = player.getUniqueId();
					player.disconnect(ChatColor.RED+"Banned!");
				} else {
					uuid = Bungee.instance.uTable.get(args[1]).getUuid();
				}
				DBPlayer dp = Bungee.instance.pTable.get(uuid);
				dp.setBan(true);
				dp.setReason(ChatColor.RED+args[2]);
				dp.update();
				sender.sendMessage(ChatColor.RED+"Processing searching database to ipban "+args[1]);
				BungeeCord.getInstance().getScheduler().runAsync(Bungee.instance, new Runnable() {
					public void run() {
						ArrayList<DBPIP> pips = Bungee.instance.pipTable.getAllIPPlayer(dp.getId());
						sender.sendMessage(ChatColor.GREEN+"lookuped for "+pips.size()+" ip link!");
						for (DBPIP pip : pips) {
							DBIP dip = Bungee.instance.ipTable.getFromID(pip.getIp());
							dip.setBanned(true);
							dip.setReason(ChatColor.RED+args[2]);
							dip.update();
							sender.sendMessage(ChatColor.GREEN+"Processed : "+dip.getIp());
							sender.sendMessage(ChatColor.GREEN+"searching "+dip.getIp()+" accounts");
							ArrayList<DBPIP> targets = Bungee.instance.pipTable.getAllfromIP(dip.getId());
							for (DBPIP spip : targets) {
								DBPlayer target = Bungee.instance.pTable.getFromID(spip.getPlayer());
								target.setBan(true);
								target.setReason(ChatColor.RED+"sub account : "+args[1]+" - "+args[2]);
								target.update();
							}
							sender.sendMessage(ChatColor.RED+"banned for "+targets.size()+" accounts in "+dip.getIp()+" !");
						}
						sender.sendMessage(ChatColor.GREEN+args[1]+" was successfully banned!");
					}
				});
			}
		} else if (args[0].equalsIgnoreCase("unban")) {
			if (args.length != 2) {
				sender.sendMessage(ChatColor.RED+"Usage /rezxis unban <target>");
				return;
			}
			DBPlayer dp = Bungee.instance.pTable.get(Bungee.instance.uTable.get(args[1]).getUuid());
			dp.setBan(false);
			dp.setReason("");
			dp.update();
			sender.sendMessage(ChatColor.RED+"Processing searching database to unban "+args[1]);
			BungeeCord.getInstance().getScheduler().runAsync(Bungee.instance, new Runnable() {
				public void run() {
					ArrayList<DBPIP> pips = Bungee.instance.pipTable.getAllIPPlayer(dp.getId());
					sender.sendMessage(ChatColor.GREEN+"lookuped for "+pips.size()+" ip link!");
					for (DBPIP pip : pips) {
						DBIP dip = Bungee.instance.ipTable.getFromID(pip.getId());
						dip.setBanned(false);
						dip.setReason("");
						dip.update();
						sender.sendMessage(ChatColor.GREEN+"Processed : "+dip.getIp());
						sender.sendMessage(ChatColor.GREEN+"searching "+dip.getIp()+" accounts");
						ArrayList<DBPIP> targets = Bungee.instance.pipTable.getAllfromIP(dip.getId());
						for (DBPIP spip : targets) {
							DBPlayer target = Bungee.instance.pTable.getFromID(spip.getPlayer());
							target.setBan(false);
							target.setReason("");
							target.update();
						}
						sender.sendMessage(ChatColor.RED+"unbanned for "+targets.size()+" accounts in "+dip.getIp()+" !");
					}
					sender.sendMessage(ChatColor.GREEN+args[1]+" was successfully unbanned!");
				}
			});
		} else if (args[0].equalsIgnoreCase("status")) {
			if (args.length != 2) {
				sender.sendMessage(ChatColor.RED+"Usage /rezxis status <target>");
				return;
			}
			BungeeCord.getInstance().getScheduler().runAsync(Bungee.instance, new Runnable() {
				public void run() {
					UUID uuid;
					if (BungeeCord.getInstance().getPlayer(args[1]).isConnected()) {
						uuid = BungeeCord.getInstance().getPlayer(args[1]).getUniqueId();
					} else {
						uuid = Bungee.instance.uTable.get(args[1]).getUuid();
					}
					DBPlayer player = Bungee.instance.pTable.get(uuid);
					DBServer server = Bungee.instance.sTable.get(uuid);
					if (player == null) {
						sender.sendMessage(ChatColor.RED+args[1]+" doesn't exisst!");
						return;
					}
					msg(sender,"Status - "+args[1]);
					msg(sender,ChatColor.RED+"General Status");
					msg(sender,"ID : "+player.getId());
					msg(sender,"UUID : "+uuid.toString());
					msg(sender,"Rank : "+player.getRank().name());
					msg(sender,"RC : "+player.getCoin());
					msg(sender,"Expire : "+player.getRankExpire().toLocaleString());
					msg(sender,"OfflineBoot : "+player.isOfflineBoot());
					msg(sender,"NextVote : "+player.getNextVote().toLocaleString());
					msg(sender,"Online : "+player.isOnline());
					msg(sender,"Banned : "+player.isBan());
					if (player.isBan())
						msg(sender,"Reason : "+player.getReason());
					msg(sender,ChatColor.RED+"Server Status");
					if (server == null) {
						msg(sender,"The Player has no server.");
					} else {
						msg(sender,"ID : "+server.getId());
						msg(sender,"name : "+server.getDisplayName());
						msg(sender,"port : "+server.getPort());
						msg(sender,"status : "+server.getStatus().name());
						msg(sender,"host : "+server.getHost());
						msg(sender,"vote : "+server.getVote());
						msg(sender,"players : "+server.getPlayers());
					}
					msg(sender,"account status");
					ArrayList<DBPIP> pips = Bungee.instance.pipTable.getAllIPPlayer(player.getId());
					for (DBPIP pip : pips) {
						DBIP dip = Bungee.instance.ipTable.getFromID(pip.getId());
						msg(sender,"ip : "+dip.getIp());
						ArrayList<DBPIP> targets = Bungee.instance.pipTable.getAllfromIP(dip.getId());
						for (DBPIP spip : targets) {
							DBPlayer target = Bungee.instance.pTable.getFromID(spip.getPlayer());
							msg(sender,"-"+BungeeCord.getInstance().getPlayer(target.getUUID()).getName());
						}
					}
				}
			});
		}
	}
	
	private static void msg(CommandSender sender, String msg) {
		sender.sendMessage(ChatColor.GREEN+msg);
	}
}