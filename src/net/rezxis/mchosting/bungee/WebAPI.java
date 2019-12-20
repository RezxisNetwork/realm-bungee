package net.rezxis.mchosting.bungee;

import java.io.IOException;

import com.google.gson.Gson;

import okhttp3.OkHttpClient;
import okhttp3.Request;
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
