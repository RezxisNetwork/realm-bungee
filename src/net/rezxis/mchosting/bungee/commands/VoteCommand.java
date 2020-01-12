package net.rezxis.mchosting.bungee.commands;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.rezxis.mchosting.database.Tables;
import net.rezxis.mchosting.database.object.ServerWrapper;
import net.rezxis.mchosting.database.object.player.DBPlayer;
import net.rezxis.mchosting.database.object.server.DBServer;
import net.rezxis.mchosting.database.object.server.DBThirdParty;

public class VoteCommand extends Command {

	public VoteCommand() {
		super("vote");
	}

	@SuppressWarnings("deprecation")
	@Override
	public void execute(CommandSender arg0, String[] args) {
		ProxiedPlayer pp = (ProxiedPlayer)arg0;
		DBPlayer self = Tables.getPTable().get(pp.getUniqueId());
		if (self.getNextVote().after(new Date())) {
			pp.sendMessage(ChatColor.RED+"投票は一日一回のみです。");
			return;
		} else {
			ServerWrapper server = ServerWrapper.getServerByName(pp.getServer().getInfo().getName());
			if (server == null) {
				pp.sendMessage(ChatColor.RED+"エラーが発生しました。");
				return;
			} else {
				if (server.isDBServer()) {
					DBServer ds = server.getDBServer();
					ds.addVote(1);
					ds.update();
				} else {
					DBThirdParty dtp = server.getDBThirdParty();
					dtp.setScore(dtp.getScore()+1);
					dtp.update();
				}
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(new Date());
				calendar.add(Calendar.DAY_OF_WEEK, 1);
				self.setNextVote(calendar.getTime());
				self.update();
				pp.sendMessage(server.getDisplayName()+ChatColor.GREEN+"に投票しました。");
				ByteArrayOutputStream stream = new ByteArrayOutputStream();
		        DataOutputStream out = new DataOutputStream(stream);
		        try {
					out.writeUTF("vote");
					out.writeUTF(pp.getName());
				} catch (IOException e) {
					e.printStackTrace();
				}
			    pp.getServer().sendData("rezxis", stream.toByteArray());
			}
		}
	}
}
