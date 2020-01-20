package net.rezxis.mchosting.bungee.commands;

import java.io.IOException;

import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.rezxis.mchosting.bungee.Bungee;
import net.rezxis.mchosting.bungee.WebAPI;

public class ReportCommand extends Command {

	public ReportCommand() {
		super("report");
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		if (args.length == 0) {
			sender.sendMessage(new TextComponent(ChatColor.RED+"Usage : /report <text>"));
			return;
		}
		String message = "";
		for (int i = 0; i < args.length; i++) {
			message += args[i];
		}
		for (ProxiedPlayer pp : BungeeCord.getInstance().getPlayers()) {
			if (pp.hasPermission("rezxis.admin"))
				pp.sendMessage(new TextComponent(ChatColor.GRAY+"[REPORT] - "+ChatColor.RED+sender.getName()+"("+((ProxiedPlayer)sender).getServer().getInfo().getName()+") : "+message));
		}
		final String fmsg = message;
		BungeeCord.getInstance().getScheduler().runAsync(Bungee.instance, new Runnable() {
			public void run() {
				try {
					WebAPI.webhook("Rezxis-Reports", "[REPORT] - "+sender.getName()+"("+((ProxiedPlayer)sender).getServer().getInfo().getName()+") : "+fmsg);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		sender.sendMessage(new TextComponent(ChatColor.GREEN+"レポートが完了しました。"));
		sender.sendMessage(new TextComponent(ChatColor.GREEN+"内容 : "+message));
	}
}
