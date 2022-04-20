package uk.co.hopperelec.hopperbot.features;

import com.fasterxml.jackson.databind.JsonNode;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEvent;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventListener;
import com.sedmelluq.discord.lavaplayer.player.event.TrackEndEvent;
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.hopperbot.HopperBotCommandFeature;
import uk.co.hopperelec.hopperbot.HopperBotFeatures;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.FileSystems;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

public final class PlaylistFeature extends HopperBotCommandFeature implements AudioEventListener {
    private static final String songsDataFileLocation = "Playlist/songs.yml";
    private static final String songsDirectoryLocation = "Playlist/";
    private static final int maxNextSongAttempts = 5;
    private final String absoluteSongsDirectoryLocation = FileSystems.getDefault().getPath(songsDirectoryLocation).toAbsolutePath().toString();
    private final Map<String, Map<String,JsonNode>> songData = new HashMap<>();
    private Set<String> songFilenames;
    private final Random random = new Random();
    private final AudioPlayerManager playerManager;
    private final AudioPlayer player;
    private final AudioSendHandler sendHandler;
    private AudioFrame frame;

    public PlaylistFeature(JDABuilder builder) {
        super(builder,HopperBotFeatures.PLAYLIST, "~");
        playerManager = new DefaultAudioPlayerManager();
        player = playerManager.createPlayer();
        playerManager.registerSourceManager(new LocalAudioSourceManager());

        sendHandler = new AudioSendHandler() {
            @Override
            public boolean canProvide() {
                return frame != null;
            }

            @Override
            public ByteBuffer provide20MsAudio() {
                return ByteBuffer.wrap(frame.getData());
            }

            @Override
            public boolean isOpus() {
                return true;
            }
        };
        newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> frame = player.provide(), 20, 20, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onEvent(AudioEvent event) {
        if (event instanceof TrackEndEvent && ((TrackEndEvent) event).endReason.mayStartNext) {
            playNextSong(0);
        }
    }

    public void playNextSong(int attempts) {
        if (attempts < maxNextSongAttempts) {
            final Iterator<String> iterator = songFilenames.iterator();
            for (int i = 0; i < random.nextInt(songFilenames.size()); i++) {
                iterator.next();
            }
            final String songFileName = iterator.next();

            synchronized (this) {
                getUtils().jda().getPresence().setActivity(Activity.listening(FilenameUtils.removeExtension(songFileName)));
                getUtils().log("Now trying to play "+songFileName,null,featureEnum);

                final String songFileLocation = absoluteSongsDirectoryLocation+File.separator+songFileName;
                playerManager.loadItem(songFileLocation, new AudioLoadResultHandler() {
                    @Override
                    public void trackLoaded(AudioTrack track) {
                        getUtils().log("Successfully loaded "+songFileName,null,featureEnum);
                        player.playTrack(track);
                    }

                    @Override
                    public void playlistLoaded(AudioPlaylist playlist) {
                        // Only individual tracks are loaded; playlists are not expected
                    }

                    @Override
                    public void noMatches() {
                        getUtils().log("Couldn't find song at "+songFileLocation.replace(File.separator,"\\"+File.separator),null,featureEnum);
                        playNextSong(attempts+1);
                    }

                    @Override
                    public void loadFailed(FriendlyException exception) {
                        getUtils().log("Failed loading "+songFileName,null,featureEnum);
                        playNextSong(attempts+1);
                    }
                });
            }
        } else {
            getUtils().log("Maximum attempts at playing the next song ("+maxNextSongAttempts+") reached",null,featureEnum);
            getUtils().jda().getPresence().setActivity(null);
        }
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        super.onReady(event);

        final JsonNode songsDataNode = getUtils().getYAMLFile(featureEnum, songsDataFileLocation, JsonNode.class);
        if (songsDataNode != null) {
            songsDataNode.fields().forEachRemaining(song -> {
                songData.put(song.getKey(),new HashMap<>());
                song.getValue().fields().forEachRemaining(songProperty -> {
                    songData.get(song.getKey()).put(songProperty.getKey(),songProperty.getValue());
                });
            });
            songFilenames = songData.keySet();
            getUtils().log("Serialized songs",null,featureEnum);

            player.addListener(this);
            playNextSong(0);
        }
    }

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        final Map<String, JsonNode> config = getUtils().getFeatureConfig(event.getGuild(),featureEnum);
        if (config != null) {
            final long id = config.get("voice_channel").asLong();
            final VoiceChannel voiceChannel = event.getGuild().getVoiceChannelById(id);
            if (voiceChannel == null) {
                getUtils().log("Could not find voice channel by ID "+id,event.getGuild(),featureEnum);
            } else {
                event.getGuild().getAudioManager().openAudioConnection(voiceChannel);
                event.getGuild().getAudioManager().setSendingHandler(sendHandler);
            }
        }
    }
}
