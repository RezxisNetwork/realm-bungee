package net.rezxis.mchosting.bungee;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.commons.lang3.RandomStringUtils;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

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
import net.rezxis.utils.WebAPI.CheckIPResponse;
import net.rezxis.mchosting.database.object.player.DBPlayer.Rank;
import net.rezxis.mchosting.database.tables.UuidTable;

public class JoinListeners implements Listener {
	
	public void onLink(LoginEvent e) {
		e.setCancelled(true);
		DBPlayer dp = Tables.getPTable().get(e.getConnection().getUniqueId());
		if (dp == null) {
			e.setCancelReason(ChatColor.RED+"一度サーバーに接続してください。");
			return;
		}
		if (dp.getDiscordId() != -1) {
			e.setCancelReason(ChatColor.RED+"既にDiscordアカウントとリンクされています。");
			return;
		}
		String key = RandomStringUtils.randomAlphabetic(10);
		dp.setVerifyCode(key);
		dp.update();
		e.setCancelReason(ChatColor.GREEN+"認証コード : "+key);
	}
	
	@EventHandler
	public void onJoin(LoginEvent e) {
		String hostname = e.getConnection().getVirtualHost().getHostName();
		if (hostname != null && hostname.startsWith("link.rezxis.net")) {
			onLink(e);
			return;
		}
		boolean first = false;
		DBPlayer player = Tables.getPTable().get(e.getConnection().getUniqueId());
		if (player == null) {
			first = true;
			Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Japan"),Locale.JAPANESE);
			calendar.add(Calendar.DAY_OF_WEEK, -2);
			player = new DBPlayer(-1, e.getConnection().getUniqueId(), Rank.NORMAL, 0, false, new Date(), calendar.getTime(), true, false ,"",false,false,new Date(),"",0,"",-1, "");
			Tables.getPTable().insert(player);
		}
		if (Bungee.instance.maintenance) {
			if (player.getRank() != DBPlayer.Rank.DEVELOPER && player.getRank() != DBPlayer.Rank.STAFF) {
				e.setCancelled(true);
				e.setCancelReason(ChatColor.RED+"rezxis is under maintenance mode");
				return;
			}
		}
		String ip = e.getConnection().getAddress().getAddress().getHostAddress();
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
			e.setCancelReason(ChatColor.RED+"5アカウント以上の同時接続はできません。");
			return;
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
			e.setCancelReason(ChatColor.RED+player.getReason());
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
			if (!first) {
				ArrayList<DBPIP> pips = Tables.getPipTable().getAllIPPlayer(player.getId());
				StringBuilder sb = new StringBuilder("New ip address : "+e.getConnection().getName()+" Info\n");
				for (DBPIP pip : pips) {
					DBIP dip = Tables.getIpTable().getFromID(pip.getIp());
					sb.append("ip : "+dip.getIp()+"\n");
					ArrayList<DBPIP> targets = Tables.getPipTable().getAllfromIP(dip.getId());
					for (DBPIP spip : targets) {
						DBPlayer target = Tables.getPTable().getFromID(spip.getPlayer());
						sb.append("-"+UuidTable.instnace.get(target.getUUID()).getName()+"\n");
					}
				}
				WebAPI.webhook(DiscordWebHookEnum.PRIVATE, sb.toString());
			}
		}
		if (dbip.isBanned()) {
			e.setCancelled(true);
			e.setCancelReason(dbip.getReason());
			player.setBan(true);
			player.setReason(dbip.getReason());
			player.update();
			return;
		}
		if (first) {
			ArrayList<DBPIP> pips = Tables.getPipTable().getAllIPPlayer(player.getId());
			StringBuilder sb = new StringBuilder("First join : "+e.getConnection().getName()+" Info\n");
			for (DBPIP pip : pips) {
				DBIP dip = Tables.getIpTable().getFromID(pip.getIp());
				sb.append("ip : "+dip.getIp()+"\n");
				ArrayList<DBPIP> targets = Tables.getPipTable().getAllfromIP(dip.getId());
				for (DBPIP spip : targets) {
					DBPlayer target = Tables.getPTable().getFromID(spip.getPlayer());
					sb.append("-"+UuidTable.instnace.get(target.getUUID()).getName()+"\n");
				}
			}
			WebAPI.webhook(DiscordWebHookEnum.PRIVATE, sb.toString());
		}
		
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
	        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
	        XContentBuilder builder = XContentFactory.jsonBuilder();
	        builder.startObject();
	        {
	            builder.timeField("@timestamp", sdf.format(System.currentTimeMillis()));
	            builder.field("player",e.getConnection().getName());
	            builder.field("name", "login");
	            builder.field("join",true);
	            builder.field("ip", ip);
	            builder.field("first", first);
	        }
	        builder.endObject();
	        IndexRequest request = new IndexRequest("login").source(builder);
	        Bungee.rcl.indexAsync(request, Bungee.COMMON_OPTIONS, Bungee.listener);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	@EventHandler
	public void onJoin(PostLoginEvent e) {
		DBPlayer player = Tables.getPTable().get(e.getPlayer().getUniqueId());
		if (player.isStaff()) {
			e.getPlayer().setPermission("rezxis.admin", true);
			e.getPlayer().setPermission("bat.lookup.displayip", true);
			e.getPlayer().setPermission("bat.admin", true);
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

		if (!player.isVpnBypass()) {
			BungeeCord.getInstance().getScheduler().runAsync(Bungee.instance, new Runnable() {
				public void run() {
					try {
						String ip = e.getPlayer().getPendingConnection().getAddress().getAddress().getHostAddress();
						CheckIPResponse response = WebAPI.checkIP(ip);
						if (response.isBad()) {
							String msg = "[VPN] - username : ("+e.getPlayer().getPendingConnection().getName()+") , Address : ("+ip+") , Type : ("+response.getType()+") , Country : ("+response.getCountry()+")";
							for (ProxiedPlayer pp : BungeeCord.getInstance().getPlayers()) {
								if (pp.hasPermission("rezxis.admin"))
									pp.sendMessage(new TextComponent(ChatColor.RED+msg));
							}
							WebAPI.webhook(DiscordWebHookEnum.CONNECT, msg);
							DBPlayer dp = Tables.getPTable().get(e.getPlayer().getUniqueId());
							dp.setBan(true);
							dp.setReason(ChatColor.RED+"vpn was detected.");
							dp.update();
							e.getPlayer().disconnect(new TextComponent(ChatColor.RED+"あなたのIPアドレスはブロックされています。"));
							return;
						}
						if (!response.getCountry().equalsIgnoreCase("JP")) {
							if (response.getCountry().equalsIgnoreCase("OpenVPN")) {
								return;
							}
							e.getPlayer().disconnect(new TextComponent(ChatColor.RED+"国外からの接続はブロックされています。"));
							return;
						}
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			});
		}
	}
	
	@EventHandler
	public void onLeft(PlayerDisconnectEvent event) {
		Bungee.instance.inspection.remove(event.getPlayer().getUniqueId());
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
	        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
	        XContentBuilder builder = XContentFactory.jsonBuilder();
	        builder.startObject();
	        {
	            builder.timeField("@timestamp", sdf.format(System.currentTimeMillis()));
	            builder.field("player", event.getPlayer().getName());
	            builder.field("name", "login");
	            builder.field("join",false);
	            builder.field("ip", event.getPlayer().getPendingConnection().getAddress().getAddress().getHostAddress());
	            builder.field("first", false);
	        }
	        builder.endObject();
	        IndexRequest request = new IndexRequest("login").source(builder);
	        Bungee.rcl.indexAsync(request, Bungee.COMMON_OPTIONS, Bungee.listener);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		DBPlayer player = Tables.getPTable().get(event.getPlayer().getUniqueId());
		player.setOnline(false);
		player.update();
	}
}
