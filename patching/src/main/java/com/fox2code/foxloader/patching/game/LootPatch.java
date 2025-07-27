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
package com.fox2code.foxloader.patching.game;

import com.fox2code.foxloader.patching.TransformerUtils;
import org.objectweb.asm.tree.ClassNode;

final class LootPatch extends GamePatch {
    private static final String TileEntityDungeonChest = "net/minecraft/common/block/tileentity/TileEntityDungeonChest";
    private static final String EntityCucurboo = "net/minecraft/common/entity/animals/EntityCucurboo";

    LootPatch() {
        super(new String[]{TileEntityDungeonChest, EntityCucurboo});
    }

    @Override
    public ClassNode transform(ClassNode classNode) {
        switch (classNode.name) {
            case TileEntityDungeonChest: {
                TransformerUtils.makeFieldPublic(classNode, "DUNGEON_LOOT_TABLE");
                break;
            }
            case EntityCucurboo: {
                TransformerUtils.makeFieldPublic(classNode, "CUCURBOO_LOOT_TABLE");
                break;
            }
        }
        return classNode;
    }
}
