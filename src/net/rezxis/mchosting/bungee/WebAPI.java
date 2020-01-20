package net.rezxis.mchosting.bungee;

import java.io.IOException;

import com.google.gson.Gson;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class WebAPI {

	private static OkHttpClient client;
	private static Gson gson = new Gson();
	
	static {
		client = new OkHttpClient.Builder().build();
	}
	
	public static McuaResponse checkIP(String ip) throws IOException {
		String url = "https://api.mcua.net/checkip/"+ip;
		Response response = client.newCall(new Request.Builder().url(url).get().build()).execute();
		McuaResponse rs = gson.fromJson(response.body().string(), McuaResponse.class);
		return rs;
	}
	
	public static void webhook(String name, String contents) throws IOException {
		String url = "https://discordapp.com/api/webhooks/668590844609167363/lhRTzgcVulx2ulTyg-RRjcL8kxhEJ1tD4qz50DRzr_Vm9O5npXSOaBjT_d1IuVy5MvtA";
		@SuppressWarnings("deprecation")
		Request request = new Request.Builder().url(url).addHeader("User-Agent", "Rezxis")
				.post(RequestBody.create(MediaType.parse("application/JSON; charset=utf-8"), new Gson().toJson(new DiscordWebHookRequest(name,"https://i.gyazo.com/141e75149b5cfe462af38d922027043f.png",contents)))).build();
		client.newCall(request).execute();
	}
	
	public class McuaResponse {
		private String ip;
		private String bad;
		private String type;
		private String country;
		
		public McuaResponse (String ip, String bad, String type, String country) {
			this.ip = ip;
			this.bad = bad;
			this.type = type;
			this.country = country;
		}
		
		public String getIP() {
			return this.ip;
		}
		
		public boolean isBad() {
			return Boolean.valueOf(bad);
		}
		
		public String getType() {
			return this.type;
		}
		
		public String getCountry() {
			return this.country;
		}
	}
}
