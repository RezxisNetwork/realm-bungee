package net.rezxis.mchosting.bungee.commands;

import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.rezxis.mchosting.database.Tables;
import net.rezxis.mchosting.database.object.player.DBPlayer;

public class PayCommand extends Command {

	public PayCommand() {
		super("pay");
		// TODO Auto-generated constructor stub
	}

	@SuppressWarnings("deprecation")
	@Override
	public void execute(CommandSender sender, String[] args) {
		if (args.length != 2) {
			sender.sendMessage(ChatColor.RED+"使い方 : /pay 送金先 金額");
		} else {
			DBPlayer player = Tables.getPTable().get(BungeeCord.getInstance().getPlayer(sender.getName()).getUniqueId());
			DBPlayer target = Tables.getPTable().get(BungeeCord.getInstance().getPlayer(args[0]).getUniqueId());
			if (target == null) {
				sender.sendMessage(ChatColor.RED+"指定されたプレイヤーは存在しません");
				return;
			}
			int c = 0;
			try {
				c = Integer.valueOf(args[1]);
			} catch (Exception ex) {
				sender.sendMessage(ChatColor.RED+"正しい値を入れていください。");
				return;
			}
			if (c <= 0) {
				sender.sendMessage(ChatColor.RED+"正しい値を入れていください。");
				return;
			}
			if (player.getCoin() < c) {
				sender.sendMessage(ChatColor.RED+"コインが足りません。");
				return;
			}
			if (args[0].equalsIgnoreCase(sender.getName())) {
				sender.sendMessage(ChatColor.RED+"自分には送金できません。");
				return;
			}
			player.setCoin(player.getCoin()-c);
			target.setCoin(target.getCoin()+c);
			player.update();
			target.update();
			sender.sendMessage(ChatColor.GREEN+(c+"を"+args[0]+"に送金しました。"));
			BungeeCord.getInstance().getPlayer(args[0]).sendMessage(ChatColor.GREEN+""+sender.getName()+"から"+c+"を受け取りました。");
		}
	}
}
