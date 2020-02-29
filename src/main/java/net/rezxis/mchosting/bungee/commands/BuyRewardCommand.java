package net.rezxis.mchosting.bungee.commands;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.rezxis.mchosting.database.Tables;
import net.rezxis.mchosting.database.object.player.DBPlayer;
import net.rezxis.mchosting.database.object.player.DBPlayer.Rank;

public class BuyRewardCommand extends Command {

	public BuyRewardCommand() {
		super("buyreward", "rezxis.rank");
	}

	public static UUID getUUIDFromNonDashedString(String uuid) {
		return UUID.fromString( uuid.substring( 0, 8 ) + "-" + uuid.substring( 8, 12 ) + "-" + uuid.substring( 12, 16 ) + "-" + uuid.substring( 16, 20 ) + "-" + uuid.substring( 20, 32 ) );
    }
	@SuppressWarnings("deprecation")
	@Override
	public void execute(CommandSender sender, String[] args) {
		UUID uuid = getUUIDFromNonDashedString(args[0]);
		ProxiedPlayer pp = BungeeCord.getInstance().getPlayer(uuid);
		DBPlayer player = Tables.getPTable().get(uuid);
		if (Integer.valueOf(args[1]) == 0) {
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(new Date());
			Rank rank = Rank.valueOf(args[2]);
			if (player.getRank() == rank && !player.isExpiredRank()) {
				calendar.setTime(player.getRankExpire());
			}
			calendar.add(Calendar.MONTH, 1);
			player.setRank(rank);
			player.setRankExpire(calendar.getTime());
			player.update();
			if (pp != null)
				if (pp.isConnected())
					pp.sendMessage(ChatColor.GREEN+"寄付ありがとうございます！内容 : "+rank.getPrefix());
		} else if (Integer.valueOf(args[1]) == 1) {
			if (!player.isExpiredSupporter()) {
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(player.getSupporterExpire());
				calendar.add(Calendar.MONTH, 1);
				player.setSupporterExpire(calendar.getTime());
			} else {
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(new Date());
				calendar.add(Calendar.MONTH, 1);
				player.setSupporterExpire(calendar.getTime());
			}
			player.update();
			if (pp != null)
				if (pp.isConnected())
					pp.sendMessage(ChatColor.GREEN+"寄付ありがとうございます！内容 : サポーター 期限 : "+player.getSupporterExpire().toLocaleString());
		}
	}
}
