package net.earthcomputer.multiconnect.protocols.v1_12_2.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandSource;
import net.minecraft.util.registry.Registry;

import static net.earthcomputer.multiconnect.protocols.v1_12_2.command.Commands_1_12_2.*;
import static net.earthcomputer.multiconnect.protocols.v1_12_2.command.arguments.EntityArgumentType_1_12_2.*;
import static net.earthcomputer.multiconnect.protocols.v1_12_2.command.arguments.EnumArgumentType.*;
import static net.minecraft.command.argument.IdentifierArgumentType.*;

public class StopSoundCommand {

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(literal("stopsound")
            .then(argument("player", players())
                .executes(ctx -> 0)
                .then(argument("category", enumArg("master", "music", "record", "weather", "block", "hostile", "neutral", "player", "ambient", "voice"))
                    .executes(ctx -> 0)
                    .then(argument("sound", identifier())
                        .suggests((ctx, builder) -> CommandSource.suggestIdentifiers(Registry.SOUND_EVENT.getIds(), builder))
                        .executes(ctx -> 0)))));
    }

}
