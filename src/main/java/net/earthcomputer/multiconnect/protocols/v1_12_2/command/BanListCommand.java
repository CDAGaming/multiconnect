package net.earthcomputer.multiconnect.protocols.v1_12_2.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandSource;

import static net.earthcomputer.multiconnect.protocols.v1_12_2.command.Commands_1_12_2.*;

public class BanListCommand {

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(literal("banlist")
            .executes(ctx -> 0)
            .then(literal("players")
                .executes(ctx -> 0))
            .then(literal("ips")
                .executes(ctx -> 0)));
    }

}
