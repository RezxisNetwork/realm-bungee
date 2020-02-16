package net.rezxis.mchosting.bungee;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.ServerPing.Protocol;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ServerKickEvent;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.event.ServerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.rezxis.mchosting.bungee.commands.BuyRewardCommand;
import net.rezxis.mchosting.bungee.commands.GTellCommand;
import net.rezxis.mchosting.bungee.commands.HubCommand;
import net.rezxis.mchosting.bungee.commands.LobbyCommand;
import net.rezxis.mchosting.bungee.commands.PayCommand;
import net.rezxis.mchosting.bungee.commands.PingCommand;
import net.rezxis.mchosting.bungee.commands.ReportCommand;
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
	public ArrayList<String> messages = new ArrayList<>();
	public ArrayList<UUID> inspection = new ArrayList<>();
	public RestHighLevelClient rcl = null;
	private static final RequestOptions COMMON_OPTIONS;
	public boolean logging = true;
	
	private static ActionListener<IndexResponse> listener = new ActionListener<IndexResponse>() {
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
        build.addHeader("Authorization", "Basic cmV6eGlzOktudFN5a3JsWHlBY1B0T0o=");
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
		rcl = new RestHighLevelClient(RestClient.builder(new HttpHost("96.44.162.140",9200,"http")));
		getProxy().getPluginManager().registerCommand(this, new RezxisCommand());
		getProxy().getPluginManager().registerCommand(this, new PayCommand());
		getProxy().getPluginManager().registerCommand(this, new HubCommand());
		getProxy().getPluginManager().registerCommand(this, new LobbyCommand());
		getProxy().getPluginManager().registerCommand(this, new PingCommand());
		getProxy().getPluginManager().registerCommand(this, new BuyRewardCommand());
		getProxy().getPluginManager().registerCommand(this, new VoteCommand());
		getProxy().getPluginManager().registerCommand(this, new GTellCommand());
		getProxy().getPluginManager().registerCommand(this, new ReportCommand());
		messages.add(ChatColor.GREEN+"一日一回気に入ったレールムに投票しよう！"+ChatColor.AQUA+" 投票したいサーバーに入って/vote");
		messages.add(ChatColor.GREEN+"公式Discordに参加して、最新情報をゲットしよう！ "+ChatColor.AQUA+"https://discord.gg/3E6BvNY");
		messages.add(ChatColor.GREEN+"JMSに投票して報酬をゲットしよう！ "+ChatColor.AQUA+" https://minecraft.jp/servers/play.rezxis.net/vote");
		messages.add(ChatColor.GREEN+"Ticketからの報告以外で、"+ChatColor.AQUA+"/report <内容>で報告が可能です");
		messages.add(ChatColor.GREEN+"/gtell <相手> <内容> でサーバーを超えて個人chatができます。");
		messages.add(ChatColor.GOLD+"mystery box 星4,5を開けたい？ https://stoe.rezxis.net からサポーターを買おう ！ ");
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
		this.getProxy().getScheduler().schedule(this, new AnnounceTask(), 1, min, TimeUnit.MINUTES);
		this.getProxy().getScheduler().schedule(this, new RewardTask(), 1, 15, TimeUnit.MINUTES);
	}
	
	@EventHandler
    public void onPM(PluginMessageEvent event) {
		
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
                sdf.setTimeZone(TimeZone.getTimeZone("JST"));
                XContentBuilder builder = XContentFactory.jsonBuilder();
                builder.startObject();
                {
                    builder.timeField("timestamp", sdf.format(System.currentTimeMillis()));
                    builder.field("player", ((ProxiedPlayer)event.getSender()).getName());
                    builder.field("server", ((ProxiedPlayer)event.getSender()).getServer().getInfo().getName());
                    builder.field("content", event.getMessage());
                    builder.field("ip", ((ProxiedPlayer)event.getSender()).getAddress().getAddress().getHostAddress());
                    builder.field("message", ((ProxiedPlayer)event.getSender()).getName() + " (" + ((ProxiedPlayer)event.getSender()).getServer().getInfo().getName() + "): " + event.getMessage());
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
}
