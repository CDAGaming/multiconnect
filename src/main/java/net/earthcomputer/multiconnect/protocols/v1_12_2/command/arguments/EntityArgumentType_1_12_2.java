package net.earthcomputer.multiconnect.protocols.v1_12_2.command.arguments;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.earthcomputer.multiconnect.protocols.v1_12_2.TabCompletionManager;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.command.arguments.EntitySummonArgument;
import net.minecraft.entity.EntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class EntityArgumentType_1_12_2 implements ArgumentType<Void> {

    private static final Collection<String> EXAMPLES = Arrays.asList("Player", "0123", "@e", "@e[type=foo]", "dd12be42-52a9-4a91-a8a1-11c01849e498");

    private static final SimpleCommandExceptionType EXPECTED_SELECTOR_TYPE_EXCEPTION = new SimpleCommandExceptionType(new LiteralMessage("Expected selector type"));
    private static final DynamicCommandExceptionType UNKNOWN_OPTION_EXCEPTION = new DynamicCommandExceptionType(arg -> new LiteralMessage("Unknown option \"" + arg + "\""));
    private static final DynamicCommandExceptionType DUPLICATE_OPTION_EXCEPTION = new DynamicCommandExceptionType(arg -> new LiteralMessage("Duplicate option \"" + arg + "\""));
    private static final DynamicCommandExceptionType DISALLOWED_OPTION_EXCEPTION = new DynamicCommandExceptionType(arg -> new LiteralMessage("Option \"" + arg + "\" is disallowed at this location"));
    private static final SimpleCommandExceptionType NO_MULTIPLE_EXCEPTION = new SimpleCommandExceptionType(new LiteralMessage("Cannot match multiple entities here"));
    private static final SimpleCommandExceptionType PLAYERS_ONLY_EXCEPTION = new SimpleCommandExceptionType(new LiteralMessage("This selector cannot match players, but only players are allowed"));

    private static final Map<String, Option> SELECTOR_OPTIONS = new HashMap<>();

    private final boolean singleTarget;
    private final boolean playersOnly;
    private boolean suggestPlayerNames = true;

    private EntityArgumentType_1_12_2(boolean singleTarget, boolean playersOnly) {
        this.singleTarget = singleTarget;
        this.playersOnly = playersOnly;
    }

    public static EntityArgumentType_1_12_2 players() {
        return new EntityArgumentType_1_12_2(false, true);
    }

    public static EntityArgumentType_1_12_2 onePlayer() {
        return new EntityArgumentType_1_12_2(true, true);
    }

    public static EntityArgumentType_1_12_2 entities() {
        return new EntityArgumentType_1_12_2(false, false);
    }

    public static EntityArgumentType_1_12_2 oneEntity() {
        return new EntityArgumentType_1_12_2(true, false);
    }

    public EntityArgumentType_1_12_2 noSuggestPlayerNames() {
        this.suggestPlayerNames = false;
        return this;
    }

    @Override
    public Void parse(StringReader reader) throws CommandSyntaxException {
        if (reader.canRead() && reader.peek() == '@') {
            new EntitySelectorParser(reader, singleTarget, playersOnly).parse();
        } else {
            reader.readUnquotedString();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        if (!(context.getSource() instanceof ISuggestionProvider))
            return builder.buildFuture();

        StringReader reader = new StringReader(builder.getInput());
        reader.setCursor(builder.getStart());

        CompletableFuture<Suggestions> playerCompletions;
        if ((reader.canRead() && reader.peek() == '@') || !suggestPlayerNames) {
            playerCompletions = Suggestions.empty();
        } else {
            playerCompletions = ((ISuggestionProvider) context.getSource()).getSuggestionsFromServer((CommandContext<ISuggestionProvider>) context, builder.restart());
        }

        EntitySelectorParser parser = new EntitySelectorParser(reader, singleTarget, playersOnly);
        try {
            parser.parse();
        } catch (CommandSyntaxException ignore) {
        }
        CompletableFuture<Suggestions> selectorCompletions = parser.suggestor.apply(builder.restart());

        return CompletableFuture.allOf(playerCompletions, selectorCompletions)
                .thenCompose(v -> UnionArgumentType.mergeSuggestions(playerCompletions.join(), selectorCompletions.join()));
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    private static class EntitySelectorParser {
        private final StringReader reader;
        private boolean singleTarget;
        private boolean playersOnly;

        private Function<SuggestionsBuilder, CompletableFuture<Suggestions>> suggestor = SuggestionsBuilder::buildFuture;

        private boolean cannotSelectPlayers = false;
        private boolean typeKnown = false;
        private Set<String> seenOptions = new HashSet<>();

        public EntitySelectorParser(StringReader reader, boolean singleTarget, boolean playersOnly) {
            this.reader = reader;
            this.singleTarget = singleTarget;
            this.playersOnly = playersOnly;
        }

        public void parse() throws CommandSyntaxException {
            int start = reader.getCursor();
            suggestor = builder -> {
                builder = builder.createOffset(start);
                builder.suggest("@p");
                if (!singleTarget)
                    builder.suggest("@a");
                builder.suggest("@r");
                if (!playersOnly)
                    builder.suggest("@e");
                builder.suggest("@s");
                return builder.buildFuture();
            };
            reader.expect('@');
            if (!reader.canRead()) {
                reader.setCursor(start);
                throw EXPECTED_SELECTOR_TYPE_EXCEPTION.createWithContext(reader);
            }
            switch (reader.read()) {
                case 'p':
                    singleTarget = true;
                    playersOnly = true;
                    typeKnown = true;
                    break;
                case 'a':
                    if (singleTarget) {
                        reader.setCursor(start);
                        throw NO_MULTIPLE_EXCEPTION.createWithContext(reader);
                    }
                    playersOnly = true;
                    typeKnown = true;
                    break;
                case 'r':
                    typeKnown = true;
                    break;
                case 'e':
                    break;
                case 's':
                    typeKnown = true;
                    singleTarget = true;
                    break;
                default:
                    reader.setCursor(start);
                    throw EXPECTED_SELECTOR_TYPE_EXCEPTION.createWithContext(reader);
            }

            int bracketStart = reader.getCursor();
            if (!reader.canRead()) {
                suggestor = builder -> {
                    builder = builder.createOffset(bracketStart);
                    builder.suggest("[");
                    return builder.buildFuture();
                };
                return;
            }

            if (reader.canRead() && reader.peek() == '[') {
                reader.skip();
                if (reader.canRead() && reader.peek() == ']') {
                    reader.skip();
                } else {
                    while (true) {
                        readOption();
                        if (reader.canRead() && reader.peek() == ',') {
                            reader.skip();
                        } else {
                            reader.expect(']');
                            break;
                        }
                    }
                }
            }

            suggestor = SuggestionsBuilder::buildFuture;
        }

        private void readOption() throws CommandSyntaxException {
            suggestOption();

            int start = reader.getCursor();
            String optionName = reader.readUnquotedString();
            if (!optionName.startsWith("score_") && !SELECTOR_OPTIONS.containsKey(optionName)) {
                reader.setCursor(start);
                throw UNKNOWN_OPTION_EXCEPTION.createWithContext(reader, optionName);
            }
            if (seenOptions.contains(optionName)) {
                reader.setCursor(start);
                throw DUPLICATE_OPTION_EXCEPTION.createWithContext(reader, optionName);
            }
            if (!optionName.startsWith("score_") && !SELECTOR_OPTIONS.get(optionName).isAllowed(this)) {
                reader.setCursor(start);
                throw DISALLOWED_OPTION_EXCEPTION.createWithContext(reader, optionName);
            }
            seenOptions.add(optionName);

            reader.expect('=');

            suggestor = SuggestionsBuilder::buildFuture;

            if (optionName.startsWith("score_")) {
                reader.readInt();
            } else {
                SELECTOR_OPTIONS.get(optionName).parse(this);
            }
        }

        private void suggestOption() {
            int start = reader.getCursor();
            List<String> seenOptionsCopy = new ArrayList<>(seenOptions);

            suggestor = builder -> {
                SuggestionsBuilder normalOptionBuilder = builder.createOffset(start);
                ISuggestionProvider.suggest(SELECTOR_OPTIONS.keySet().stream()
                        .filter(opt -> SELECTOR_OPTIONS.get(opt).isAllowed(this))
                        .filter(opt -> !seenOptionsCopy.contains(opt))
                        .map(opt -> opt + "=")
                        .collect(Collectors.toSet()), normalOptionBuilder);
                CompletableFuture<Suggestions> normalOptions = normalOptionBuilder.buildFuture();

                SuggestionsBuilder scoreOptionBuilder = builder.createOffset(start);
                CompletableFuture<Suggestions> scoreOptions = getScoreObjectives().thenCompose(objectives -> {
                    ISuggestionProvider.suggest(objectives.stream()
                            .map(str -> "score_" + str)
                            .filter(str -> !seenOptionsCopy.contains(str))
                            .map(str -> str + "="),
                            scoreOptionBuilder);
                    ISuggestionProvider.suggest(objectives.stream()
                            .map(str -> "score_" + str + "_min")
                            .filter(str -> !seenOptionsCopy.contains(str))
                            .map(str -> str + "="),
                            scoreOptionBuilder);
                    return scoreOptionBuilder.buildFuture();
                });

                return CompletableFuture.allOf(normalOptions, scoreOptions)
                        .thenCompose(v -> UnionArgumentType.mergeSuggestions(normalOptions.join(), scoreOptions.join()));
            };
        }

        private CompletableFuture<List<String>> getScoreObjectives() {
            return TabCompletionManager.requestCustomCompletion("/scoreboard objectives remove ");
        }

        private void parseInt(int min, int max) throws CommandSyntaxException {
            int start = reader.getCursor();
            int val = reader.readInt();
            if (val < min) {
                reader.setCursor(start);
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.integerTooLow().createWithContext(reader, min, val);
            }
            if (val > max) {
                reader.setCursor(start);
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.integerTooHigh().createWithContext(reader, max, val);
            }
        }

        private boolean parseIsInverted() {
            if (reader.canRead() && reader.peek() == '!') {
                reader.skip();
                return true;
            }
            return false;
        }
    }

    static {
        SELECTOR_OPTIONS.put("r", parser -> parser.parseInt(0, Integer.MAX_VALUE));
        SELECTOR_OPTIONS.put("rm", parser -> parser.parseInt(0, Integer.MAX_VALUE));
        SELECTOR_OPTIONS.put("l", new Option() {
            @Override
            public void parse(EntitySelectorParser parser) throws CommandSyntaxException {
                parser.playersOnly = true;
                parser.typeKnown = true;
                parser.parseInt(0, Integer.MAX_VALUE);
            }

            @Override
            public boolean isAllowed(EntitySelectorParser parser) {
                return !parser.cannotSelectPlayers;
            }
        });
        SELECTOR_OPTIONS.put("lm", new Option() {
            @Override
            public void parse(EntitySelectorParser parser) throws CommandSyntaxException {
                parser.playersOnly = true;
                parser.typeKnown = true;
                parser.parseInt(0, Integer.MAX_VALUE);
            }

            @Override
            public boolean isAllowed(EntitySelectorParser parser) {
                return !parser.cannotSelectPlayers;
            }
        });
        SELECTOR_OPTIONS.put("x", parser -> parser.reader.readInt());
        SELECTOR_OPTIONS.put("y", parser -> parser.reader.readInt());
        SELECTOR_OPTIONS.put("z", parser -> parser.reader.readInt());
        SELECTOR_OPTIONS.put("dx", parser -> parser.reader.readInt());
        SELECTOR_OPTIONS.put("dy", parser -> parser.reader.readInt());
        SELECTOR_OPTIONS.put("dz", parser -> parser.reader.readInt());
        SELECTOR_OPTIONS.put("rx", parser -> parser.parseInt(-90, 90));
        SELECTOR_OPTIONS.put("rxm", parser -> parser.parseInt(-90, 90));
        SELECTOR_OPTIONS.put("ry", parser -> parser.parseInt(-360, 360));
        SELECTOR_OPTIONS.put("rym", parser -> parser.parseInt(-360, 360));
        SELECTOR_OPTIONS.put("c", parser -> {
            int start = parser.reader.getCursor();
            int val = parser.reader.readInt();
            boolean multiple = val != -1 && val != 1;
            if (parser.singleTarget && multiple) {
                parser.reader.setCursor(start);
                throw NO_MULTIPLE_EXCEPTION.createWithContext(parser.reader);
            }
            if (!multiple)
                parser.singleTarget = true;
        });
        SELECTOR_OPTIONS.put("m", new Option() {
            @Override
            public void parse(EntitySelectorParser parser) throws CommandSyntaxException {
                int start = parser.reader.getCursor();
                parser.playersOnly = true;
                parser.typeKnown = true;
                parser.suggestor = builder -> {
                    builder = builder.createOffset(start);
                    builder.suggest(0);
                    builder.suggest(1);
                    builder.suggest(2);
                    builder.suggest(3);
                    return builder.buildFuture();
                };
                parser.parseInt(0, 3);
            }

            @Override
            public boolean isAllowed(EntitySelectorParser parser) {
                return !parser.cannotSelectPlayers;
            }
        });
        SELECTOR_OPTIONS.put("team", parser -> {
            int start = parser.reader.getCursor();
            parser.suggestor = builder -> {
                SuggestionsBuilder newBuilder = builder.createOffset(start);
                return TabCompletionManager.requestCustomCompletion("/scoreboard teams remove ").thenCompose(teams -> {
                    ISuggestionProvider.suggest(teams, newBuilder);
                    ISuggestionProvider.suggest(teams.stream().map(str -> "!" + str), newBuilder);
                    return newBuilder.buildFuture();
                });
            };
            parser.parseIsInverted();
            parser.reader.readUnquotedString();
        });
        SELECTOR_OPTIONS.put("name", parser -> {
            parser.parseIsInverted();
            parser.reader.readUnquotedString();
        });
        SELECTOR_OPTIONS.put("type", new Option() {
            @Override
            public void parse(EntitySelectorParser parser) throws CommandSyntaxException {
                int start = parser.reader.getCursor();
                parser.suggestor = builder -> {
                    builder = builder.createOffset(start);
                    if (parser.playersOnly) {
                        ISuggestionProvider.suggestIterable(Collections.singleton(new ResourceLocation("player")), builder);
                    } else {
                        ISuggestionProvider.suggestIterable(Registry.ENTITY_TYPE.keySet(), builder);
                        ISuggestionProvider.suggestIterable(Registry.ENTITY_TYPE.keySet(), builder, "!");
                    }
                    return builder.buildFuture();
                };
                boolean inverted = parser.parseIsInverted();
                ResourceLocation entityId = ResourceLocation.read(parser.reader);
                if (!Registry.ENTITY_TYPE.containsKey(entityId)) {
                    parser.reader.setCursor(start);
                    throw EntitySummonArgument.ENTITY_UNKNOWN_TYPE.createWithContext(parser.reader, entityId);
                }
                if (!inverted) {
                    parser.typeKnown = true;
                    if (Registry.ENTITY_TYPE.getOrDefault(entityId) == EntityType.PLAYER) {
                        parser.playersOnly = true;
                    } else {
                        parser.cannotSelectPlayers = true;
                    }
                }
                if (!parser.typeKnown || parser.cannotSelectPlayers) {
                    parser.reader.setCursor(start);
                    throw PLAYERS_ONLY_EXCEPTION.createWithContext(parser.reader);
                }
            }

            @Override
            public boolean isAllowed(EntitySelectorParser parser) {
                return !parser.typeKnown;
            }
        });
        SELECTOR_OPTIONS.put("tag", parser -> {
            parser.parseIsInverted();
            parser.reader.readUnquotedString();
        });
    }

    @FunctionalInterface
    private interface Option {
        void parse(EntitySelectorParser parser) throws CommandSyntaxException;

        default boolean isAllowed(EntitySelectorParser parser) {
            return true;
        }
    }
}
