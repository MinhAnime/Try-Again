package com.minhduong.command;

import com.minhduong.data.LanguageManager;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.ServerCommandSource;

public class LanguageCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(net.minecraft.server.command.CommandManager.literal("tryagain")
                .then(net.minecraft.server.command.CommandManager.literal("reload")
                        .requires(source -> true)
                        .executes(ctx -> {
                            LanguageManager.load();
                            ctx.getSource().sendMessage(com.minhduong.util.Messages.success("reloaded"));
                            return 1;
                        })
                )
        );
    }
}
