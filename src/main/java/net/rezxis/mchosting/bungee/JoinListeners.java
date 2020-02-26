package net.rezxis.mchosting.bungee;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;

import org.apache.commons.lang3.RandomStringUtils;

import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.ServerConnector;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.rezxis.mchosting.database.Tables;
import net.rezxis.mchosting.database.object.player.DBIP;
import net.rezxis.mchosting.database.object.player.DBPIP;
import net.rezxis.mchosting.database.object.player.DBPlayer;
import net.rezxis.mchosting.database.object.player.DBUUID;
import net.rezxis.utils.WebAPI;
import net.rezxis.utils.WebAPI.DiscordWebHookEnum;
import net.rezxis.utils.WebAPI.McuaResponse;
import net.rezxis.mchosting.database.object.player.DBPlayer.Rank;

public class JoinListeners implements Listener {

	
	@EventHandler
	public void onJoin(LoginEvent e) {
		DBPlayer player = Tables.getPTable().get(e.getConnection().getUniqueId());
		if (player == null) {
			player = new DBPlayer(-1, e.getConnection().getUniqueId(), Rank.NORMAL, 0, false, new Date(), new Date(), true, false ,"",false,false,new Date(),"",0,"",-1);
			Tables.getPTable().insert(player);
		}
		String ip = e.getConnection().getAddress().getAddress().getHostAddress();
		if (e.getConnection().getVirtualHost().getHostName().startsWith("link")) {
			if (player.getDiscordId() != -1) {
				e.setCancelled(true);
				e.setCancelReason(new TextComponent(ChatColor.RED+"あなたのDiscordはすでにリンクされています。"));
				return;
			}
			String key = RandomStringUtils.randomAlphabetic(10);
			player.setVerifyCode(key);
			player.update();
			e.setCancelled(true);
			e.setCancelReason(new TextComponent(ChatColor.GREEN+"Link Code : "+key));
			return;
		}
		//check multi connection;
		int accs = 0;
		for (ProxiedPlayer pp : BungeeCord.getInstance().getPlayers()) {
			if (!pp.getUniqueId().equals(e.getConnection().getUniqueId())) {
				if (pp.getAddress().getHostString().equals(ip)) {
					accs += 1;
				}
			}
		}
		if (accs >= 5) {
			e.setCancelled(true);
			e.setCancelReason(new TextComponent(ChatColor.RED+"同時接続はできません。"));
		}
		if (!player.isVpnBypass()) {
			BungeeCord.getInstance().getScheduler().runAsync(Bungee.instance, new Runnable() {
				public void run() {
					try {
						McuaResponse response = WebAPI.checkIP(ip);
						if (response.isBad() || !response.getCountry().equalsIgnoreCase("JP")) {
							BungeeCord.getInstance().getPlayer(e.getConnection().getUniqueId()).disconnect(new TextComponent(ChatColor.RED+"あなたのIPアドレスはブロックされています。"));
							String msg = "[VPN] - username : ("+e.getConnection().getName()+") , Address : ("+ip+") , Type : ("+response.getType()+") , Country : ("+response.getCountry()+")";
							for (ProxiedPlayer pp : BungeeCord.getInstance().getPlayers()) {
								if (pp.hasPermission("rezxis.admin"))
									pp.sendMessage(new TextComponent(ChatColor.RED+msg));
							}
							WebAPI.webhook(DiscordWebHookEnum.CONNECT, msg);
							return;
						}
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			});
		}
		DBUUID dbuid = Tables.getUTable().get(e.getConnection().getUniqueId());
		if (dbuid == null) {
			dbuid = new DBUUID(-1,e.getConnection().getName(), e.getConnection().getUniqueId());
			Tables.getUTable().insert(dbuid);
		} else if (!dbuid.getName().equals(e.getConnection().getName())) {
			dbuid.setName(e.getConnection().getName());
			dbuid.update();
		}
		
		if (player.isBan()) {
			e.setCancelled(true);
			e.setCancelReason(new TextComponent(ChatColor.RED+player.getReason()));
			return;
		}
		DBIP dbip = Tables.getIpTable().get(ip);
		if (dbip == null) {
			dbip = new DBIP(-1,ip,false,"",new Date());
			Tables.getIpTable().insert(dbip);
		}
		DBPIP dbpip = Tables.getPipTable().getFromIPPlayer(dbip.getId(),player.getId());
		if (dbpip == null) {
			dbpip = new DBPIP(-1,dbip.getId(),player.getId());
			Tables.getPipTable().insert(dbpip);
		}
		if (dbip.isBanned()) {
			e.setCancelled(true);
			e.setCancelReason(new TextComponent(dbip.getReason()));
			player.setBan(true);
			player.setReason(dbip.getReason());
			player.update();
			return;
		}
	}
	
	@EventHandler
	public void onJoin(PostLoginEvent e) {
		DBPlayer player = Tables.getPTable().get(e.getPlayer().getUniqueId());
		if (player.isStaff()) {
			e.getPlayer().setPermission("rezxis.admin", true);
			try {
				Field field = ServerConnector.class.getDeclaredField("arra");
				field.setAccessible(true);
				@SuppressWarnings("unchecked")
				ArrayList<String> arr = (ArrayList<String>) field.get(null);
				if (!arr.contains(e.getPlayer().getUniqueId().toString().replace("-", ""))) {
					arr.add(e.getPlayer().getUniqueId().toString().replace("-", ""));
					field.set(null, arr);
				}
				e.getPlayer().sendMessage(new TextComponent(ChatColor.GREEN+"自動的にip変更が有効化されました。"));
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		if (player.getRank() == Rank.DEVELOPER) {
			e.getPlayer().setPermission("rezxis.rank", true);
		} else {
			e.getPlayer().setPermission("rezxis.rank", false);
		}
		player.setOnline(true);
		player.update();
	}
	
	@EventHandler
	public void onLeft(PlayerDisconnectEvent event) {
		Bungee.instance.inspection.remove(event.getPlayer().getUniqueId());
	}
}
