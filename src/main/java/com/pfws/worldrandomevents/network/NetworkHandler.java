package com.pfws.worldrandomevents.network;

import com.pfws.worldrandomevents.WorldRandomEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public class NetworkHandler {

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(EventStartPayload.TYPE, EventStartPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(EventEndPayload.TYPE, EventEndPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SkyEffectPayload.TYPE, SkyEffectPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ScreenShakePayload.TYPE, ScreenShakePayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ParticleEffectPayload.TYPE, ParticleEffectPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(TitleMessagePayload.TYPE, TitleMessagePayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SoundEffectPayload.TYPE, SoundEffectPayload.STREAM_CODEC);
    }

    public static void sendEventStart(ServerPlayer player, String eventId, int durationTicks) {
        ServerPlayNetworking.send(player, new EventStartPayload(eventId, durationTicks));
    }

    public static void sendEventEnd(ServerPlayer player, String eventId) {
        ServerPlayNetworking.send(player, new EventEndPayload(eventId));
    }

    public static void sendSkyEffect(ServerPlayer player, int skyColor, int fogColor) {
        ServerPlayNetworking.send(player, new SkyEffectPayload(skyColor, fogColor));
    }

    public static void sendScreenShake(ServerPlayer player, float intensity, int durationTicks) {
        ServerPlayNetworking.send(player, new ScreenShakePayload(intensity, durationTicks));
    }

    public static void sendParticleEffect(ServerPlayer player, String particleType, int count, double x, double y, double z) {
        ServerPlayNetworking.send(player, new ParticleEffectPayload(particleType, count, x, y, z));
    }

    public static void sendTitleMessage(ServerPlayer player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        ServerPlayNetworking.send(player, new TitleMessagePayload(title, subtitle, fadeIn, stay, fadeOut));
    }

    public static void sendSoundEffect(ServerPlayer player, String soundId, double x, double y, double z, float volume, float pitch) {
        ServerPlayNetworking.send(player, new SoundEffectPayload(soundId, x, y, z, volume, pitch));
    }

    public record EventStartPayload(String eventId, int durationTicks) implements CustomPacketPayload {
        public static final Type<EventStartPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(WorldRandomEvents.MOD_ID, "event_start"));
        public static final StreamCodec<FriendlyByteBuf, EventStartPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, EventStartPayload::eventId,
            ByteBufCodecs.VAR_INT, EventStartPayload::durationTicks,
            EventStartPayload::new
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record EventEndPayload(String eventId) implements CustomPacketPayload {
        public static final Type<EventEndPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(WorldRandomEvents.MOD_ID, "event_end"));
        public static final StreamCodec<FriendlyByteBuf, EventEndPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, EventEndPayload::eventId,
            EventEndPayload::new
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record SkyEffectPayload(int skyColor, int fogColor) implements CustomPacketPayload {
        public static final Type<SkyEffectPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(WorldRandomEvents.MOD_ID, "sky_effect"));
        public static final StreamCodec<FriendlyByteBuf, SkyEffectPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SkyEffectPayload::skyColor,
            ByteBufCodecs.VAR_INT, SkyEffectPayload::fogColor,
            SkyEffectPayload::new
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ScreenShakePayload(float intensity, int durationTicks) implements CustomPacketPayload {
        public static final Type<ScreenShakePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(WorldRandomEvents.MOD_ID, "screen_shake"));
        public static final StreamCodec<FriendlyByteBuf, ScreenShakePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, ScreenShakePayload::intensity,
            ByteBufCodecs.VAR_INT, ScreenShakePayload::durationTicks,
            ScreenShakePayload::new
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ParticleEffectPayload(String particleType, int count, double x, double y, double z) implements CustomPacketPayload {
        public static final Type<ParticleEffectPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(WorldRandomEvents.MOD_ID, "particle_effect"));
        public static final StreamCodec<FriendlyByteBuf, ParticleEffectPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ParticleEffectPayload::particleType,
            ByteBufCodecs.VAR_INT, ParticleEffectPayload::count,
            ByteBufCodecs.DOUBLE, ParticleEffectPayload::x,
            ByteBufCodecs.DOUBLE, ParticleEffectPayload::y,
            ByteBufCodecs.DOUBLE, ParticleEffectPayload::z,
            ParticleEffectPayload::new
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record TitleMessagePayload(String title, String subtitle, int fadeIn, int stay, int fadeOut) implements CustomPacketPayload {
        public static final Type<TitleMessagePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(WorldRandomEvents.MOD_ID, "title_message"));
        public static final StreamCodec<FriendlyByteBuf, TitleMessagePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, TitleMessagePayload::title,
            ByteBufCodecs.STRING_UTF8, TitleMessagePayload::subtitle,
            ByteBufCodecs.VAR_INT, TitleMessagePayload::fadeIn,
            ByteBufCodecs.VAR_INT, TitleMessagePayload::stay,
            ByteBufCodecs.VAR_INT, TitleMessagePayload::fadeOut,
            TitleMessagePayload::new
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record SoundEffectPayload(String soundId, double x, double y, double z, float volume, float pitch) implements CustomPacketPayload {
        public static final Type<SoundEffectPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(WorldRandomEvents.MOD_ID, "sound_effect"));
        public static final StreamCodec<FriendlyByteBuf, SoundEffectPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SoundEffectPayload::soundId,
            ByteBufCodecs.DOUBLE, SoundEffectPayload::x,
            ByteBufCodecs.DOUBLE, SoundEffectPayload::y,
            ByteBufCodecs.DOUBLE, SoundEffectPayload::z,
            ByteBufCodecs.FLOAT, SoundEffectPayload::volume,
            ByteBufCodecs.FLOAT, SoundEffectPayload::pitch,
            SoundEffectPayload::new
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
}