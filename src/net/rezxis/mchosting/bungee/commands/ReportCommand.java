package net.rezxis.mchosting.bungee.commands;

import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.rezxis.mchosting.bungee.WebAPI;
import net.rezxis.mchosting.bungee.WebAPI.DiscordWebHookEnum;

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
		WebAPI.webhook(DiscordWebHookEnum.REPORT, "[REPORT] - "+sender.getName()+"("+((ProxiedPlayer)sender).getServer().getInfo().getName()+") : "+fmsg);
		sender.sendMessage(new TextComponent(ChatColor.GREEN+"レポートが完了しました。"));
		sender.sendMessage(new TextComponent(ChatColor.GREEN+"内容 : "+message));
	}
}
