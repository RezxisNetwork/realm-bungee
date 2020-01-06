package net.rezxis.mchosting.bungee;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.ServerPing.Protocol;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerKickEvent;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.rezxis.mchosting.bungee.WebAPI.McuaResponse;
import net.rezxis.mchosting.bungee.commands.HubCommand;
import net.rezxis.mchosting.bungee.commands.LobbyCommand;
import net.rezxis.mchosting.bungee.commands.PayCommand;
import net.rezxis.mchosting.bungee.commands.PingCommand;
import net.rezxis.mchosting.bungee.commands.RezxisCommand;
import net.rezxis.mchosting.bungee.tasks.AnnounceTask;
import net.rezxis.mchosting.bungee.tasks.RewardTask;
import net.rezxis.mchosting.database.Database;
import net.rezxis.mchosting.database.object.player.DBIP;
import net.rezxis.mchosting.database.object.player.DBPIP;
import net.rezxis.mchosting.database.object.player.DBPlayer;
import net.rezxis.mchosting.database.object.player.DBPlayer.Rank;
import net.rezxis.mchosting.database.object.player.DBUUID;
import net.rezxis.mchosting.database.tables.CrateTable;
import net.rezxis.mchosting.database.tables.IPTable;
import net.rezxis.mchosting.database.tables.PIPTable;
import net.rezxis.mchosting.database.tables.PlayersTable;
import net.rezxis.mchosting.database.tables.ServersTable;
import net.rezxis.mchosting.database.tables.UuidTable;
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
	public UuidTable uTable;
	public CrateTable cTable;
	public ArrayList<String> messages;
	
	public void onEnable() {
		instance = this;
		getProxy().getPluginManager().registerCommand(this, new RezxisCommand());
		getProxy().getPluginManager().registerCommand(this, new PayCommand());
		getProxy().getPluginManager().registerCommand(this, new HubCommand());
		getProxy().getPluginManager().registerCommand(this, new LobbyCommand());
		getProxy().getPluginManager().registerCommand(this, new PingCommand());
		messages = new ArrayList<>();
		messages.add(ChatColor.GREEN+"一日一回気に入ったレールムに投票しよう！"+ChatColor.AQUA+" /vote <投票対象サーバーのオーナー名>");
		messages.add(ChatColor.GREEN+"公式Discordに参加して、最新情報をゲットしよう！ "+ChatColor.AQUA+"https://discord.gg/kzBT6xg");
		messages.add(ChatColor.GREEN+"JMSに投票して報酬をゲットしよう！ "+ChatColor.AQUA+" https://minecraft.jp/servers/play.rezxis.net/vote");
		props = new Props("hosting.propertis");
		Database.init(props.DB_HOST,props.DB_USER,props.DB_PASS,props.DB_PORT,props.DB_NAME);
		pTable = new PlayersTable();
		ipTable = new IPTable();
		sTable = new ServersTable();
		pipTable = new PIPTable();
		uTable = new UuidTable();
		cTable = new CrateTable();
		getProxy().getPluginManager().registerListener(this, this);
		new Thread(()->{
				try {
					ws = new WSClient(new URI(props.SYNC_ADDRESS), new WSClientHandler());
				} catch (Exception e) {
					e.printStackTrace();
				}
				ws.setConnectionLostTimeout(0);
				ws.connect();
			
		}).start();
		
		this.getProxy().getScheduler().schedule(this, new AnnounceTask(), 1, min, TimeUnit.MINUTES);
		this.getProxy().getScheduler().schedule(this, new RewardTask(), 15, TimeUnit.MINUTES);
	}
	
	@EventHandler
	public void onPing(ProxyPingEvent e) {
		ServerPing ping = e.getResponse();
		ping.setVersion(new Protocol("RezxisMC", 340));
		e.setResponse(ping);
	}
	
	@EventHandler
	public void onLeft(PlayerDisconnectEvent e) {
		DBPlayer player = pTable.get(e.getPlayer().getUniqueId());
		player.setOnline(false);
		player.update();
	}
	
	@SuppressWarnings("deprecation")
	@EventHandler
	public void onJoin(LoginEvent e) {
		DBPlayer player = pTable.get(e.getConnection().getUniqueId());
		if (player == null) {
			player = new DBPlayer(-1, e.getConnection().getUniqueId(), Rank.NORMAL, 0, false, new Date(), new Date(), true, false ,"",false);
			pTable.insert(player);
		}
		String ip = e.getConnection().getAddress().getAddress().getHostAddress();
		if (!player.isVpnBypass()) {
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
		}
		player.setOnline(true);
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
		DBUUID dbuid = uTable.get(e.getConnection().getUniqueId());
		if (dbuid == null) {
			dbuid = new DBUUID(-1,e.getConnection().getName(), e.getConnection().getUniqueId());
			uTable.insert(dbuid);
		} else if (!dbuid.getName().equals(e.getConnection().getName())) {
			dbuid.setName(e.getConnection().getName());
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
