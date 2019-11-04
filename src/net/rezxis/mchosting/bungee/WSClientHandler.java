package net.rezxis.mchosting.bungee;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.java_websocket.handshake.ServerHandshake;

import com.google.gson.Gson;

import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.conf.Configuration;
import net.rezxis.mchosting.network.ClientHandler;
import net.rezxis.mchosting.network.packet.Packet;
import net.rezxis.mchosting.network.packet.PacketType;
import net.rezxis.mchosting.network.packet.ServerType;
import net.rezxis.mchosting.network.packet.bungee.BungPlayerSendPacket;
import net.rezxis.mchosting.network.packet.bungee.BungServerStarted;
import net.rezxis.mchosting.network.packet.bungee.BungServerStopped;
import net.rezxis.mchosting.network.packet.sync.SyncAuthSocketPacket;

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
		if (type == PacketType.ServerStarted) {
			BungServerStarted signal = gson.fromJson(message, BungServerStarted.class);
			ServerInfo serverInfo = ProxyServer.getInstance().constructServerInfo(signal.displayName, new InetSocketAddress("127.0.0.1", signal.port), signal.displayName, false);
			try {
				Map<String,ServerInfo> servers = ProxyServer.getInstance().getServers();
				servers.put(signal.displayName, serverInfo);
				Field field = Configuration.class.getDeclaredField("servers");
				field.setAccessible(true);
				field.set(BungeeCord.getInstance().config, servers);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		} else if (type == PacketType.ServerStopped) {
			BungServerStopped signal = gson.fromJson(message, BungServerStopped.class);
			for (Entry<String,ServerInfo> entry : ProxyServer.getInstance().getServers().entrySet()) {
				if (entry.getValue().getAddress().getPort() == signal.port) {
					try {
						Map<String,ServerInfo> servers = ProxyServer.getInstance().getServers();
						servers.remove(entry.getKey());
						Field field = Configuration.class.getDeclaredField("servers");
						field.setAccessible(true);
						field.set(BungeeCord.getInstance().config, servers);
					} catch(Exception ex) {
						ex.printStackTrace();
					}
				}
			}
		} else if (type == PacketType.PlayerSendPacket) {
			BungPlayerSendPacket signal = gson.fromJson(message, BungPlayerSendPacket.class);
			ProxiedPlayer p = BungeeCord.getInstance().getPlayer(UUID.fromString(signal.player));
			p.connect(BungeeCord.getInstance().getServerInfo(signal.server));
		}
	}

	@Override
	public void onClose(int code, String reason, boolean remote) {
		System.out.println("closed / code : "+code+" / reason : "+reason+" / remote : "+remote);
	}

	@Override
	public void onMessage(ByteBuffer buffer) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onError(Exception ex) {
		ex.printStackTrace();
	}

}
