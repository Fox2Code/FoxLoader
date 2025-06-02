package com.fox2code.foxloader.registry.missing;

import com.fox2code.foxloader.registry.GameRegistry;
import net.minecraft.common.item.ItemStack;
import net.minecraft.common.item.description.ItemDesc;
import net.minecraft.common.util.ChatColors;
import net.minecraft.common.world.World;

import java.util.List;

public class MissingItemDesc implements ItemDesc {
    @Override
    public void runDesc(World world, List<String> list, ItemStack itemStack) {
        String remoteRawId = null;
        boolean isBlock = false;
        if (world.isRemote) {
            int remoteItemId = itemStack.getRemoteItemID();
            if (!GameRegistry.isLoaderReservedItemId(remoteItemId)) {
                return;
            }
            remoteRawId = GameRegistry.itemIdMappingInNames[remoteItemId];
            isBlock = GameRegistry.isItemBlock(remoteItemId);
        }
        if (remoteRawId == null) {
            int itemId = itemStack.getItemID();
            remoteRawId = GameRegistry.itemIdMappingLocalNames[itemId];
            isBlock = GameRegistry.isItemBlock(itemId);
            if (remoteRawId == null) return;
        }
        if ("foxloader:item_missing".equals(remoteRawId) ||
                "foxloader:block_missing".equals(remoteRawId)) {
            list.add(ChatColors.GRAY + st.translateKey(isBlock ?
                    "tile.foxloader.block_missing.desc" :
                    "item.foxloader.item_missing.desc"));
            return;
        }
        list.add(ChatColors.GRAY + st.translateKey(isBlock ?
                "tile.foxloader.block_missing.missing_desc" :
                "item.foxloader.item_missing.missing_desc"));
        list.add(ChatColors.GRAY + " " + remoteRawId);
    }
}
