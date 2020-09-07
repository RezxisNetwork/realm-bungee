package net.rezxis.mchosting.bungee;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpHost;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.ServerPing.Protocol;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
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
import net.md_5.bungee.conf.Configuration;
import net.md_5.bungee.event.EventHandler;
import net.rezxis.mchosting.bungee.commands.BuyRewardCommand;
import net.rezxis.mchosting.bungee.commands.GTellCommand;
import net.rezxis.mchosting.bungee.commands.HubCommand;
import net.rezxis.mchosting.bungee.commands.LinkCommand;
import net.rezxis.mchosting.bungee.commands.LobbyCommand;
import net.rezxis.mchosting.bungee.commands.PayCommand;
import net.rezxis.mchosting.bungee.commands.PingCommand;
import net.rezxis.mchosting.bungee.commands.ReportCommand;
import net.rezxis.mchosting.bungee.commands.RezxisCommand;
import net.rezxis.mchosting.bungee.commands.ServerCommand;
import net.rezxis.mchosting.bungee.commands.VoteCommand;
import net.rezxis.mchosting.bungee.tasks.AnnounceTask;
import net.rezxis.mchosting.bungee.tasks.RewardTask;
import net.rezxis.mchosting.database.Tables;
import net.rezxis.mchosting.database.object.HostName;
import net.rezxis.mchosting.database.object.ServerWrapper;
import net.rezxis.mchosting.database.object.player.DBPlayer;
import net.rezxis.mchosting.database.object.server.DBThirdParty;
import net.rezxis.mchosting.network.WSClient;

public class Bungee extends Plugin implements Listener {

	public static Bungee instance;
	public WSClient ws;
	public Props props;
	public int min = 15;
	public ArrayList<TextComponent> messages = new ArrayList<>();
	public ArrayList<UUID> inspection = new ArrayList<>();
	public static RestHighLevelClient rcl = null;
	public static final RequestOptions COMMON_OPTIONS;
	public boolean logging = true;
	
	public static ActionListener<IndexResponse> listener = new ActionListener<IndexResponse>() {
        @Override
        public void onResponse(IndexResponse indexResponse) {
            return;
        }

        @Override
        public void onFailure(Exception e) {
            e.printStackTrace();
        }
    };
    
    static {
        RequestOptions.Builder build = RequestOptions.DEFAULT.toBuilder();
        COMMON_OPTIONS = build.build();
    }
	
	public void onLoad() {
		ClassPool cp = ClassPool.getDefault();
		try {
			CtClass cClass = cp.get("net.md_5.bungee.ServerConnector");
			CtMethod cMethod = cClass.getDeclaredMethod("connected");
			CtField f1 = CtField.make("static java.util.ArrayList arra = new java.util.ArrayList();", cClass);
			cClass.addField(f1);
			String body = "{this.ch = $1;"
					+ "this.handshakeHandler = new net.md_5.bungee.forge.ForgeServerHandler(this.user,this.ch,this.target);"
					+ "net.md_5.bungee.protocol.packet.Handshake oha = this.user.getPendingConnection().getHandshake();"
					+ "net.md_5.bungee.protocol.packet.Handshake cha = new net.md_5.bungee.protocol.packet.Handshake(oha.getProtocolVersion(),oha.getHost(),oha.getPort(),2);"
					+ "if (net.md_5.bungee.BungeeCord.getInstance().config.isIpForward()) {"
					+ "String nh = cha.getHost() + \"\00\" + user.getAddress().getHostString() + \"\00\" + user.getUUID();"
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
		rcl = new RestHighLevelClient(RestClient.builder(new HttpHost("192.168.0.1",9200,"http")));
		getProxy().getPluginManager().registerCommand(this, new RezxisCommand());
		getProxy().getPluginManager().registerCommand(this, new PayCommand());
		getProxy().getPluginManager().registerCommand(this, new HubCommand());
		getProxy().getPluginManager().registerCommand(this, new LobbyCommand());
		getProxy().getPluginManager().registerCommand(this, new PingCommand());
		getProxy().getPluginManager().registerCommand(this, new BuyRewardCommand());
		getProxy().getPluginManager().registerCommand(this, new VoteCommand());
		getProxy().getPluginManager().registerCommand(this, new GTellCommand());
		getProxy().getPluginManager().registerCommand(this, new ReportCommand());
		getProxy().getPluginManager().registerCommand(this, new LinkCommand());
		getProxy().getPluginManager().registerCommand(this, new ServerCommand());
		messages.add(new TextComponent(ChatColor.GREEN+"一日一回気に入ったレールムに投票しよう！"+ChatColor.AQUA+" 投票したいサーバーに入って/vote"));
		messages.add(new TextComponent(ChatColor.GREEN+"公式Discordに参加して、最新情報をゲットしよう！ "+ChatColor.AQUA+"https://discord.gg/3E6BvNY"));
		messages.add(new TextComponent(ChatColor.GREEN+"JMSに投票して報酬をゲットしよう！ "+ChatColor.AQUA+" https://minecraft.jp/servers/play.rezxis.net/vote"));
		messages.add(new TextComponent(ChatColor.AQUA+"現在の自分の投票ステータス /voteinfo"));
		messages.add(new TextComponent(ChatColor.GREEN+"Ticketからの報告以外で、"+ChatColor.AQUA+"/report <内容>で報告が可能です"));
		messages.add(new TextComponent(ChatColor.GREEN+"/gtell <相手> <内容> でサーバーを超えて個人chatができます。"));
		messages.add(new TextComponent(ChatColor.GOLD+"mystery box 星4,5を開けたい？ https://store.rezxis.net からサポーターを買おう ！ "));
		TextComponent twitter = new TextComponent(ChatColor.GREEN+"公式ツイッター @rezxis_net");
		twitter.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://twitter.com/rezxis_net"));
		messages.add(twitter);
		
		props = new Props("hosting.propertis");
		//Database.init(props.DB_HOST,props.DB_USER,props.DB_PASS,props.DB_PORT,props.DB_NAME);
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
		Runtime.getRuntime().addShutdownHook(new Thread(()->{
			if (!ws.isClosed()) {
				ws.close();
			}
		}));
		this.getProxy().getScheduler().schedule(this, new AnnounceTask(), 1, min, TimeUnit.MINUTES);
		this.getProxy().getScheduler().schedule(this, new RewardTask(), 1, 15, TimeUnit.MINUTES);
		reloadServers();
		ServerManager.reloadForcesHost();
		ServerManager.reloadServers();
	}
	
	private void reloadServers() {
		ArrayList<ServerWrapper> wrapper = ServerWrapper.getServers(true, null, false);
		Map<String,ServerInfo> servers = ProxyServer.getInstance().getServers();
		for (ServerWrapper wrap : wrapper) {
			servers.put(wrap.getDisplayName(), 
					ProxyServer.getInstance().constructServerInfo(wrap.getDisplayName(), new InetSocketAddress(wrap.getAddress(), wrap.getPort()), wrap.getDisplayName(), false));
		}
		try {
			Field field = Configuration.class.getDeclaredField("servers");
			field.setAccessible(true);
			field.set(BungeeCord.getInstance().config, servers);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	@EventHandler
	public void onChat(ChatEvent event) {
		ProxiedPlayer sender = (ProxiedPlayer) event.getSender();
		for (ProxiedPlayer pp : getProxy().getPlayers()) {
			if (pp.hasPermission("rezxis.admin") && inspection.contains(pp.getUniqueId())) {
				pp.sendMessage(new TextComponent(ChatColor.GRAY + "[Insp] " + sender.getName() + " (" + sender.getServer().getInfo().getName() + ") "+ChatColor.GRAY+": " + event.getMessage()));
			}
		}
        if (logging) {
        	try {
            	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                XContentBuilder builder = XContentFactory.jsonBuilder();
                builder.startObject();
                {
                    builder.timeField("@timestamp", sdf.format(System.currentTimeMillis()));
                    builder.field("player", ((ProxiedPlayer)event.getSender()).getName());
                    builder.field("server", ((ProxiedPlayer)event.getSender()).getServer().getInfo().getName());
                    
                    builder.field("content", event.getMessage());
                    builder.field("message", ((ProxiedPlayer)event.getSender()).getName() + " (" + ((ProxiedPlayer)event.getSender()).getServer().getInfo().getName() + "): " + event.getMessage());
                    
                    //builder.field("message", event.getMessage());
                    builder.field("name", "inspection");
                    
                    builder.field("ip", ((ProxiedPlayer)event.getSender()).getAddress().getAddress().getHostAddress());
                }
                builder.endObject();
                IndexRequest request = new IndexRequest("inspection").source(builder);
                rcl.indexAsync(request, COMMON_OPTIONS, listener);
            } catch (Exception ex) {
            	ex.printStackTrace();
            }
        }
	}
	
	@EventHandler
	public void onConnect(ServerConnectedEvent event) {
		ServerWrapper swp = ServerWrapper.getServerByName(event.getServer().getInfo().getName());
		if (swp != null) {
			if (!swp.isDBServer()) {
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
			if (!swp.isDBServer()) {
				DBThirdParty dp = swp.getDBThirdParty();
				dp.setPlayers(event.getTarget().getPlayers().size());
				dp.update();
			}
		}
	}
	
	@EventHandler
	public void onPing(ProxyPingEvent e) {
		ServerPing eping = e.getResponse();
		eping.setVersion(new Protocol("RezxisMC", 340));
		e.setResponse(eping);
		String s = null;
		try {
			String hostname = e.getConnection().getVirtualHost().getHostName();
			if (hostname == null)
				return;
			HostName hn = Tables.getRezxisHostTable().get(hostname);
			if (hn == null)
				return;
			s = hn.getDest();
			if (!hn.isPing())
				return;
		} catch (Exception ex) {}
		if (s != null) {
			ServerInfo info = BungeeCord.getInstance().getServerInfo(s);
			if (info != null) {
				int i = PingCallBack.idd;
				PingCallBack.idd += 1;
				info.ping(new PingCallBack(i));
				long time = System.currentTimeMillis();
				boolean timeouted = false;
				while (!PingCallBack.puted.get(i)) {
					if (System.currentTimeMillis() - time > 1000) {
						PingCallBack.puted.put(i, true);
						timeouted = true;
					}
				}
				Object obj = PingCallBack.pings.get(i);
				if (obj instanceof Throwable) {
					PingCallBack.pings.remove(i);
					PingCallBack.puted.remove(i);
					timeouted = true;
				}
				if (timeouted) {
					ServerPing ping = e.getResponse();
					TextComponent tc = new TextComponent("Couldn't Sync to server. maybe server is offline");
					tc.setColor(ChatColor.RED);
					ping.setDescriptionComponent(tc);
					return;
				}
				ServerPing p1 = (ServerPing) PingCallBack.pings.get(i);
				ServerPing ping = e.getResponse();
				ping.setDescriptionComponent(p1.getDescriptionComponent());
				ping.setFavicon(p1.getFaviconObject());
				ping.setPlayers(p1.getPlayers());
				ping.setVersion(p1.getVersion());
				e.setResponse(ping);
				PingCallBack.pings.remove(i);
				PingCallBack.puted.remove(i);
			}
		}
	}
	
	@EventHandler
	public void onLeft(PlayerDisconnectEvent e) {
		DBPlayer player = Tables.getPTable().get(e.getPlayer().getUniqueId());
		player.setOnline(false);
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
			ev.getPlayer().sendMessage(new TextComponent(ev.getKickReasonComponent()));
		}
	}
	
	public static class PingCallBack implements Callback<ServerPing> {
		
		public static HashMap<Integer, Object> pings = new HashMap<>();
		public static HashMap<Integer, Boolean> puted = new HashMap<>();
		public static int idd = 0;
		
		public int lid;
		
		public PingCallBack(int id) {
			lid = id;
			puted.put(lid, false);
		}
		
		@Override
		public void done(ServerPing ping, Throwable ex) {
			if (puted.getOrDefault(lid, false)) {
				return;
			}
			if (ex != null) {
				ex.printStackTrace();
				pings.put(lid, ex);
			} else {
				pings.put(lid, ping);
			}
			puted.put(lid, true);
		}
	}
}
