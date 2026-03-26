package com.example.devtools.features.packets;

import com.example.devtools.core.IDevFeature;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.common.Mod;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = "devtools", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class InventoryViewer implements IDevFeature {

    public static final InventoryViewer INSTANCE = new InventoryViewer();

    /** Entity-ID → Equipment [0=MainHand,1=OffHand,2=Feet,3=Legs,4=Chest,5=Head] */
    private final Map<Integer, ItemStack[]> equipmentCache = new ConcurrentHashMap<>();
    private final Map<Integer, List<ItemStack>> containerCache = new ConcurrentHashMap<>();

    private boolean enabled = false;

    private InventoryViewer() {}

    @Override public String getId()          { return "inventory_viewer"; }
    @Override public String getDisplayName() { return "Inventory Viewer"; }
    @Override public boolean isEnabled()     { return enabled; }
    @Override public void setEnabled(boolean e) { this.enabled = e; }

    // Aufgerufen von MixinInventoryViewer
    public void onContainerContent(ClientboundContainerSetContentPacket packet) {
        if (!enabled) return;
        containerCache.put(packet.getContainerId(), new ArrayList<>(packet.getItems()));
    }

    public void onEquipment(ClientboundSetEquipmentPacket packet) {
        if (!enabled) return;
        ItemStack[] slots = equipmentCache.computeIfAbsent(packet.getEntity(), id -> new ItemStack[6]);
        packet.getSlots().forEach(pair -> {
            int idx = pair.getFirst().getIndex();
            if (idx >= 0 && idx < 6) slots[idx] = pair.getSecond().copy();
        });
    }

    public ItemStack[]       getEquipment(int entityId)  { return equipmentCache.get(entityId); }
    public List<ItemStack>   getContainer(int id)        { return containerCache.getOrDefault(id, Collections.emptyList()); }
    public Map<Integer, ItemStack[]> getAllEquipment()   { return Collections.unmodifiableMap(equipmentCache); }
    public void clearCache() { equipmentCache.clear(); containerCache.clear(); }

    public String getEquipmentSummary(Player target) {
        ItemStack[] eq = equipmentCache.get(target.getId());
        if (eq == null) return "§7(kein Equipment gecacht)";
        String[] names = {"Hand", "Offhand", "Schuhe", "Hose", "Brust", "Helm"};
        StringBuilder sb = new StringBuilder();
        for (int i = 5; i >= 0; i--) {
            if (eq[i] != null && !eq[i].isEmpty())
                sb.append("§7").append(names[i]).append(": §f")
                  .append(eq[i].getDisplayName().getString()).append("\n");
        }
        return sb.length() > 0 ? sb.toString() : "§7(leer)";
    }
}
