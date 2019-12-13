package net.rezxis.mchosting.bungee;

import java.net.URI;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerKickEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.rezxis.mchosting.database.DBPlayer;
import net.rezxis.mchosting.database.Database;
import net.rezxis.mchosting.database.DBPlayer.Rank;
import net.rezxis.mchosting.database.tables.PlayersTable;
import net.rezxis.mchosting.network.WSClient;

public class Bungee extends Plugin implements Listener {

	public static Bungee instance;
	public WSClient ws;
	public Props props;
	public int min = 15;
	public PlayersTable pTable;
	
	public void onEnable() {
		instance = this;
		Database.init();
		pTable = new PlayersTable();
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
				for (ProxiedPlayer player : getProxy().getPlayers()) {
					player.sendMessage(ChatColor.GREEN+"一日一回気に入ったレールムに投票しよう！"+ChatColor.AQUA+" /vote <投票対象サーバーのオーナー名>");
					player.sendMessage(ChatColor.GREEN+"公式Discordに参加して、最新情報をゲットしよう！ "+ChatColor.AQUA+"https://discord.gg/QAskk72");
					player.sendMessage(ChatColor.GREEN+"JMSに投票して報酬をゲットしよう！ "+ChatColor.AQUA+" https://minecraft.jp/servers/play.rezxis.net/vote");
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
	public void onJoin(PostLoginEvent e) {
		DBPlayer player = pTable.get(e.getPlayer().getUniqueId());
		if (player == null) {
			player = new DBPlayer(-1, e.getPlayer().getUniqueId(), Rank.NORMAL, 0, false, new Date(), new Date(), true);
			pTable.insert(player);
		} else {
			player.setOnline(true);
			player.update();
		}
		
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
