package net.rezxis.mchosting.bungee;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.Date;

import io.netty.channel.AbstractChannel;
import io.netty.channel.Channel;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerHandshakeEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.rezxis.mchosting.bungee.WebAPI.McuaResponse;
import net.rezxis.mchosting.database.Tables;
import net.rezxis.mchosting.database.object.player.DBIP;
import net.rezxis.mchosting.database.object.player.DBPIP;
import net.rezxis.mchosting.database.object.player.DBPlayer;
import net.rezxis.mchosting.database.object.player.DBUUID;
import net.rezxis.mchosting.database.object.player.DBPlayer.Rank;

public class JoinListeners implements Listener {

	@SuppressWarnings("deprecation")
	@EventHandler
	public void onJoin(LoginEvent e) {
		DBPlayer player = Tables.getPTable().get(e.getConnection().getUniqueId());
		if (player == null) {
			player = new DBPlayer(-1, e.getConnection().getUniqueId(), Rank.NORMAL, 0, false, new Date(), new Date(), true, false ,"",false,false,new Date(),"",0);
			Tables.getPTable().insert(player);
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
		DBIP dbip = Tables.getIpTable().get(ip);
		if (dbip == null) {
			dbip = new DBIP(-1,ip,false,"",new Date());
			Tables.getIpTable().insert(dbip);
		}
		if (dbip.isBanned()) {
			e.setCancelled(true);
			e.setCancelReason(dbip.getReason());
			return;
		}
		DBPIP dbpip = Tables.getPipTable().getFromIPPlayer(dbip.getId(),player.getId());
		if (dbpip == null) {
			dbpip = new DBPIP(-1,dbip.getId(),player.getId());
			Tables.getPipTable().insert(dbpip);
		}
		DBUUID dbuid = Tables.getUTable().get(e.getConnection().getUniqueId());
		if (dbuid == null) {
			dbuid = new DBUUID(-1,e.getConnection().getName(), e.getConnection().getUniqueId());
			Tables.getUTable().insert(dbuid);
		} else if (!dbuid.getName().equals(e.getConnection().getName())) {
			dbuid.setName(e.getConnection().getName());
		}
	}
	
	@EventHandler
	public void onJoin(PostLoginEvent e) {
		DBPlayer player = Tables.getPTable().get(e.getPlayer().getUniqueId());
		Rank rank = player.getRank();
		if (rank == Rank.STAFF | rank == Rank.DEVELOPER | rank == Rank.OWNER) {
			e.getPlayer().setPermission("rezxis.admin", true);
		}
		if (rank == Rank.DEVELOPER) {
			e.getPlayer().setPermission("rezxis.rank", true);
		} else {
			e.getPlayer().setPermission("rezxis.rank", false);
		}
		player.update(); 
	}
	
	@EventHandler
	public void onHandShake(PlayerHandshakeEvent event) {
	}
}
