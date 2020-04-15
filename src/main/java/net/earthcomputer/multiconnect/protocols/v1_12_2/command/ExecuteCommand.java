package net.earthcomputer.multiconnect.protocols.v1_12_2.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.ISuggestionProvider;

import static net.earthcomputer.multiconnect.protocols.v1_12_2.command.Commands_1_12_2.*;
import static net.earthcomputer.multiconnect.protocols.v1_12_2.command.arguments.BlockStateArgumentType_1_12_2.*;
import static net.earthcomputer.multiconnect.protocols.v1_12_2.command.arguments.CommandArgumentType.*;
import static net.earthcomputer.multiconnect.protocols.v1_12_2.command.arguments.EntityArgumentType_1_12_2.*;
import static net.minecraft.command.arguments.BlockPosArgument.blockPos;
import static net.minecraft.command.arguments.Vec3Argument.vec3;

public class ExecuteCommand {

    public static void register(CommandDispatcher<ISuggestionProvider> dispatcher) {
        dispatcher.register(literal("execute")
            .then(argument("entity", entities())
                .then(argument("pos", vec3())
                    .then(literal("detect")
                        .then(argument("detectPos", blockPos())
                            .then(argument("block", testBlockState())
                                .then(argument("command", command(dispatcher))))))
                    .then(argument("command", command(dispatcher))))));
    }

}
