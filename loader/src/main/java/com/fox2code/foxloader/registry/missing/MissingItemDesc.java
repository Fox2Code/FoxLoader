/*
 * MIT License
 * 
 * Copyright (c) 2023-2025 Fox2Code
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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
