package net.rezxis.mchosting.bungee.commands;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.rezxis.mchosting.database.Tables;
import net.rezxis.mchosting.database.object.player.DBPlayer;
import net.rezxis.mchosting.database.object.player.DBPlayer.Rank;
import net.rezxis.mchosting.database.object.player.DBUUID;
import net.rezxis.utils.WebAPI;
import net.rezxis.utils.WebAPI.DiscordWebHookEnum;

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
		//UUID uuid = getUUIDFromNonDashedString(args[0]);
		ProxiedPlayer pp = BungeeCord.getInstance().getPlayer(args[0]);//BungeeCord.getInstance().getPlayer(uuid);
		UUID uuid;
		if (pp.isConnected()) {
			uuid = pp.getUniqueId();
		} else {
			DBUUID dbuid = Tables.getUTable().get(args[0]);
			if (dbuid != null) {
				uuid = dbuid.getUuid();
			} else {
				WebAPI.webhook(DiscordWebHookEnum.PRIVATE, "@everyone [TebexPaymentGateway] Error in fetching uuid! name : "+args[0]);
				return;
			}
		}
		DBPlayer player = Tables.getPTable().get(uuid);
		if (Integer.valueOf(args[1]) == 0) {
			Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Japan"),Locale.JAPANESE);
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
				Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Japan"),Locale.JAPANESE);
				calendar.setTime(player.getSupporterExpire());
				calendar.add(Calendar.MONTH, 1);
				player.setSupporterExpire(calendar.getTime());
			} else {
				Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Japan"),Locale.JAPANESE);
				calendar.add(Calendar.MONTH, 1);
				player.setSupporterExpire(calendar.getTime());
			}
			player.setSupporter(true);
			player.update();
			if (pp != null)
				if (pp.isConnected())
					pp.sendMessage(ChatColor.GREEN+"寄付ありがとうございます！内容 : サポーター 期限 : "+player.getSupporterExpire().toLocaleString());
		}
	}
}
