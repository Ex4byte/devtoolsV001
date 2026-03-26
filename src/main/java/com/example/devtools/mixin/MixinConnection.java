package com.example.devtools.mixin;

import com.example.devtools.features.packets.PacketLogger;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

/**
 * Mixin in net.minecraft.network.Connection.
 *
 * Hookt:
 *  - channelRead0 → eingehende Pakete (Netty-Thread!)
 *  - send          → ausgehende Pakete
 *
 * WICHTIG: Nur lesen, NIEMALS Pakete modifizieren oder canceln!
 */
@Mixin(Connection.class)
public class MixinConnection {

    @Shadow
    private PacketFlow receiving;

    @Inject(
        method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V",
        at = @At("HEAD"),
        cancellable = false
    )
    private void devtools$onPacketReceived(ChannelHandlerContext ctx, Packet<?> packet, CallbackInfo ci) {
        if (receiving == PacketFlow.CLIENTBOUND) {
            PacketLogger.INSTANCE.logIncoming(packet);
        }
    }

    @Inject(
        method = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V",
        at = @At("HEAD")
    )
    private void devtools$onPacketSent(Packet<?> packet, @Nullable PacketSendListener listener, CallbackInfo ci) {
        PacketLogger.INSTANCE.logOutgoing(packet);
    }
}
