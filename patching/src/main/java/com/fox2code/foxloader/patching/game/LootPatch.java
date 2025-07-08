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
