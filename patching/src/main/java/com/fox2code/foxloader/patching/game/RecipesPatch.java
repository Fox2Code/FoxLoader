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
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.Collections;

final class RecipesPatch extends GamePatch {
    private static final String Items = "net/minecraft/common/item/Items";
    private static final String ShapedRecipes = "net/minecraft/common/recipe/ShapedRecipes";
    private static final String ShapelessRecipes = "net/minecraft/common/recipe/ShapelessRecipes";
    private static final String RecipesDyes = "net/minecraft/common/recipe/RecipesDyes";
    private static final String TaggedIngredient = "net/minecraft/common/recipe/TaggedIngredient";
    private static final String FoxTaggedIngredients = "com/fox2code/foxloader/recipe/FoxTaggedIngredients";

    RecipesPatch() {
        super(new String[]{ShapedRecipes, ShapelessRecipes, RecipesDyes});
    }

    @Override
    public ClassNode transform(ClassNode classNode) {
        switch (classNode.name) {
            case ShapedRecipes: {
                TransformerUtils.makeGetterForFields(classNode, "width", "height", "ingredients");
                break;
            }
            case ShapelessRecipes: {
                TransformerUtils.makeGetterForFields(classNode, "ingredients");
                break;
            }
            case RecipesDyes: {
                patchAllDyeRecipes(classNode);
                break;
            }
        }
        return classNode;
    }

    private static void patchAllDyeRecipes(ClassNode classNode) {
        for (MethodNode methodNode : classNode.methods) {
            ArrayList<StaticDyeOccurrence> staticDyeOccurrences = new ArrayList<>();
            for (AbstractInsnNode abstractInsnNode : methodNode.instructions) {
                if (abstractInsnNode.getOpcode() == GETSTATIC) {
                    FieldInsnNode fieldInsnNode = (FieldInsnNode) abstractInsnNode;
                    if (Items.equals(fieldInsnNode.owner) && "DYE_POWDER".equals(fieldInsnNode.name)) {
                        AbstractInsnNode start = TransformerUtils.previousCodeInsn(fieldInsnNode, 2);
                        AbstractInsnNode varLoad = TransformerUtils.nextCodeInsn(fieldInsnNode, 2);
                        if (start.getOpcode() == NEW && varLoad.getOpcode() == ILOAD) {
                            AbstractInsnNode end = TransformerUtils.nextCodeInsn(varLoad);
                            if (end.getOpcode() == Opcodes.INVOKESPECIAL) {
                                staticDyeOccurrences.add(new StaticDyeOccurrence(start, end, varLoad));
                            }
                        }
                    }
                }
            }
            for (StaticDyeOccurrence staticDyeOccurrence : staticDyeOccurrences) {
                staticDyeOccurrence.patchOut(methodNode);
            }
        }
    }

    private static final class StaticDyeOccurrence {
        private final AbstractInsnNode from, to;
        private final AbstractInsnNode loadVar;

        private StaticDyeOccurrence(AbstractInsnNode from, AbstractInsnNode to, AbstractInsnNode loadVar) {
            this.from = from;
            this.to = to;
            this.loadVar = loadVar;
        }

        void patchOut(MethodNode methodNode) {
            AbstractInsnNode nextAfter = this.to.getNext();
            AbstractInsnNode previousBefore = this.from.getPrevious();
            TransformerUtils.removeInstructionsInRange(
                    methodNode.instructions, previousBefore, nextAfter);
            InsnList insnList = new InsnList();
            insnList.add(new FieldInsnNode(GETSTATIC, FoxTaggedIngredients, "DYES", "[L" + TaggedIngredient + ";"));
            insnList.add(this.loadVar.clone(Collections.emptyMap()));
            insnList.add(new InsnNode(AALOAD));
            methodNode.instructions.insert(previousBefore, insnList);
        }
    }
}
