package net.rezxis.mchosting.bungee;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map.Entry;

import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.config.ServerInfo;
import net.rezxis.mchosting.database.Tables;
import net.rezxis.mchosting.database.object.HostName;
import net.rezxis.mchosting.database.object.server.BungeeCordServer;
import net.rezxis.mchosting.database.object.server.DBServer;

public class ServerManager {

	private static HashMap<Integer, ServerInfo> servers = new HashMap<>();
	private static HashMap<Integer, String> realms = new HashMap<>();
	//private static HashMap<String, ServerInfo> realmServers = new HashMap<>();
	
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
		for (Entry<Integer, ServerInfo> e : servers.entrySet()) {
			BungeeCordServer s = Tables.getBungeeCordServersTable().getServerFromID(e.getKey());
			if (s == null) {
				BungeeCord.getInstance().getServers().remove(e.getValue().getName());
			}
		}
	}

	public static void reloadForcesHost() {
		ListenerInfo li = BungeeCord.getInstance().config.getListeners().iterator().next();
		li.getForcedHosts().clear();
		for (Entry<Integer, String> data : realms.entrySet()) {
			li.getForcedHosts().put(data.getValue()+".direct.rezxis.net", Tables.getSTable().getByID(data.getKey()).getDisplayName());
		}
		for (HostName hn : Tables.getRezxisHostTable().getAll()) {
			li.getForcedHosts().put(hn.getHost(), hn.getDest());
		}
	}
	
	public static void started(DBServer server) {
		realms.put(server.getId(), server.getDirect());
		//realmServers.put(server.getDirect()+".direct.rezxis.net", BungeeCord.getInstance().getServerInfo(server.getDisplayName()));
		reloadForcesHost();
	}
	
	public static void stopped(DBServer server) {
		realms.remove(server.getId());
		reloadForcesHost();
	}
}
