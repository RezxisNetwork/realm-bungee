package net.rezxis.mchosting.bungee;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.event.ServerKickEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.conf.Configuration;
import net.md_5.bungee.event.EventHandler;
import net.rezxis.mchosting.bungee.WebAPI.McuaResponse;
import net.rezxis.mchosting.bungee.commands.PayCommand;
import net.rezxis.mchosting.bungee.commands.RezxisCommand;
import net.rezxis.mchosting.database.Database;
import net.rezxis.mchosting.database.object.player.DBIP;
import net.rezxis.mchosting.database.object.player.DBPIP;
import net.rezxis.mchosting.database.object.player.DBPlayer;
import net.rezxis.mchosting.database.object.player.DBPlayer.Rank;
import net.rezxis.mchosting.database.object.server.DBServer;
import net.rezxis.mchosting.database.tables.IPTable;
import net.rezxis.mchosting.database.tables.PIPTable;
import net.rezxis.mchosting.database.tables.PlayersTable;
import net.rezxis.mchosting.database.tables.ServersTable;
import net.rezxis.mchosting.network.WSClient;

public class Bungee extends Plugin implements Listener {

	public static Bungee instance;
	public WSClient ws;
	public Props props;
	public int min = 15;
	public PlayersTable pTable;
	public IPTable ipTable;
	public PIPTable pipTable;
	public ServersTable sTable;
	public ArrayList<String> messages;
	
	public void onEnable() {
		instance = this;
		getProxy().getPluginManager().registerCommand(this, new RezxisCommand());
		getProxy().getPluginManager().registerCommand(this, new PayCommand());
		messages = new ArrayList<>();
		messages.add(ChatColor.GREEN+"一日一回気に入ったレールムに投票しよう！"+ChatColor.AQUA+" /vote <投票対象サーバーのオーナー名>");
		messages.add(ChatColor.GREEN+"公式Discordに参加して、最新情報をゲットしよう！ "+ChatColor.AQUA+"https://discord.gg/kzBT6xg");
		messages.add(ChatColor.GREEN+"JMSに投票して報酬をゲットしよう！ "+ChatColor.AQUA+" https://minecraft.jp/servers/play.rezxis.net/vote");
		Database.init();
		pTable = new PlayersTable();
		ipTable = new IPTable();
		sTable = new ServersTable();
		pipTable = new PIPTable();
		getProxy().getPluginManager().registerListener(this, this);
		props = new Props("hosting.propertis");
		new Thread(()->{
				try {
					ws = new WSClient(new URI(props.SYNC_ADDRESS), new WSClientHandler());
				} catch (Exception e) {
					e.printStackTrace();
				}
				ws.setConnectionLostTimeout(0);
				ws.connect();
			
		}).start();
		this.getProxy().getScheduler().schedule(this, new Runnable() {
			public void run() {
				String msg = messages.get(new Random().nextInt(messages.size()-1));
				for (ProxiedPlayer player : getProxy().getPlayers()) {
					player.sendMessage(msg);
				}
			}
		}, 1, min, TimeUnit.MINUTES);
	}
	
	@EventHandler
	public void onLeft(PlayerDisconnectEvent e) {
		DBPlayer player = pTable.get(e.getPlayer().getUniqueId());
		player.setOnline(false);
		player.update();
	}
	
	@EventHandler
	public void onPreJoin(LoginEvent e) {
		String ip = e.getConnection().getAddress().getAddress().getHostAddress();
		try {
			McuaResponse response = WebAPI.checkIP(ip);
			if (response.isBad()) {
				e.setCancelled(true);
				e.setCancelReason(ChatColor.RED+"あなたのIPアドレスはブロックされています。");
				return;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		UUID uuid = e.getConnection().getUniqueId();
		if (uuid == null) {
			System.out.println("uuid is null");
		}
		DBPlayer player = pTable.get(uuid);
		if (player == null) {
			player = new DBPlayer(-1, uuid, Rank.NORMAL, 0, false, new Date(), new Date(), true, false ,"");
			pTable.insert(player);
		} else {
			player.setOnline(true);
		}
		if (player.isBan()) {
			e.setCancelled(true);
			e.setCancelReason(ChatColor.RED+player.getReason());
			return;
		}
		player.update();
		DBIP dbip = ipTable.get(ip);
		if (dbip == null) {
			dbip = new DBIP(-1,ip,false,"",new Date());
			ipTable.insert(dbip);
		}
		if (dbip.isBanned()) {
			e.setCancelled(true);
			e.setCancelReason(dbip.getReason());
			return;
		}
		DBPIP dbpip = pipTable.getFromIPPlayer(dbip.getId(),player.getId());
		if (dbpip == null) {
			dbpip = new DBPIP(-1,dbip.getId(),player.getId());
			pipTable.insert(dbpip);
		}
	}
	
	@EventHandler
	public void onJoin(PostLoginEvent e) {
		DBPlayer player = pTable.get(e.getPlayer().getUniqueId());
		Rank rank = player.getRank();
		if (rank == Rank.STAFF | rank == Rank.DEVELOPER | rank == Rank.OWNER) {
			e.getPlayer().setPermission("rezxis.admin", true);
		}
		player.update(); 
	}
	
	@EventHandler
    public void onServerKickEvent(ServerKickEvent ev) {
		Server server = ev.getPlayer().getServer();
		if (server == null)
			return;
		if (!server.getInfo().getName().equalsIgnoreCase("lobby")) {
			ev.setCancelled(true);
			ev.setCancelServer(getProxy().getServerInfo("lobby"));
			ev.getPlayer().sendMessage(ev.getKickReason());
		}
	}
}
