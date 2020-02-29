package net.rezxis.mchosting.bungee.commands;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.UUID;

import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.ServerConnector;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.rezxis.mchosting.bungee.Bungee;
import net.rezxis.mchosting.database.Tables;
import net.rezxis.mchosting.database.object.player.DBIP;
import net.rezxis.mchosting.database.object.player.DBPIP;
import net.rezxis.mchosting.database.object.player.DBPlayer;
import net.rezxis.mchosting.database.object.player.DBUUID;
import net.rezxis.mchosting.database.object.server.DBServer;
import net.rezxis.mchosting.database.tables.UuidTable;
import net.rezxis.utils.WebAPI;
import net.rezxis.utils.WebAPI.DiscordWebHookEnum;

public class RezxisCommand extends Command {

	public RezxisCommand() {
		super("rezxis", "rezxis.admin");
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		if (args[0].equalsIgnoreCase("ban")) {
			//Usage : /rezxis ban <target> <reason>
			if (args.length != 3) {
				sender.sendMessage(new TextComponent(ChatColor.RED+"Usage /rezxis ban <target> <reason>"));
			} else {
				UUID uuid;
				if (BungeeCord.getInstance().getPlayer(args[1]) != null && BungeeCord.getInstance().getPlayer(args[1]).isConnected()) {
					uuid = BungeeCord.getInstance().getPlayer(args[1]).getUniqueId();
					BungeeCord.getInstance().getPlayer(args[1]).disconnect(new TextComponent(ChatColor.RED+args[2]));
				} else {
					DBUUID dbuid = Tables.getUTable().get(args[1]);
					if (dbuid == null) {
						sender.sendMessage(new TextComponent(ChatColor.RED+"Couldn't find the player."));
						return;
					}
					uuid = dbuid.getUuid();
				}
				DBPlayer dp = Tables.getPTable().get(uuid);
				dp.setBan(true);
				dp.setReason(args[2]);
				dp.update();
				WebAPI.webhook(DiscordWebHookEnum.PRIVATE, String.format("%s was banned by %s reason : %s", args[1], sender.getName(), args[2]));
			}
		} else if (args[0].equalsIgnoreCase("ipban")) {
			if (args.length != 3) {
				sender.sendMessage(new TextComponent(ChatColor.RED+"Usage /rezxis ipban <target> <reason>"));
			} else {
				final UUID uuid;
				if (BungeeCord.getInstance().getPlayer(args[1]) != null && BungeeCord.getInstance().getPlayer(args[1]).isConnected()) {
					uuid = BungeeCord.getInstance().getPlayer(args[1]).getUniqueId();
				} else {
					DBUUID dbuid = Tables.getUTable().get(args[1]);
					if (dbuid == null) {
						sender.sendMessage(new TextComponent(ChatColor.RED+"Couldn't find the player."));
						return;
					}
					uuid = dbuid.getUuid();
				}
				WebAPI.webhook(DiscordWebHookEnum.PRIVATE, String.format("%s was banned by %s reason : %s", args[1], sender.getName(), args[2]));
				DBPlayer dp = Tables.getPTable().get(uuid);
				dp.setBan(true);
				dp.setReason(ChatColor.RED+args[2]);
				dp.update();
				sender.sendMessage(new TextComponent(ChatColor.RED+"Processing searching database to ipban "+args[1]));
				ProxiedPlayer tgt0 = BungeeCord.getInstance().getPlayer(uuid);
				if (tgt0 != null && tgt0.isConnected()) {
					tgt0.disconnect(new TextComponent(ChatColor.RED+args[2]));
				}
				BungeeCord.getInstance().getScheduler().runAsync(Bungee.instance, new Runnable() {
					public void run() {
						ArrayList<DBPIP> pips = Tables.getPipTable().getAllIPPlayer(dp.getId());
						sender.sendMessage(new TextComponent(ChatColor.GREEN+"lookuped for "+pips.size()+" ip link!"));
						for (DBPIP pip : pips) {
							DBIP dip = Tables.getIpTable().getFromID(pip.getIp());
							dip.setBanned(true);
							dip.setReason(ChatColor.RED+args[2]);
							dip.update();
							sender.sendMessage(new TextComponent(ChatColor.GREEN+"Processed : "+dip.getIp()));
							sender.sendMessage(new TextComponent(ChatColor.GREEN+"searching "+dip.getIp()+" accounts"));
							ArrayList<DBPIP> targets = Tables.getPipTable().getAllfromIP(dip.getId());
							for (DBPIP spip : targets) {
								DBPlayer target = Tables.getPTable().getFromID(spip.getPlayer());
								if (target.getUUID().equals(uuid))
									continue;
								target.setBan(true);
								target.setReason(ChatColor.RED+"sub account : "+args[1]+" - "+args[2]);
								target.update();
								ProxiedPlayer tgt = BungeeCord.getInstance().getPlayer(target.getUUID());
								if (tgt != null && tgt.isConnected()) {
									tgt.disconnect(new TextComponent(ChatColor.RED+"sub account : "+args[1]+" - "+args[2]));
								}
							}
							sender.sendMessage(new TextComponent(ChatColor.RED+"banned for "+targets.size()+" accounts in "+dip.getIp()+" !"));
						}
						sender.sendMessage(new TextComponent(ChatColor.GREEN+args[1]+" was successfully banned!"));
					}
				});
			}
		} else if (args[0].equalsIgnoreCase("unban")) {
			if (args.length != 2) {
				sender.sendMessage(new TextComponent(ChatColor.RED+"Usage /rezxis unban <target>"));
				return;
			}
			WebAPI.webhook(DiscordWebHookEnum.PRIVATE, String.format("%s was unbanned by %s", args[1], sender.getName()));
			DBPlayer dp = Tables.getPTable().get(Tables.getUTable().get(args[1]).getUuid());
			dp.setBan(false);
			dp.setReason("");
			dp.update();
			sender.sendMessage(new TextComponent(ChatColor.RED+"Processing searching database to unban "+args[1]));
			BungeeCord.getInstance().getScheduler().runAsync(Bungee.instance, new Runnable() {
				public void run() {
					ArrayList<DBPIP> pips = Tables.getPipTable().getAllIPPlayer(dp.getId());
					sender.sendMessage(new TextComponent(ChatColor.GREEN+"lookuped for "+pips.size()+" ip link!"));
					for (DBPIP pip : pips) {
						DBIP dip = Tables.getIpTable().getFromID(pip.getIp());
						dip.setBanned(false);
						dip.setReason("");
						dip.update();
						sender.sendMessage(new TextComponent(ChatColor.GREEN+"Processed : "+dip.getIp()));
						sender.sendMessage(new TextComponent(ChatColor.GREEN+"searching "+dip.getIp()+" accounts"));
						ArrayList<DBPIP> targets = Tables.getPipTable().getAllfromIP(dip.getId());
						for (DBPIP spip : targets) {
							DBPlayer target = Tables.getPTable().getFromID(spip.getPlayer());
							target.setBan(false);
							target.setReason("");
							target.update();
						}
						sender.sendMessage(new TextComponent(ChatColor.RED+"unbanned for "+targets.size()+" accounts in "+dip.getIp()+" !"));
					}
					sender.sendMessage(new TextComponent(ChatColor.GREEN+args[1]+" was successfully unbanned!"));
				}
			});
		} else if (args[0].equalsIgnoreCase("status")) {
			if (args.length != 2) {
				sender.sendMessage(new TextComponent(ChatColor.RED+"Usage /rezxis status <target>"));
				return;
			}
			BungeeCord.getInstance().getScheduler().runAsync(Bungee.instance, new Runnable() {
				@SuppressWarnings("deprecation")
				public void run() {
					UUID uuid;
					if (BungeeCord.getInstance().getPlayer(args[1]) != null && BungeeCord.getInstance().getPlayer(args[1]).isConnected()) {
						uuid = BungeeCord.getInstance().getPlayer(args[1]).getUniqueId();
					} else {
						DBUUID dbuid = Tables.getUTable().get(args[1]);
						if (dbuid == null) {
							sender.sendMessage(new TextComponent(ChatColor.RED+"Couldn't find the player."));
							return;
						}
						uuid = dbuid.getUuid();
					}
					if (uuid == null) {
						sender.sendMessage(new TextComponent(ChatColor.RED+args[1]+" doesn't exisst!"));
						return;
					}
					DBPlayer player = Tables.getPTable().get(uuid);
					DBServer server = Tables.getSTable().get(uuid);
					if (player == null) {
						sender.sendMessage(new TextComponent(ChatColor.RED+args[1]+" doesn't exisst!"));
						return;
					}
					msg(sender,"Status - "+args[1]);
					msg(sender,ChatColor.RED+"General Status");
					if (BungeeCord.getInstance().getPlayer(args[1]) != null && BungeeCord.getInstance().getPlayer(args[1]).isConnected())
						msg(sender,"Connected to : "+BungeeCord.getInstance().getPlayer(args[1]).getServer().getInfo().getName());
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
					ArrayList<DBPIP> pips = Tables.getPipTable().getAllIPPlayer(player.getId());
					for (DBPIP pip : pips) {
						DBIP dip = Tables.getIpTable().getFromID(pip.getIp());
						msg(sender,"ip : "+dip.getIp());
						ArrayList<DBPIP> targets = Tables.getPipTable().getAllfromIP(dip.getId());
						for (DBPIP spip : targets) {
							DBPlayer target = Tables.getPTable().getFromID(spip.getPlayer());
							msg(sender,"-"+UuidTable.instnace.get(target.getUUID()).getName());
						}
					}
				}
			});
		} else if (args[0].equalsIgnoreCase("inspection")) {
			if (sender instanceof ProxiedPlayer) {
				ProxiedPlayer sdr = (ProxiedPlayer) sender;
				if (Bungee.instance.inspection.contains(sdr.getUniqueId())) {
					sdr.sendMessage(new TextComponent(ChatColor.RED+"Chat inspection was disabled."));
					Bungee.instance.inspection.remove(sdr.getUniqueId());
				} else {
					sdr.sendMessage(new TextComponent(ChatColor.GREEN+"Chat inspection was enabled."));
					Bungee.instance.inspection.add(sdr.getUniqueId());
				}
			}
		} else if (args[0].equalsIgnoreCase("spoof")) {
			try {
				Field field = ServerConnector.class.getDeclaredField("arra");
				field.setAccessible(true);
				@SuppressWarnings("unchecked")
				ArrayList<String> arr = (ArrayList<String>) field.get(null);
				if (arr.contains(((ProxiedPlayer)sender).getUniqueId().toString().replace("-", ""))) {
					arr.remove(((ProxiedPlayer)sender).getUniqueId().toString().replace("-", ""));
					sender.sendMessage(new TextComponent(ChatColor.RED+"ip変更を無効化しました。"));
				} else {
					arr.add(((ProxiedPlayer)sender).getUniqueId().toString().replace("-", ""));
					sender.sendMessage(new TextComponent(ChatColor.GREEN+"ip変更を有効化しました。"));
					sender.sendMessage(new TextComponent(ChatColor.GREEN+"次回別サーバーに接続するときipは変更されます。"));
				}
				field.set(null, arr);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		} else if (args[0].equalsIgnoreCase("logging")) {
			if (Bungee.instance.logging) {
				sender.sendMessage(new TextComponent(ChatColor.RED+"Chat logを無効化しました。"));
				Bungee.instance.logging = false;
				WebAPI.webhook(DiscordWebHookEnum.PRIVATE, String.format("ChatLog was disabled by %s", sender.getName()));
			} else {
				sender.sendMessage(new TextComponent(ChatColor.GREEN+"Chat logを有効化しました。"));
				Bungee.instance.logging = true;
				WebAPI.webhook(DiscordWebHookEnum.PRIVATE, String.format("ChatLog was enabled by %s", sender.getName()));
			}
		} else if (args[0].equalsIgnoreCase("kickall")) {
			if (args.length != 2) {
				return;
			}
			for (ProxiedPlayer player : BungeeCord.getInstance().getPlayers()) {
				if (!player.hasPermission("rezxis.admin")) {
					player.disconnect(new TextComponent("切断されました。　理由 : "+args[1]));
				}
			}
		}else {
			sender.sendMessage(new TextComponent(ChatColor.RED+"commandが存在しません。"));
		}
	}
	
	private static void msg(CommandSender sender, String msg) {
		sender.sendMessage(new TextComponent(ChatColor.GREEN+msg));
	}
}
