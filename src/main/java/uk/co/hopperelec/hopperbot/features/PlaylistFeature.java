package uk.co.hopperelec.hopperbot.features;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
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
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import uk.co.hopperelec.hopperbot.HopperBotFeatures;
import uk.co.hopperelec.hopperbot.commands.CommandUsageFilter;
import uk.co.hopperelec.hopperbot.commands.HopperBotButtonFeature;
import uk.co.hopperelec.hopperbot.commands.HopperBotCommand;
import uk.co.hopperelec.hopperbot.commands.command_responders.CommandResponder;
import uk.co.hopperelec.hopperbot.commands.command_responders.SlashCommandResponder;
import uk.co.hopperelec.hopperbot.commands.command_responders.TextCommandResponder;

import javax.annotation.CheckForNull;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.FileSystems;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.apache.commons.io.FilenameUtils.removeExtension;

public final class PlaylistFeature extends HopperBotButtonFeature implements AudioEventListener {
    @NotNull private static final String SONGS_FILE_LOC = "Playlist/songs.yml";
    @NotNull private static final String SONGS_DIR_LOC = "Playlist/";
    @NotNull private static final String ABSOLUTE_SONGS_DIR_LOC = FileSystems.getDefault().getPath(SONGS_DIR_LOC).toAbsolutePath().toString();
    @NotNull private static final OptionData searchOption = new OptionData(OptionType.STRING, "search", "Keyword to search song properties for");
    private static final int SONGLIST_MAX_LINES = 31;
    private static final int MAX_NEXT_SONG_ATTEMPTS = 5;
    private static final double MIN_SEARCH_CONFIDENCE = 0.7;
    private static final int playSong_VOTE_EXPIRY_SECONDS = 120;
    private static final String playSong_VOTE_EMOJI = "\u2705";

    @NotNull private final List<HopperBotPlaylistSong> songs = new ArrayList<>();
    @NotNull private final List<HopperBotPlaylistSong> lastThreeSongs = new ArrayList<>(3);
    @NotNull private final List<MessageEmbed> songlistPages = new ArrayList<>();
    @NotNull private final Random random = new Random();
    @NotNull private final JaroWinklerSimilarity jaroWinklerSimilarity = new JaroWinklerSimilarity();
    @NotNull private final BiMap<Message,HopperBotPlaylistSong> playSongVoteMessages = HashBiMap.create();

    private boolean anyoneListening = false;
    @NotNull private final AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
    @NotNull private final AudioPlayer player = playerManager.createPlayer();
    private AudioFrame frame;
    @NotNull private final AudioSendHandler sendHandler = new AudioSendHandler() {
        @Override
        public boolean canProvide() {
            return frame != null;
        }

        @Override
        @NotNull
        public ByteBuffer provide20MsAudio() {
            return ByteBuffer.wrap(frame.getData());
        }

        @Override
        public boolean isOpus() {
            return true;
        }
    };

    private static class HopperBotPlaylistSong {
        @NotNull final String filename;
        @NotNull final String title;
        @NotNull final List<String> authors = new ArrayList<>();
        @NotNull final List<String> singers = new ArrayList<>();
        @NotNull final String url;
        @Nullable final String note;
        @Nullable final String lyrics;
        @Nullable final List<MessageEmbed> lyricEmbeds;
        final MessageEmbed songInfoEmbed;
        @Nullable Timer voteExpiryTimer = null;

        HopperBotPlaylistSong(@NotNull String filename, @NotNull Map<String,JsonNode> songJsonData, @NotNull PlaylistFeature playlistFeature) {
            this.filename = filename;
            title = songJsonData.get("Title").textValue();
            songJsonData.get("Authors").elements().forEachRemaining(author -> authors.add(author.textValue()));
            songJsonData.get("Singers").elements().forEachRemaining(singer -> singers.add(singer.textValue()));
            url = songJsonData.get("URL").textValue();
            final String note = songJsonData.get("Note").textValue();
            this.note = note.equals("") ? null : note;

            final Iterator<JsonNode> lyricsIterator = songJsonData.get("Lyrics").elements();
            if (lyricsIterator.hasNext()) {
                final List<String> lyricsLines = new ArrayList<>();
                lyricsIterator.forEachRemaining(lyric -> lyricsLines.add(lyric.textValue()));
                lyrics = String.join("\n",lyricsLines);

                lyricEmbeds = new ArrayList<>();
                final List<String> lyricPages = new ArrayList<>();
                for (String verse : lyrics.split("\n\n")) {
                    if (verse.length() >= 1024) {
                        String lastItem = "";
                        lyricPages.add(lastItem);
                        for (String line : verse.split("\n")) {
                            if (lastItem.length() + line.length() <= 1024) {
                                lyricPages.set(lyricPages.size()-1,lastItem+"\n"+line);
                            } else {
                                lyricPages.add(line);
                                lastItem = line;
                            }
                        }
                    } else {
                        lyricPages.add(verse);
                    }
                }
                int index = 0;
                for (String lyricPage : lyricPages) {
                    final EmbedBuilder embedBuilder = playlistFeature.pagedEmbedBase("Lyrics",index++,lyricPages.size());
                    lyricEmbeds.add(embedBuilder.addField(strippedFilename(),lyricPage,false).build());
                }
            } else {
                lyrics = null;
                lyricEmbeds = null;
            }

            songInfoEmbed = addBooleanField(
                    addNullableField(
                            addListField(
                                    addListField(
                                            getEmbedBase()
                                                    .setTitle("Song info")
                                                    .setAuthor(strippedFilename() + " (Click for YouTube video)",fullURL())
                                                    .setImage(thumbnail())
                                                    .addField("Title",title,false),
                                            "Author", authors, true
                                    ), "Singer", singers, true
                            ), "Note", note, false
                    ), "Lyrics available (/lyrics [song])", lyrics != null, false
            ).build();
        }

        @NotNull
        public String strippedFilename() {
            return removeExtension(filename);
        }

        @NotNull
        public String thumbnail() {
            return "https://i.ytimg.com/vi/"+url+"/hqdefault.jpg";
        }

        @NotNull
        public String fullURL() {
            return "https://www.youtube.com/watch?v="+url;
        }

        @NotNull
        private EmbedBuilder addListField(@NotNull EmbedBuilder embedBuilder, @NotNull String name, @NotNull List<String> values, boolean inline) {
            if (values.isEmpty()) {
                return embedBuilder;
            }
            if (values.size() == 1) {
                return embedBuilder.addField(name,values.get(0),true);
            }
            return embedBuilder.addField(name+"s",String.join(", ",values),inline);
        }
        @NotNull
        private EmbedBuilder addNullableField(@NotNull EmbedBuilder embedBuilder, @NotNull String name, @Nullable String value, boolean inline) {
            if (value == null || value.equals("")) {
                return embedBuilder;
            }
            return embedBuilder.addField(name,value,inline);
        }
        @NotNull
        private EmbedBuilder addBooleanField(@NotNull EmbedBuilder embedBuilder, @NotNull String name, boolean value, boolean inline) {
            return embedBuilder.addField(name+"?",value ? "Yes" : "No",inline);
        }
    }

    public PlaylistFeature(@NotNull JDABuilder builder) {
        super("playlist", builder, HopperBotFeatures.PLAYLIST, "~");
        addCommands(
                new HopperBotCommand<>(this, "songlist", "Shows list of songs currently in the playlist", new String[]{"playlist"}, null) {
                    @Override
                    public void runTextCommand(@NotNull MessageReceivedEvent event, @NotNull String content) {
                        event.getMessage().replyEmbeds(feature.songlistPages.get(0)).setActionRow(feature.getPageButtons("songlist", 1, feature.songlistPages)).queue();
                    }

                    @Override
                    public void runSlashCommand(@NotNull SlashCommandInteractionEvent event) {
                        event.replyEmbeds(feature.songlistPages.get(0)).addActionRow(feature.getPageButtons("songlist", 1, feature.songlistPages)).queue();
                    }
                }, new HopperBotCommand<>(this, "play", "Plays a specific song (only if bot owner or only person listening)", null,
                    new OptionData[]{searchOption.setRequired(true)},
                    CommandUsageFilter.NON_EMPTY_CONTENT
            ) {
                @Override
                public void runTextCommand(@NotNull MessageReceivedEvent event, @NotNull String content) {
                    feature.playSongCommand(new TextCommandResponder(event.getMessage()), event.getAuthor(), content);
                }

                @Override
                public void runSlashCommand(@NotNull SlashCommandInteractionEvent event) {
                    final OptionMapping optionMapping = event.getOption("search");
                    if (optionMapping != null) {
                        feature.playSongCommand(new SlashCommandResponder(event, false), event.getUser(), optionMapping.getAsString());
                    }
                }
            }, new HopperBotCommand<>(this, "lyrics", "Displays the lyrics to the currently playing or a specified song", null,
                    new OptionData[]{searchOption.setRequired(false)}
            ) {
                @Override
                public void runTextCommand(@NotNull MessageReceivedEvent event, @NotNull String content) {
                    if (content.equals("")) {
                        feature.lyricsCommand(new TextCommandResponder(event.getMessage()));
                    } else {
                        feature.lyricsCommand(new TextCommandResponder(event.getMessage()), content);
                    }
                }

                @Override
                public void runSlashCommand(@NotNull SlashCommandInteractionEvent event) {
                    final OptionMapping optionMapping = event.getOption("search");
                    if (optionMapping == null) {
                        feature.lyricsCommand(new SlashCommandResponder(event, false));
                    } else {
                        feature.lyricsCommand(new SlashCommandResponder(event, false), optionMapping.getAsString());
                    }
                }
            }, new HopperBotCommand<>(this, "songinfo", "Shows lots of information about the currently playing or specified song", null,
                    new OptionData[]{searchOption.setRequired(false)}
            ) {
                @Override
                public void runTextCommand(@NotNull MessageReceivedEvent event, @NotNull String content) {
                    if (content.equals("")) {
                        feature.songInfoCommand(new TextCommandResponder(event.getMessage()));
                    } else {
                        feature.songInfoCommand(new TextCommandResponder(event.getMessage()), content);
                    }
                }

                @Override
                public void runSlashCommand(@NotNull SlashCommandInteractionEvent event) {
                    final OptionMapping optionMapping = event.getOption("search");
                    if (optionMapping == null) {
                        feature.songInfoCommand(new SlashCommandResponder(event, false));
                    } else {
                        feature.songInfoCommand(new SlashCommandResponder(event, false), optionMapping.getAsString());
                    }
                }
            }
        );
        playerManager.registerSourceManager(new LocalAudioSourceManager());
        newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> frame = player.provide(), 20, 20, TimeUnit.MILLISECONDS);
    }

    private enum WhoListening {
        NOBODY, OTHER_GUILDS, ONLY_GUILD, ONLY_USER
    }
    @CheckReturnValue
    private WhoListening onlyUserOrGuildListening(@NotNull User user) {
        boolean selfFound = false;
        boolean otherUsers = false;
        for (Guild guild : guilds) {
            final VoiceChannel voiceChannel = getVoiceChannelFor(guild);
            if (selfFound) {
                if (isAnyoneListeningIn(voiceChannel)) {
                    return WhoListening.OTHER_GUILDS;
                }
            } else if (voiceChannel != null) {
                for (Member member : voiceChannel.getMembers()) {
                    if (member.getUser() == user) {
                        selfFound = true;
                    } else if (!member.getUser().isBot()) {
                        otherUsers = true;
                    }
                }
                if (!selfFound && otherUsers) {
                    return WhoListening.OTHER_GUILDS;
                }
            }
        }
        if (selfFound) {
            return otherUsers ? WhoListening.ONLY_GUILD : WhoListening.ONLY_USER;
        }
        return WhoListening.NOBODY;
    }

    @CheckReturnValue
    private boolean onlyGuildListening(@NotNull Guild guildToCheck) {
        boolean selfFound = false;
        for (Guild guild : guilds) {
            final VoiceChannel voiceChannel = getVoiceChannelFor(guild);
            final boolean listenedTo = isAnyoneListeningIn(voiceChannel);
            if (guild == guildToCheck) {
                selfFound = true;
                if (voiceChannel == null || !listenedTo) {
                    return false;
                }
            } else if (listenedTo) {
                return false;
            }
        }
        return selfFound;
    }

    @CheckReturnValue
    private boolean onlyPersonListening(@NotNull User user) {
        boolean selfFound = false;
        for (Guild guild : guilds) {
            final VoiceChannel voiceChannel = getVoiceChannelFor(guild);
            if (selfFound) {
                if (isAnyoneListeningIn(voiceChannel)) {
                    return false;
                }
            } else if (voiceChannel != null) {
                for (Member member : voiceChannel.getMembers()) {
                    if (member.getUser() == user) {
                        selfFound = true;
                    } else if (!member.getUser().isBot()) {
                        return false;
                    }
                }
            }
        }
        return selfFound;
    }

    @NotNull
    @CheckReturnValue
    private Map.Entry<HopperBotPlaylistSong,Double> searchSongs(@NotNull String search) {
        final Map<HopperBotPlaylistSong,Double> results = new HashMap<>();
        for (HopperBotPlaylistSong song : songs) {
            results.put(song, jaroWinklerSimilarity.apply(search,song.filename));
        }
        return Collections.max(results.entrySet(), Comparator.comparingDouble(Map.Entry::getValue));
    }

    private void ifSongPlaying(@NotNull CommandResponder responder, @NotNull BiConsumer<CommandResponder, HopperBotPlaylistSong> action) {
        final HopperBotPlaylistSong song = currentSong();
        if (song == null) {
            responder.respond("No song is currently playing");
        } else {
            action.accept(responder, song);
        }
    }

    private void ifCloseMatch(@NotNull CommandResponder responder, @NotNull String search, @NotNull BiConsumer<CommandResponder, HopperBotPlaylistSong> action) {
        final Map.Entry<HopperBotPlaylistSong,Double> searchResult = searchSongs(search);
        if (searchResult.getValue() < MIN_SEARCH_CONFIDENCE) {
            responder.respond("Could not find a close match. Try being more specific");
        } else {
            action.accept(responder, searchResult.getKey());
        }
    }

    private void songInfoCommand(@NotNull CommandResponder responder, @NotNull HopperBotPlaylistSong song) {
        if (song.songInfoEmbed == null) {
            responder.respond("Playlist loaded too early so the embed for this song's info could not be generated. Please contact bot developer.");
        } else {
            responder.respond(song.songInfoEmbed);
        }
    }
    private void songInfoCommand(@NotNull CommandResponder responder) {
        ifSongPlaying(responder, this::songInfoCommand);
    }
    private void songInfoCommand(@NotNull CommandResponder responder, @NotNull String search) {
        ifCloseMatch(responder, search, this::songInfoCommand);
    }

    private void lyricsCommand(@NotNull CommandResponder responder, @NotNull HopperBotPlaylistSong song) {
        final List<MessageEmbed> lyricEmbeds = song.lyricEmbeds;
        if (lyricEmbeds == null) {
            responder.respond("Current song, '"+song.strippedFilename()+"', doesn't have lyrics");
        } else {
            responder.respond(lyricEmbeds.get(0), getPageButtons("lyrics"+songs.indexOf(song),1,songlistPages));
        }
    }
    private void lyricsCommand(@NotNull CommandResponder responder) {
        ifSongPlaying(responder, this::lyricsCommand);
    }
    private void lyricsCommand(@NotNull CommandResponder responder, @NotNull String search) {
        ifCloseMatch(responder, search, this::lyricsCommand);
    }

    private void playSongVoteExpire(@NotNull Message message, @NotNull HopperBotPlaylistSong song) {
        message.editMessage("~~"+message.getContentRaw()+"~~").queue();
        playSongVoteMessages.remove(message);
        song.voteExpiryTimer = null;
    }
    private void playSongCreateVote(@NotNull Message message, @NotNull HopperBotPlaylistSong song) {
        message.addReaction(playSong_VOTE_EMOJI).queue();
        final Timer expiry = new Timer();
        expiry.schedule(new TimerTask() {
            @Override
            public void run() {
                playSongVoteExpire(message,song);
            }
        }, 1000 * playSong_VOTE_EXPIRY_SECONDS);
        playSongVoteMessages.put(message,song);
        song.voteExpiryTimer = expiry;
    }
    private void playSongCommand(@NotNull CommandResponder responder, @NotNull User user, @NotNull String search) {
        final boolean vote;
        if (user.getIdLong() == getConfig().getBotOwnerId()) {
            vote = false;
        } else {
            final WhoListening whoListening = onlyUserOrGuildListening(user);
            if (whoListening == WhoListening.NOBODY) {
                responder.respond("You're not listening; request denied.");
                return;
            } else if (whoListening == WhoListening.OTHER_GUILDS) {
                responder.respond("Another guild is listening right now; require denied.");
                return;
            } else {
                vote = whoListening == WhoListening.ONLY_GUILD;
            }
        }
        final Map.Entry<HopperBotPlaylistSong,Double> searchResult = searchSongs(search);
        if (searchResult.getValue() < MIN_SEARCH_CONFIDENCE) {
            responder.respond("Could not find a close match. Try being more specific!");
        } else {
            final String songName = searchResult.getKey().strippedFilename();
            if (vote) {
                if (playSongVoteMessages.containsValue(searchResult.getKey())) {
                    responder.respond("This song has already been suggested recently; vote for it here: "+playSongVoteMessages.inverse().get(searchResult.getKey()).getJumpUrl());
                } else {
                    responder.respond("You're not the only person listening right now, so everyone else must agree to listen to this song first. " +
                                    "If everyone else listening reacts to this message with "+playSong_VOTE_EMOJI+" " +
                                    "within **"+playSong_VOTE_EXPIRY_SECONDS+" seconds**, " +
                                    "this song will be played: **"+songName+"**",
                            message -> playSongCreateVote(message,searchResult.getKey())
                    );
                }
            } else {
                playSong(searchResult.getKey(),0);
                responder.respond("Attempting to play closest match: "+songName);
            }
        }
    }

    @NotNull
    @CheckReturnValue
    private Button pageButton(@NotNull String commandName, @NotNull String action, @NotNull String emojiUnicode, boolean disabled) {
        final Button button = Button.of(ButtonStyle.PRIMARY,"playlist-"+commandName+"-"+action, Emoji.fromUnicode(emojiUnicode));
        if (disabled) {
            return button.asDisabled();
        }
        return button;
    }
    @NotNull
    @CheckReturnValue
    @Unmodifiable
    private List<Button> getPageButtons(@NotNull String commandName, int currentPage, @NotNull List<MessageEmbed> pages) {
        final boolean prev = currentPage == 1;
        final boolean next = currentPage == pages.size();
        return List.of(
                pageButton(commandName,"start","U+23EE",prev),
                pageButton(commandName,"previous","U+023EA",prev),
                pageButton(commandName,"next","U+023E9",next),
                pageButton(commandName,"end","U+023ED",next)
        );
    }
    @CheckReturnValue
    private int getPageNumber(@NotNull ButtonInteractionEvent event) {
        final MessageEmbed.AuthorInfo author = event.getMessage().getEmbeds().get(0).getAuthor();
        if (author == null || author.getName() == null) {
            return -1;
        }
        final String[] titleWords = author.getName().split(" ");
        return Integer.parseInt(titleWords[1].split("/")[0]);
    }
    private void runPageButton(@NotNull List<MessageEmbed> pages, @NotNull ButtonInteractionEvent event, @NotNull String @NotNull [] parts) {
        final int newPage = switch (parts[2]) {
            default -> 1;
            case "previous" -> getPageNumber(event)-1;
            case "next" -> getPageNumber(event)+1;
            case "end" -> pages.size();
        };
        event.editMessageEmbeds(pages.get(newPage-1)).setActionRow(getPageButtons(parts[1],newPage,pages)).queue();
    }

    public void runButtonCommand(@NotNull ButtonInteractionEvent event, @NotNull String @NotNull [] parts) {
        if (parts[1].equals("songlist")) {
            runPageButton(songlistPages,event,parts);
        } else if (parts[1].startsWith("lyrics")) {
            final List<MessageEmbed> lyricEmbeds = songs.get(Integer.parseInt(parts[1].substring("lyrics".length()))).lyricEmbeds;
            if (lyricEmbeds != null) {
                runPageButton(lyricEmbeds,event,parts);
            }
        }
    }

    @Override
    public void onEvent(@NotNull AudioEvent event) {
        if (event instanceof TrackEndEvent && ((TrackEndEvent) event).endReason.mayStartNext) {
            playNextSong();
        }
    }

    private void unsetPresence() {
        getJDA().getPresence().setActivity(null);
    }

    private void playNextSong(int attempts) {
        if (attempts < MAX_NEXT_SONG_ATTEMPTS) {
            if (anyoneListening) {
                playSong(nextSong(), attempts);
                return;
            } else {
                logGlobally("Nobody currently listening- pausing playback",featureEnum);
            }
        } else {
            logGlobally("Maximum attempts at playing the next song ("+MAX_NEXT_SONG_ATTEMPTS+") reached",featureEnum);
        }
        unsetPresence();
    }
    private void playNextSong() {
        playNextSong(0);
    }

    @Nullable
    @CheckReturnValue
    @CheckForNull
    private HopperBotPlaylistSong currentSong() {
        if (lastThreeSongs.isEmpty()) {
            return null;
        }
        return lastThreeSongs.get(lastThreeSongs.size()-1);
    }

    @NotNull
    @CheckReturnValue
    private HopperBotPlaylistSong nextSong() {
        HopperBotPlaylistSong song = null;
        boolean loop = true;
        while (loop) {
            final Iterator<HopperBotPlaylistSong> iterator = songs.iterator();
            for (int i = 0; i < random.nextInt(songs.size()); i++) {
                iterator.next();
            }
            song = iterator.next();
            if (!lastThreeSongs.contains(song)) {
                loop = false;
            }
        }
        return song;
    }

    private synchronized void playSong(@NotNull HopperBotPlaylistSong song, int attempts) {
        getJDA().getPresence().setActivity(Activity.listening(song.strippedFilename()));
        logGlobally("Now trying to play "+song.filename,featureEnum);
        if (song.voteExpiryTimer != null) {
            song.voteExpiryTimer.cancel();
            playSongVoteExpire(playSongVoteMessages.inverse().get(song),song);
        }

        final String songFileLocation = ABSOLUTE_SONGS_DIR_LOC+File.separator+song.filename;
        playerManager.loadItem(songFileLocation, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                logGlobally("Successfully loaded "+song.filename,featureEnum);
                player.playTrack(track);
                lastThreeSongs.add(song);
                if (lastThreeSongs.size() == 4) {
                    lastThreeSongs.remove(0);
                }
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                // Only individual tracks are loaded; playlists are not expected
            }

            @Override
            public void noMatches() {
                logGlobally("Couldn't find song at "+songFileLocation.replace(File.separator,"\\"+File.separator),featureEnum);
                playNextSong(attempts+1);
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                logGlobally("Failed loading "+song.filename,featureEnum);
                playNextSong(attempts+1);
            }
        });
    }

    @NotNull
    @CheckReturnValue
    private Map<String,TreeSet<String>> songsByAuthor() {
        final Map<String,TreeSet<String>> authorFields = new HashMap<>();
        songs.forEach((song) -> {
            final String author = song.authors.get(0);
            if (!authorFields.containsKey(author)) {
                authorFields.put(author,new TreeSet<>());
            }
            authorFields.get(author).add(song.title);
        });
        return authorFields;
    }
    @NotNull
    @CheckReturnValue
    private List<Map.Entry<String,TreeSet<String>>> sortedSongsByAuthor() {
        return new ArrayList<>(songsByAuthor().entrySet()).stream().sorted((firstEntry, secondEntry) -> {
            final int sizeDifference = secondEntry.getValue().size() - firstEntry.getValue().size();
            if (sizeDifference == 0) {
                return firstEntry.getKey().compareTo(secondEntry.getKey());
            }
            return sizeDifference;
        }).toList();
    }

    @NotNull
    @CheckReturnValue
    private EmbedBuilder pagedEmbedBase(@NotNull String title, int pageIndex, int maxPages) {
        return getEmbedBase().setTitle(title).setAuthor("Page "+(pageIndex+1)+"/"+(maxPages));
    }

    @NotNull
    @CheckReturnValue
    private List<Integer> songlistPages(@NotNull List<Map.Entry<String,TreeSet<String>>> authorFields) {
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

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        super.onReady(event);

        final JsonNode songsDataNode = getYAMLFile(featureEnum, SONGS_FILE_LOC, JsonNode.class);
        if (songsDataNode == null) {
            return;
        }
        songsDataNode.fields().forEachRemaining(song -> {
            Map<String,JsonNode> songJsonData = new HashMap<>();
            song.getValue().fields().forEachRemaining(field -> songJsonData.put(field.getKey(), field.getValue()));
            songs.add(new HopperBotPlaylistSong(song.getKey(), songJsonData, this));
        });
        logGlobally("Serialized songs",featureEnum);

        player.addListener(this);
        if (anyoneListening) {
            playNextSong();
        }

        final List<Map.Entry<String,TreeSet<String>>> authorFields = sortedSongsByAuthor();
        final List<Integer> pages = songlistPages(authorFields);
        final AtomicReference<EmbedBuilder> embedBuilder = new AtomicReference<>(pagedEmbedBase("Playlist",0,pages.size()+1));
        for (int authors = 0; authors < authorFields.size(); authors++) {
            if (pages.contains(authors)) {
                songlistPages.add(embedBuilder.get().build());
                embedBuilder.set(pagedEmbedBase("Playlist",songlistPages.size(),pages.size()+1));
            }
            embedBuilder.get().addField(authorFields.get(authors).getKey(),String.join("\n",authorFields.get(authors).getValue()),false);
        }
        songlistPages.add(embedBuilder.get().build());
    }

    @Nullable
    @CheckForNull
    @CheckReturnValue
    private VoiceChannel getVoiceChannelFor(@NotNull Guild guild) {
        final Map<String, JsonNode> config = getFeatureConfig(guild,featureEnum);
        if (config == null) {
            return null;
        }
        return guild.getVoiceChannelById(config.get("voice_channel").asLong());
    }

    @CheckReturnValue
    private boolean isAnyoneListeningIn(@Nullable VoiceChannel voiceChannel) {
        if (voiceChannel == null) {
            return false;
        }
        return voiceChannel.getMembers().stream().anyMatch(member -> !member.getUser().isBot());
    }
    @CheckReturnValue
    private boolean isAnyoneListeningIn(@NotNull Guild guild) {
        return isAnyoneListeningIn(getVoiceChannelFor(guild));
    }
    @CheckReturnValue
    private boolean findAnyoneListeningAtAll() {
        anyoneListening = guilds.stream().anyMatch(this::isAnyoneListeningIn);
        return anyoneListening;
    }

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        final VoiceChannel voiceChannel = getVoiceChannelFor(event.getGuild());
        if (voiceChannel == null) {
            logToGuild("Could not find playlist voice channel",event.getGuild());
        } else {
            event.getGuild().getAudioManager().openAudioConnection(voiceChannel);
            event.getGuild().getAudioManager().setSendingHandler(sendHandler);
            if (!anyoneListening) {
                anyoneListening = isAnyoneListeningIn(voiceChannel);
            }
        }
    }

    @Override
    public void onGuildVoiceJoin(@NotNull GuildVoiceJoinEvent event) {
        if (!anyoneListening && !event.getMember().getUser().isBot() && event.getChannelJoined() == getVoiceChannelFor(event.getGuild())) {
            anyoneListening = true;
            playNextSong();
        }
    }

    @Override
    public void onGuildVoiceLeave(@NotNull GuildVoiceLeaveEvent event) {
        if (usesFeature(event.getGuild(),featureEnum) && !findAnyoneListeningAtAll()) {
            player.stopTrack();
            lastThreeSongs.clear();
            unsetPresence();
        }
    }

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        System.out.println(event.getReactionEmote().getEmoji());
        System.out.println(playSong_VOTE_EMOJI);
        if (event.getReactionEmote().getEmoji().equals(playSong_VOTE_EMOJI)) {
            event.retrieveMessage().queue(message -> {
                if (playSongVoteMessages.containsKey(message)) {
                    event.getReaction().retrieveUsers().queue(usersWhoReacted -> {
                        if (guilds.stream().map(this::getVoiceChannelFor)
                                .filter(Objects::nonNull)
                                .flatMap(voiceChannel -> voiceChannel.getMembers().stream())
                                .noneMatch(member -> !member.getUser().isBot() && !usersWhoReacted.contains(member.getUser()))) {
                            final HopperBotPlaylistSong song = playSongVoteMessages.get(message);
                            message.reply("Vote won! Attempting to play "+song.strippedFilename()).queue();
                            playSong(song,0);
                        }
                    });
                }
            });
        }
    }
}
