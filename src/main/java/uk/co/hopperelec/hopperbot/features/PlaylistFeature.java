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
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.hopperbot.*;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.FileSystems;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

public final class PlaylistFeature extends HopperBotButtonFeature implements AudioEventListener {
    private static final String SONGS_FILE_LOC = "Playlist/songs.yml";
    private static final String SONGS_DIR_LOC = "Playlist/";
    private static final String ABSOLUTE_SONGS_DIR_LOC = FileSystems.getDefault().getPath(SONGS_DIR_LOC).toAbsolutePath().toString();
    private static final int SONGLIST_MAX_LINES = 31;
    private static final int MAX_NEXT_SONG_ATTEMPTS = 5;

    private final Map<String, Map<String,JsonNode>> songData = new HashMap<>();
    private Set<String> songFilenames;
    private final List<String> lastThreeSongs = new ArrayList<>(3);
    private final List<MessageEmbed> songlistPages = new ArrayList<>();
    private final Random random = new Random();

    private boolean anyoneListening = false;
    private final AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
    private final AudioPlayer player = playerManager.createPlayer();
    private AudioFrame frame;
    private final AudioSendHandler sendHandler = new AudioSendHandler() {
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

    public PlaylistFeature(JDABuilder builder) {
        super("playlist",builder,HopperBotFeatures.PLAYLIST,"~",
                new HopperBotCommand("songlist","Shows list of songs currently in the playlist",new String[]{"playlist"},null) {
                    @Override
                    public void runTextCommand(MessageReceivedEvent event, String content, HopperBotCommandFeature feature, HopperBotUtils utils) {
                        final PlaylistFeature self = ((PlaylistFeature) feature);
                        event.getMessage().replyEmbeds(self.songlistPages.get(0)).setActionRow(self.getSonglistButtons(1)).queue();
                    }

                    @Override
                    public void runSlashCommand(SlashCommandInteractionEvent event, HopperBotCommandFeature feature, HopperBotUtils utils) {
                        final PlaylistFeature self = ((PlaylistFeature) feature);
                        event.replyEmbeds(self.songlistPages.get(0)).addActionRow(self.getSonglistButtons(1)).queue();
                    }
                }
        );
        playerManager.registerSourceManager(new LocalAudioSourceManager());
        newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> frame = player.provide(), 20, 20, TimeUnit.MILLISECONDS);
    }

    private Button songlistButton(String action, String emojiUnicode, boolean disabled) {
        final Button button = Button.of(ButtonStyle.PRIMARY,"playlist-songlist-"+action, Emoji.fromUnicode(emojiUnicode));
        if (disabled) {
            return button.asDisabled();
        }
        return button;
    }
    private List<Button> getSonglistButtons(int currentPage) {
        boolean prev = currentPage == 1;
        boolean next = currentPage == songlistPages.size();
        return List.of(
                songlistButton("start","U+23EE",prev),
                songlistButton("previous","U+023EA",prev),
                songlistButton("next","U+023E9",next),
                songlistButton("end","U+023ED",next)
        );
    }

    private int getSonglistPageNumber(ButtonInteractionEvent event) {
        final String title = event.getMessage().getEmbeds().get(0).getTitle();
        if (title == null) {
            return -1;
        }
        final String[] titleWords = title.split(" ");
        return Integer.parseInt(titleWords[titleWords.length-1].split("/")[0]);
    }
    public void runButtonCommand(ButtonInteractionEvent event,String[] parts) {
        if (parts[1].equals("songlist")) {
            final int newPage = switch (parts[2]) {
                default -> 1;
                case "previous" -> getSonglistPageNumber(event)-1;
                case "next" -> getSonglistPageNumber(event)+1;
                case "end" -> songlistPages.size();
            };
            event.editMessageEmbeds(songlistPages.get(newPage-1)).setActionRow(getSonglistButtons(newPage)).queue();
        }
    }

    @Override
    public void onEvent(AudioEvent event) {
        if (event instanceof TrackEndEvent && ((TrackEndEvent) event).endReason.mayStartNext) {
            playNextSong();
        }
    }

    private void playNextSong(int attempts) {
        if (attempts < MAX_NEXT_SONG_ATTEMPTS) {
            if (anyoneListening) {
                playSong(nextSong(), attempts);
                return;
            } else {
                getUtils().logGlobally("Nobody currently listening- pausing playback",featureEnum);
            }
        } else {
            getUtils().logGlobally("Maximum attempts at playing the next song ("+MAX_NEXT_SONG_ATTEMPTS+") reached",featureEnum);
        }
        getUtils().jda().getPresence().setActivity(null);
    }
    private void playNextSong() {
        playNextSong(0);
    }

    private String nextSong() {
        String song = "";
        boolean loop = true;
        while (loop) {
            final Iterator<String> iterator = songFilenames.iterator();
            for (int i = 0; i < random.nextInt(songFilenames.size()); i++) {
                iterator.next();
            }
            song = iterator.next();
            if (!lastThreeSongs.contains(song)) {
                loop = false;
            }
        }
        lastThreeSongs.add(song);
        if (lastThreeSongs.size() == 4) {
            lastThreeSongs.remove(0);
        }
        return song;
    }

    private synchronized void playSong(String songFilename, int attempts) {
        getUtils().jda().getPresence().setActivity(Activity.listening(FilenameUtils.removeExtension(songFilename)));
        getUtils().logGlobally("Now trying to play "+songFilename,featureEnum);

        final String songFileLocation = ABSOLUTE_SONGS_DIR_LOC+File.separator+songFilename;
        playerManager.loadItem(songFileLocation, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                getUtils().logGlobally("Successfully loaded "+songFilename,featureEnum);
                player.playTrack(track);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                // Only individual tracks are loaded; playlists are not expected
            }

            @Override
            public void noMatches() {
                getUtils().logGlobally("Couldn't find song at "+songFileLocation.replace(File.separator,"\\"+File.separator),featureEnum);
                playNextSong(attempts+1);
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                getUtils().logGlobally("Failed loading "+songFilename,featureEnum);
                playNextSong(attempts+1);
            }
        });
    }

    private Map<String,TreeSet<String>> songsByAuthor() {
        final Map<String,TreeSet<String>> authorFields = new HashMap<>();
        songData.values().forEach((song) -> {
            String songTitle = song.get("Title").textValue();
            final String author = song.get("Authors").get(0).textValue();
            if (!authorFields.containsKey(author)) {
                authorFields.put(author,new TreeSet<>());
            }
            authorFields.get(author).add(songTitle);
        });
        return authorFields;
    }
    private List<Map.Entry<String,TreeSet<String>>> sortedSongsByAuthor() {
        return new ArrayList<>(songsByAuthor().entrySet()).stream().sorted((firstEntry, secondEntry) -> {
            final int sizeDifference = secondEntry.getValue().size() - firstEntry.getValue().size();
            if (sizeDifference == 0) {
                return firstEntry.getKey().compareTo(secondEntry.getKey());
            }
            return sizeDifference;
        }).toList();
    }

    private List<Integer> songlistPages(List<Map.Entry<String,TreeSet<String>>> authorFields) {
        final List<Integer> pages = new ArrayList<>();
        final AtomicInteger lines = new AtomicInteger();
        final AtomicInteger authors = new AtomicInteger();
        authorFields.forEach(entry -> {
            authors.incrementAndGet();
            if (lines.addAndGet(entry.getValue().size()) > SONGLIST_MAX_LINES) {
                pages.add(authors.get());
                lines.set(0);
            }
        });
        return pages;
    }
    private EmbedBuilder songlistEmbedBase(int pageIndex,int maxPages) {
        return getUtils().getEmbedBase().setTitle("Playlist - Page "+(pageIndex+1)+"/"+(maxPages+1));
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        super.onReady(event);

        final JsonNode songsDataNode = getUtils().getYAMLFile(featureEnum, SONGS_FILE_LOC, JsonNode.class);
        if (songsDataNode == null) {
            return;
        }

        songsDataNode.fields().forEachRemaining(song -> {
            songData.put(song.getKey(),new HashMap<>());
            song.getValue().fields().forEachRemaining(songProperty -> {
                songData.get(song.getKey()).put(songProperty.getKey(),songProperty.getValue());
            });
        });
        songFilenames = songData.keySet();
        getUtils().logGlobally("Serialized songs",featureEnum);
        player.addListener(this);
        if (anyoneListening) {
            playNextSong();
        }

        final List<Map.Entry<String,TreeSet<String>>> authorFields = sortedSongsByAuthor();
        final List<Integer> pages = songlistPages(authorFields);
        final AtomicReference<EmbedBuilder> embedBuilder = new AtomicReference<>(songlistEmbedBase(0,pages.size()));
        for (int authors = 0; authors < authorFields.size(); authors++) {
            if (pages.contains(authors)) {
                songlistPages.add(embedBuilder.get().build());
                embedBuilder.set(songlistEmbedBase(songlistPages.size(),pages.size()));
            }
            embedBuilder.get().addField(authorFields.get(authors).getKey(),String.join("\n",authorFields.get(authors).getValue()),false);
        }
        songlistPages.add(embedBuilder.get().build());
    }

    private VoiceChannel getVoiceChannelFor(Guild guild) {
        final Map<String, JsonNode> config = getUtils().getFeatureConfig(guild,featureEnum);
        if (config == null) {
            return null;
        }
        return guild.getVoiceChannelById(config.get("voice_channel").asLong());
    }

    private boolean isAnyoneListening(VoiceChannel voiceChannel) {
        if (voiceChannel == null) {
            return false;
        }
        return voiceChannel.getMembers().stream().anyMatch(member -> !member.getUser().isBot());
    }

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        final VoiceChannel voiceChannel = getVoiceChannelFor(event.getGuild());
        if (voiceChannel == null) {
            getUtils().logToGuild("Could not find playlist voice channel",event.getGuild());
        } else {
            event.getGuild().getAudioManager().openAudioConnection(voiceChannel);
            event.getGuild().getAudioManager().setSendingHandler(sendHandler);
            anyoneListening = isAnyoneListening(voiceChannel);
        }
    }

    @Override
    public void onGuildVoiceJoin(@NotNull GuildVoiceJoinEvent event) {
        final boolean wasListening = anyoneListening;
        anyoneListening = isAnyoneListening(getVoiceChannelFor(event.getGuild()));
        if (!wasListening && anyoneListening) {
            playNextSong();
        }
    }

    @Override
    public void onGuildVoiceLeave(@NotNull GuildVoiceLeaveEvent event) {
        if (getUtils().usesFeature(event.getGuild(),featureEnum)) {
            anyoneListening = guilds.stream().anyMatch(guild -> isAnyoneListening(getVoiceChannelFor(guild)));
        }
    }
}
