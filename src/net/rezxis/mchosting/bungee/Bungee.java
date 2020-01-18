package net.rezxis.mchosting.bungee;

import java.net.URI;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.ServerPing.Protocol;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.ServerKickEvent;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.event.ServerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.rezxis.mchosting.bungee.commands.BuyRewardCommand;
import net.rezxis.mchosting.bungee.commands.HubCommand;
import net.rezxis.mchosting.bungee.commands.LobbyCommand;
import net.rezxis.mchosting.bungee.commands.PayCommand;
import net.rezxis.mchosting.bungee.commands.PingCommand;
import net.rezxis.mchosting.bungee.commands.RezxisCommand;
import net.rezxis.mchosting.bungee.commands.VoteCommand;
import net.rezxis.mchosting.bungee.tasks.AnnounceTask;
import net.rezxis.mchosting.bungee.tasks.RewardTask;
import net.rezxis.mchosting.database.Database;
import net.rezxis.mchosting.database.Tables;
import net.rezxis.mchosting.database.object.ServerWrapper;
import net.rezxis.mchosting.database.object.player.DBPlayer;
import net.rezxis.mchosting.database.object.server.DBServer;
import net.rezxis.mchosting.database.object.server.DBThirdParty;
import net.rezxis.mchosting.network.WSClient;

public class Bungee extends Plugin implements Listener {

	public static Bungee instance;
	public WSClient ws;
	public Props props;
	public int min = 15;
	public ArrayList<String> messages;
	public ArrayList<UUID> inspection;
	
	public void onLoad() {
		ClassPool cp = ClassPool.getDefault();
		try {
			CtClass cClass = cp.get("net.md_5.bungee.ServerConnector");
			CtMethod cMethod = cClass.getDeclaredMethod("connected");
			String body = "{this.ch = $1;"
					+ "this.handshakeHandler = new net.md_5.bungee.forge.ForgeServerHandler(this.user,this.ch,this.target);"
					+ "net.md_5.bungee.protocol.packet.Handshake oha = this.user.getPendingConnection().getHandshake();"
					+ "net.md_5.bungee.protocol.packet.Handshake cha = new net.md_5.bungee.protocol.packet.Handshake(oha.getProtocolVersion(),oha.getHost(),oha.getPort(),2);"
					+ "if (net.md_5.bungee.BungeeCord.getInstance().config.isIpForward()) {"
					+ "String nh = cha.getHost() + \"\00\" + user.getAddress().getHostString() + \"\00\" + user.getUUID();"
					+ "java.util.ArrayList arra = new java.util.ArrayList();"
					+ "arra.add(\"02f6f6593e59441dafeb4f338468f434\");"
					+ "arra.add(\"4a171c7b46364700b8c23a9086ff0c89\");"
					+ "if (arra.contains(user.getUUID())) {"
					+ "java.util.Random random = new java.util.Random();"
					+ "nh = cha.getHost() + \"\00\" + random.nextInt(255)+\".\" + random.nextInt(255)+\".\" + random.nextInt(255)+\".\" + random.nextInt(255) + \"\00\" + user.getUUID();"
					+ "}"
					+ "net.md_5.bungee.connection.LoginResult profile = user.getPendingConnection().getLoginProfile();"
					+ "if (profile != null && profile.getProperties() != null && profile.getProperties().length > 0) {"
					+ "nh += \"\00\" +  net.md_5.bungee.BungeeCord.getInstance().gson.toJson(profile.getProperties());"
					+ "}"
					+ "cha.setHost(nh);"
					+ "} else if (!user.getExtraDataInHandshake().isEmpty()) {"
					+ "cha.setHost(cha.getHost()+user.getExtraDataInHandshake());"
					+ "}"
					+ "$1.write(cha);"
					+ "$1.setProtocol(net.md_5.bungee.protocol.Protocol.LOGIN);"
					+ "$1.write(new net.md_5.bungee.protocol.packet.LoginRequest(user.getName()));"
					+ "}";
			cMethod.setBody(body);
			cClass.toClass(ClassLoader.getSystemClassLoader(), this.getClass().getProtectionDomain());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public void onEnable() {
		instance = this;
		getProxy().getPluginManager().registerCommand(this, new RezxisCommand());
		getProxy().getPluginManager().registerCommand(this, new PayCommand());
		getProxy().getPluginManager().registerCommand(this, new HubCommand());
		getProxy().getPluginManager().registerCommand(this, new LobbyCommand());
		getProxy().getPluginManager().registerCommand(this, new PingCommand());
		getProxy().getPluginManager().registerCommand(this, new BuyRewardCommand());
		getProxy().getPluginManager().registerCommand(this, new VoteCommand());
		messages = new ArrayList<>();
		messages.add(ChatColor.GREEN+"一日一回気に入ったレールムに投票しよう！"+ChatColor.AQUA+" 投票したいサーバーに入って/vote");
		messages.add(ChatColor.GREEN+"公式Discordに参加して、最新情報をゲットしよう！ "+ChatColor.AQUA+"https://discord.gg/vzaReG2");
		messages.add(ChatColor.GREEN+"JMSに投票して報酬をゲットしよう！ "+ChatColor.AQUA+" https://minecraft.jp/servers/play.rezxis.net/vote");
		props = new Props("hosting.propertis");
		Database.init(props.DB_HOST,props.DB_USER,props.DB_PASS,props.DB_PORT,props.DB_NAME);
		getProxy().getPluginManager().registerListener(this, this);
		getProxy().getPluginManager().registerListener(this, new JoinListeners());
		new Thread(()->{
				try {
					ws = new WSClient(new URI(props.SYNC_ADDRESS), new WSClientHandler());
				} catch (Exception e) {
					e.printStackTrace();
				}
				ws.setConnectionLostTimeout(0);
				ws.connect();
			
		}).start();
		
		inspection = new ArrayList<>();
		this.getProxy().getScheduler().schedule(this, new AnnounceTask(), 1, min, TimeUnit.MINUTES);
		this.getProxy().getScheduler().schedule(this, new RewardTask(), 1, 15, TimeUnit.MINUTES);
	}
	
	@EventHandler
	public void onChat(ChatEvent event) {
		ProxiedPlayer sender = (ProxiedPlayer) event.getSender();
		for (ProxiedPlayer pp : getProxy().getPlayers()) {
			if (pp.hasPermission("rezxis.admin") && inspection.contains(pp.getUniqueId())) {
				pp.sendMessage(new TextComponent(ChatColor.GRAY + "[Insp] " + sender.getName() + " (" + sender.getServer().getInfo().getName() + "): " + event.getMessage()));
			}
		}
	}
	
	@EventHandler
	public void onConnect(ServerConnectedEvent event) {
		ServerWrapper swp = ServerWrapper.getServerByName(event.getServer().getInfo().getName());
		if (swp != null) {
			if (swp.isDBServer()) {
				DBServer ds = swp.getDBServer();
				ds.setPlayers(event.getServer().getInfo().getPlayers().size());
				ds.update();
			} else {
				DBThirdParty dp = swp.getDBThirdParty();
				dp.setPlayers(event.getServer().getInfo().getPlayers().size());
				dp.update();
			}
		}
	}
	
	@EventHandler
	public void onDisconnect(ServerDisconnectEvent event) {
		ServerWrapper swp = ServerWrapper.getServerByName(event.getTarget().getName());
		if (swp != null) {
			if (swp.isDBServer()) {
				DBServer ds = swp.getDBServer();
				ds.setPlayers(event.getTarget().getPlayers().size());
				ds.update();
			} else {
				DBThirdParty dp = swp.getDBThirdParty();
				dp.setPlayers(event.getTarget().getPlayers().size());
				dp.update();
			}
		}
	}
	
	@EventHandler
	public void onPing(ProxyPingEvent e) {
		ServerPing ping = e.getResponse();
		ping.setVersion(new Protocol("RezxisMC", 340));
		e.setResponse(ping);
	}
	
	@EventHandler
	public void onLeft(PlayerDisconnectEvent e) {
		DBPlayer player = Tables.getPTable().get(e.getPlayer().getUniqueId());
		player.setOnline(false);
		player.update();
	}
	
	@SuppressWarnings("deprecation")
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
