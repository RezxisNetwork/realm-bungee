package net.rezxis.mchosting.bungee;

import java.net.InetSocketAddress;
import java.util.HashMap;

import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.config.ServerInfo;
import net.rezxis.mchosting.database.Tables;
import net.rezxis.mchosting.database.object.HostName;
import net.rezxis.mchosting.database.object.server.BungeeCordServer;

public class ServerManager {

	private static HashMap<Integer, ServerInfo> servers = new HashMap<>();
	
	public static void reloadServers() {
		for (BungeeCordServer bcs : Tables.getBungeeCordServersTable().getAll()) {
			if (servers.containsKey(bcs.getId())) {
				ServerInfo target = servers.get(bcs.getId());
				BungeeCord.getInstance().getServers().remove(target.getName());
				servers.remove(bcs.getId());
			}
			ServerInfo info = BungeeCord.getInstance().constructServerInfo(bcs.getName(),
					InetSocketAddress.createUnresolved(bcs.getAddress(), bcs.getPort()),
					bcs.getMotd(), bcs.isRestricted());
			servers.put(bcs.getId(), info);
			BungeeCord.getInstance().getServers().put(bcs.getName(), info);
		}
	}

	public static void reloadForcesHost() {
		ListenerInfo li = BungeeCord.getInstance().config.getListeners().iterator().next();
		li.getForcedHosts().clear();
		for (HostName hn : Tables.getRezxisHostTable().getAll()) {
			li.getForcedHosts().put(hn.getHost(), hn.getDest());
		}
	}
}
