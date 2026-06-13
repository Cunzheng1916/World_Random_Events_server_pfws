package com.pfws.worldrandomevents.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.pfws.worldrandomevents.WorldRandomEvents;
import com.pfws.worldrandomevents.event.BaseEvent;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class EventCommands {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("worldevents")
                .requires(src -> Commands.LEVEL_GAMEMASTERS.check(src.permissions()))
                .then(Commands.literal("start")
                    .then(Commands.argument("event", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            for (BaseEvent event : WorldRandomEvents.EVENT_MANAGER.getAllEvents()) {
                                builder.suggest(event.getId());
                            }
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            String eventId = StringArgumentType.getString(ctx, "event");
                            WorldRandomEvents.EVENT_MANAGER.forceStartEvent(eventId);
                            ctx.getSource().sendSuccess(() ->
                                Component.literal("§a事件已强制启动: " + eventId), false);
                            return 1;
                        })
                    )
                )
                .then(Commands.literal("stop")
                    .executes(ctx -> {
                        BaseEvent current = WorldRandomEvents.EVENT_MANAGER.getCurrentEvent();
                        if (current != null && current.isActive()) {
                            String name = current.getDisplayName();
                            WorldRandomEvents.EVENT_MANAGER.forceEndCurrentEvent();
                            ctx.getSource().sendSuccess(() ->
                                Component.literal("§a当前事件已停止: " + name), false);
                        } else {
                            ctx.getSource().sendFailure(Component.literal("§c当前没有正在运行的事件"));
                        }
                        return 1;
                    })
                )
                .then(Commands.literal("reset")
                    .then(Commands.argument("event", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            for (BaseEvent event : WorldRandomEvents.EVENT_MANAGER.getAllEvents()) {
                                builder.suggest(event.getId());
                            }
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            String eventId = StringArgumentType.getString(ctx, "event");
                            WorldRandomEvents.EVENT_MANAGER.resetEventCooldown(eventId);
                            ctx.getSource().sendSuccess(() ->
                                Component.literal("§a事件冷却已重置: " + eventId), false);
                            return 1;
                        })
                    )
                )
                .then(Commands.literal("list")
                    .executes(ctx -> {
                        StringBuilder sb = new StringBuilder("§6=== 世界事件列表 ===\n");
                        for (BaseEvent event : WorldRandomEvents.EVENT_MANAGER.getAllEvents()) {
                            String status = event.isActive() ? "§c[运行中]" :
                                (event.getCooldownTicks() > 0 ? "§7[冷却中]" : "§a[就绪]");
                            sb.append("  §e").append(event.getId())
                                .append(" §8- ").append(event.getDisplayName())
                                .append(" ").append(status).append("\n");
                        }
                        BaseEvent current = WorldRandomEvents.EVENT_MANAGER.getCurrentEvent();
                        if (current != null && current.isActive()) {
                            sb.append("\n§6当前事件: §c").append(current.getDisplayName());
                        }
                        String msg = sb.toString();
                        ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
                        return 1;
                    })
                )
                .then(Commands.literal("status")
                    .executes(ctx -> {
                        BaseEvent current = WorldRandomEvents.EVENT_MANAGER.getCurrentEvent();
                        if (current != null && current.isActive()) {
                            ctx.getSource().sendSuccess(() ->
                                Component.literal("§6当前事件: §c" + current.getDisplayName()
                                    + " §8(" + current.getId() + ")"), false);
                        } else {
                            ctx.getSource().sendSuccess(() ->
                                Component.literal("§a当前没有正在运行的世界事件"), false);
                        }
                        return 1;
                    })
                )
                .then(Commands.literal("reload")
                    .executes(ctx -> {
                        WorldRandomEvents.CONFIG = com.pfws.worldrandomevents.config.ModConfig.load();
                        ctx.getSource().sendSuccess(() ->
                            Component.literal("§a配置已重新加载"), false);
                        return 1;
                    })
                )
            );
        });
    }
}