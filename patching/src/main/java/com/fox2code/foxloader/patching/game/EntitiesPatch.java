/*
 * MIT License
 * 
 * Copyright (c) 2023-2026 Fox2Code
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
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

final class EntitiesPatch extends GamePatch {
    private static final String NetClientHandler = "net/minecraft/client/networking/NetClientHandler";
    private static final String EntityList = "net/minecraft/common/entity/EntityList";
    private static final String Entity = "net/minecraft/common/entity/Entity";
    private static final String World = "net/minecraft/common/world/World";
    private static final String EntityRendererManager = "net/minecraft/client/renderer/entity/EntityRendererManager";
    private static final String EntityRegistry = "com/fox2code/foxloader/registry/EntityRegistry";

    EntitiesPatch() {
        super(new String[]{NetClientHandler, EntityList, EntityRendererManager});
    }

    @Override
    public ClassNode transform(ClassNode classNode) {
        switch (classNode.name) {
            case NetClientHandler: {
                patchNetClientHandler(classNode);
                break;
            }
            case EntityList: {
                patchEntityList(classNode);
                break;
            }
            case EntityRendererManager: {
                TransformerUtils.makeFieldPublic(classNode, "entityRenderMap");
                break;
            }
        }
        return classNode;
    }

    private static void patchNetClientHandler(ClassNode classNode) {
        for (MethodNode methodNode : classNode.methods) {
            for (AbstractInsnNode abstractInsnNode : methodNode.instructions) {
                if (abstractInsnNode.getOpcode() == INVOKESTATIC) {
                    MethodInsnNode methodInsnNode = (MethodInsnNode) abstractInsnNode;
                    if (EntityList.equals(methodInsnNode.owner) &&
                            "createEntity".equals(methodInsnNode.name)) {
                        methodInsnNode.owner = EntityRegistry;
                        methodInsnNode.name = "createEntityRemote";
                    }
                }
            }
        }
    }

    private static void patchEntityList(ClassNode classNode) {
        MethodNode methodNode = TransformerUtils.getMethod(classNode, "addMapping");
        methodNode.access &= ~(ACC_PRIVATE | ACC_PROTECTED);
        methodNode.access |= ACC_PUBLIC;
    }
}
