package de.blockbuild.musikbot.commands.music;

import java.util.ArrayList;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import de.blockbuild.musikbot.Bot;
import de.blockbuild.musikbot.commands.MusicCommand;
import de.blockbuild.musikbot.core.GuildMusicManager;
import de.blockbuild.musikbot.core.TrackScheduler;

public class QueueCommand extends MusicCommand {

	private Boolean isSearch;

	public QueueCommand(Bot bot) {
		super(bot);
		this.name = "queue";
		this.aliases = new String[] { "q" };
		this.help = "Returns the playlist or adds the given track to queue.";
		this.arguments = "[URL|title]";
		this.joinOnCommand = true;
		this.isSearch = false;
	}

	@Override
	protected void doGuildCommand(CommandEvent event) {
		TrackScheduler trackScheduler = musicManager.getTrackScheduler();
		AudioPlayerManager playerManager = bot.getPlayerManager();

		if (args.isEmpty()) {
			if (!event.getMessage().getAttachments().isEmpty()) {
				if (!event.getMessage().getAttachments().get(0).isImage()) {
					String TrackURL = event.getMessage().getAttachments().get(0).getUrl();
					playerManager.loadItemOrdered(musicManager, TrackURL, new ResultHandler(trackScheduler, event));
				}
			} else {
				StringBuilder builder = new StringBuilder(event.getClient().getSuccess());
				builder.append(" **Queue:**\n").append(trackScheduler.getPlaylist());
				event.reply(builder.toString());
			}
		} else {
			String TrackUrl = args;
			if (!args.startsWith("http")) {
				TrackUrl = "ytsearch:" + TrackUrl;
				isSearch = true;
			}
			playerManager.loadItemOrdered(musicManager, TrackUrl, new ResultHandler(trackScheduler, event));
		}
	}

	@Override
	protected void doPrivateCommand(CommandEvent event) {
		event.reply(event.getClient().getError() + " This command cannot be used in Direct messages.");

		StringBuilder builder = new StringBuilder().append(event.getClient().getSuccess());

		builder.append(" **MusikBot** ").append("by Block-Build\n");
		builder.append("SpigotMC: `https://www.spigotmc.org/resources/the-discord-musikbot-on-minecraft.64277/`\n");
		builder.append("GitHub: `https://github.com/Block-Build/MusikBot`\n");
		builder.append("Version: `").append(bot.getMain().getDescription().getVersion()).append("`\n");
		builder.append("Do you have any problem or suggestion? Open an Issue on GitHub.");

		event.reply(builder.toString());
	}

	private class ResultHandler implements AudioLoadResultHandler {

		private TrackScheduler trackScheduler;
		private CommandEvent event;
		private GuildMusicManager musicManager;

		public ResultHandler(TrackScheduler trackScheduler, CommandEvent event) {
			this.trackScheduler = trackScheduler;
			this.event = event;
			this.musicManager = bot.getGuildAudioPlayer(event.getGuild());
		}

		@Override
		public void trackLoaded(AudioTrack track) {
			trackScheduler.queue(track, event);
		}

		@Override
		public void playlistLoaded(AudioPlaylist playlist) {
			if (isSearch) {
				musicManager.tracks = new ArrayList<>();

				StringBuilder builder = new StringBuilder().append(event.getClient().getSuccess());
				builder.append(" Use `!Choose <1-5>` to choose one of the search results: \n");
				for (int i = 0; i < 5; i++) {
					builder.append("`").append(i + 1 + ". ").append(playlist.getTracks().get(i).getInfo().title)
							.append("`\n");
					musicManager.tracks.add(playlist.getTracks().get(i));
					musicManager.setIsQueue(true);
				}
				event.reply(builder.toString());
			} else {
				trackScheduler.queue(playlist, event);
			}
		}

		@Override
		public void noMatches() {
			StringBuilder builder = new StringBuilder(event.getClient().getError());
			builder.append(" No result found: ").append(args);
			event.reply(builder.toString());
		}

		@Override
		public void loadFailed(FriendlyException throwable) {
			StringBuilder builder = new StringBuilder(event.getClient().getError());
			builder.append(" faild to load ").append(args);
			event.reply(builder.toString());
		}
	}
}