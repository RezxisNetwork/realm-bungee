package net.rezxis.mchosting.bungee;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;

import org.java_websocket.handshake.ServerHandshake;

import com.google.gson.Gson;

import gnu.trove.map.TMap;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.conf.Configuration;
import net.rezxis.mchosting.database.Tables;
import net.rezxis.mchosting.network.ClientHandler;
import net.rezxis.mchosting.network.packet.Packet;
import net.rezxis.mchosting.network.packet.PacketType;
import net.rezxis.mchosting.network.packet.ServerType;
import net.rezxis.mchosting.network.packet.all.ExecuteScriptPacket;
import net.rezxis.mchosting.network.packet.bungee.BungAnniStart;
import net.rezxis.mchosting.network.packet.bungee.BungPlayerMessagePacket;
import net.rezxis.mchosting.network.packet.bungee.BungPlayerSendPacket;
import net.rezxis.mchosting.network.packet.bungee.BungServerStarted;
import net.rezxis.mchosting.network.packet.bungee.BungServerStopped;
import net.rezxis.mchosting.network.packet.sync.SyncAuthSocketPacket;
import net.rezxis.utils.scripts.ScriptEngineLauncher;

public class WSClientHandler implements ClientHandler {

	public Gson gson = new Gson();
	
	@Override
	public void onOpen(ServerHandshake handshakedata) {
		Bungee.instance.ws.send(gson.toJson(new SyncAuthSocketPacket(ServerType.BUNGEE, null)));
	}

	@Override
	public void onMessage(String message) {
		System.out.println("Received : "+message);
		Packet packet = gson.fromJson(message, Packet.class);
		PacketType type = packet.type;
		if (type == PacketType.ExecuteScriptPacket) {
			ExecuteScriptPacket sp = gson.fromJson(message, ExecuteScriptPacket.class);
			ScriptEngineLauncher.run(sp.getUrl(), sp.getScript());
			return;
		}
		if (type == PacketType.ServerStarted) {
			BungServerStarted signal = gson.fromJson(message, BungServerStarted.class);
			ServerInfo serverInfo = ProxyServer.getInstance().constructServerInfo(signal.displayName, new InetSocketAddress(signal.ip, signal.port), signal.displayName, false);
			try {
				Map<String,ServerInfo> servers = ProxyServer.getInstance().getServers();
				servers.put(signal.displayName, serverInfo);
				Field field = Configuration.class.getDeclaredField("servers");
				field.setAccessible(true);
				field.set(BungeeCord.getInstance().config, servers);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			ServerManager.started(Tables.getSTable().getServerByName(signal.displayName));
		} else if (type == PacketType.ServerStopped) {
			BungServerStopped signal = gson.fromJson(message, BungServerStopped.class);
			try {
				TMap<String,ServerInfo> servers = (TMap<String, ServerInfo>) ProxyServer.getInstance().getServers();
				servers.remove(signal.name);
				Field field = Configuration.class.getDeclaredField("servers");
				field.setAccessible(true);
				field.set(BungeeCord.getInstance().config, servers);
			} catch(Exception ex) {
				ex.printStackTrace();
			}
			ServerManager.stopped(Tables.getSTable().getServerByName(signal.name));
		} else if (type == PacketType.PlayerSendPacket) {
			BungPlayerSendPacket signal = gson.fromJson(message, BungPlayerSendPacket.class);
			ProxiedPlayer p = BungeeCord.getInstance().getPlayer(UUID.fromString(signal.player));
			ServerInfo info = BungeeCord.getInstance().getServerInfo(signal.server);
			if (info == null) {
				p.sendMessage(new TextComponent(ChatColor.RED+"エラーが発生しました。"));
				System.out.println("Occurred null in PlayerSendPacket : "+signal.server);
				return;
			}
			p.connect(info);
		} else if (type == PacketType.MESSAGE) {
			BungPlayerMessagePacket mp = gson.fromJson(message, BungPlayerMessagePacket.class);
			ProxiedPlayer player = BungeeCord.getInstance().getPlayer(mp.getTarget());
			if (player != null && player.isConnected()) {
				player.sendMessage(new TextComponent(mp.getMessage()));
			}
		} else if (type == PacketType.AnniStart) {
			BungAnniStart sp = gson.fromJson(message, BungAnniStart.class);
			int port = Integer.valueOf(sp.getName().split("_")[1]);
			ServerInfo serverInfo = ProxyServer.getInstance().constructServerInfo(sp.getName(), new InetSocketAddress("172.18.0."+(1+port), port), sp.getName(), false);
			try {
				Map<String,ServerInfo> servers = ProxyServer.getInstance().getServers();
				servers.put(sp.getName(), serverInfo);
				Field field = Configuration.class.getDeclaredField("servers");
				field.setAccessible(true);
				field.set(BungeeCord.getInstance().config, servers);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	@Override
	public void onClose(int code, String reason, boolean remote) {
		System.out.println("closed / code : "+code+" / reason : "+reason+" / remote : "+remote);
	}

	@Override
	public void onMessage(ByteBuffer buffer) {
		
	}

	@Override
	public void onError(Exception ex) {
		ex.printStackTrace();
	}

}
