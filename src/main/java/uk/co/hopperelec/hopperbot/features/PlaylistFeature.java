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
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.hopperbot.HopperBotCommandFeature;
import uk.co.hopperelec.hopperbot.HopperBotFeatures;

import java.io.File;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.file.FileSystems;
import java.util.*;

public final class PlaylistFeature extends HopperBotCommandFeature implements AudioEventListener {

    private static final String songDataFileLocation = "Playlist/songs.yml";
    private static final String songDirectoryLocation = "Playlist/songs/";
    private final String absoluteSongDirectoryLocation = FileSystems.getDefault().getPath(songDirectoryLocation).toAbsolutePath().toString();
    private final Map<String, Map<String,JsonNode>> songData = new HashMap<>();
    private Set<String> songFilenames;
    private final Random random = new Random();
    final AudioPlayerManager playerManager;
    final AudioPlayer player;
    final ByteBuffer buffer;
    final MutableAudioFrame frame;
    final AudioSendHandler sendHandler;

    public PlaylistFeature(JDABuilder builder) {
        super(builder,HopperBotFeatures.PLAYLIST, "~");
        playerManager = new DefaultAudioPlayerManager();
        player = playerManager.createPlayer();
        playerManager.registerSourceManager(new LocalAudioSourceManager());

        buffer = ByteBuffer.allocate(1024);
        frame = new MutableAudioFrame();
        frame.setBuffer(buffer);
        sendHandler = new AudioSendHandler() {
            @Override
            public boolean canProvide() {
                return player.provide(frame);
            }

            @Override
            public ByteBuffer provide20MsAudio() {
                ((Buffer) buffer).flip();
                return buffer;
            }

            @Override
            public boolean isOpus() {
                return true;
            }
        };
    }

    @Override
    public void onEvent(AudioEvent event) {
        if (event instanceof TrackEndEvent && ((TrackEndEvent) event).endReason.mayStartNext) {
            playNextSong();
        }
    }

    public void playNextSong() {
        final Iterator<String> iterator = songFilenames.iterator();
        for (int i = 0; i < random.nextInt(songFilenames.size()); i++) {
            iterator.next();
        }

        final String songFileName = iterator.next();
        getUtils().log("Now trying to play "+songFileName,null,featureEnum);
        final String songFileLocation = absoluteSongDirectoryLocation+File.separator+songFileName;
        playerManager.loadItem(songFileLocation, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                getUtils().log("Successfully loaded "+songFileName,null,featureEnum);
                player.startTrack(track,false);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {}

            @Override
            public void noMatches() {
                getUtils().log("Couldn't find song at "+songFileLocation.replace(File.separator,"\\"+File.separator),null,featureEnum);
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                getUtils().log("Failed loading "+songFileName,null,featureEnum);
            }
        });
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        super.onReady(event);

        final JsonNode songDataNode = getUtils().getYAMLFile(featureEnum, songDataFileLocation, JsonNode.class);
        if (songDataNode != null) {
            songDataNode.fields().forEachRemaining(song -> {
                songData.put(song.getKey(),new HashMap<>());
                song.getValue().fields().forEachRemaining(songProperty -> {
                    songData.get(song.getKey()).put(songProperty.getKey(),songProperty.getValue());
                });
            });
            songFilenames = songData.keySet();
            getUtils().log("Serialized songs into Map<String, Map<String,JsonNode>>",null,featureEnum);

            player.addListener(this);
            playNextSong();
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
