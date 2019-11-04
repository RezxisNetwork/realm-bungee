package net.rezxis.mchosting.bungee;

import java.net.URI;

import net.md_5.bungee.api.plugin.Plugin;
import net.rezxis.mchosting.network.WSClient;

public class Bungee extends Plugin {

	public static Bungee instance;
	public WSClient ws;
	public Props props;
	
	public void onEnable() {
		instance = this;
		props = new Props("hosting.propertis");
		new Thread(()->{
				try {
					ws = new WSClient(new URI(props.SYNC_ADDRESS), new WSClientHandler());
				} catch (Exception e) {
					e.printStackTrace();
				}
				ws.connect();
			
		}).start();
	}
}
