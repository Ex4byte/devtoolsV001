package com.example.devtools.mixin;

import com.example.devtools.features.packets.InventoryViewer;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class MixinInventoryViewer {

    @Inject(
        method = "handleContainerContent(Lnet/minecraft/network/protocol/game/ClientboundContainerSetContentPacket;)V",
        at = @At("HEAD"),
        cancellable = false
    )
    private void devtools$onContainerContent(ClientboundContainerSetContentPacket packet, CallbackInfo ci) {
        InventoryViewer.INSTANCE.onContainerContent(packet);
    }

    @Inject(
        method = "handleSetEquipment(Lnet/minecraft/network/protocol/game/ClientboundSetEquipmentPacket;)V",
        at = @At("HEAD"),
        cancellable = false
    )
    private void devtools$onSetEquipment(ClientboundSetEquipmentPacket packet, CallbackInfo ci) {
        InventoryViewer.INSTANCE.onEquipment(packet);
    }
}
