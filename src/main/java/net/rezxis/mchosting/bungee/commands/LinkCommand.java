package net.rezxis.mchosting.bungee.commands;

import org.apache.commons.lang3.RandomStringUtils;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.rezxis.mchosting.database.Tables;
import net.rezxis.mchosting.database.object.player.DBPlayer;

public class LinkCommand extends Command {

	public LinkCommand() {
		super("link");
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		if (sender instanceof ProxiedPlayer) {
			ProxiedPlayer pp = (ProxiedPlayer) sender;
			DBPlayer dp = Tables.getPTable().get(pp.getUniqueId());
			if (dp.getDiscordId() != -1) {
				sender.sendMessage(new TextComponent(ChatColor.RED+"あなたのDiscordはすでにリンクされています。"));
				return;
			}
			String key = RandomStringUtils.randomAlphabetic(10);
			dp.setVerifyCode(key);
			dp.update();
			sender.sendMessage(new TextComponent(ChatColor.GREEN+"Link Code : "+key));
			sender.sendMessage(new TextComponent(ChatColor.GREEN+"#discord-link で</link "+key+">をしてください。"));
		} else
			sender.sendMessage(new TextComponent(ChatColor.RED+"this command must be ran by player"));
	}
}
