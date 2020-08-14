package net.rezxis.mchosting.bungee.commands;

import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.rezxis.mchosting.bungee.KanaConverter;

public class GTellCommand extends Command {

	private static final String prefix = ChatColor.GRAY+"[GTELL]";
	
	public GTellCommand() {
		super("gtell");
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		if (args.length < 2) {
			sender.sendMessage(new TextComponent(ChatColor.RED+"Usage : /gtell <target> <message>"));
			return;
		}
		ProxiedPlayer player = BungeeCord.getInstance().getPlayer(args[0]);
		if (player != null && player.isConnected()) {
			String s = sender.getName();
			String message = "";
			for (int i = 1; i < args.length; i++) {
				message += " "+args[i];
			}
			message = message.replace("&", "§");
			message = KanaConverter.fixBrackets(KanaConverter.conv(message));
			TextComponent comp = new TextComponent(prefix+"["+s+"->"+player.getName()+"]"+message);
			sender.sendMessage(comp);
			player.sendMessage(comp);
		} else {
			sender.sendMessage(new TextComponent(prefix+ChatColor.RED+"指定されたプレイヤーはオフラインです。"));
		}
	}
}
