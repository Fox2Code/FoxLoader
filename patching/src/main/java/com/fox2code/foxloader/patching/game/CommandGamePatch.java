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
import org.objectweb.asm.tree.*;

final class CommandGamePatch extends GamePatch {
    private static final String PlayerCommandHandler = "net/minecraft/common/command/PlayerCommandHandler";
    private static final String ClientPlayerCommandHandler = "net/minecraft/client/command/ClientPlayerCommandHandler";
    private static final String ServerPlayerCommandHandler = "net/minecraft/server/command/ServerPlayerCommandHandler";
    private static final String CommandRegistry$Internal = "com/fox2code/foxloader/registry/CommandRegistry$Internal";
    private static final String GameRegistry = "com/fox2code/foxloader/registry/GameRegistry";
    private static final String CommandCompletionRegistry$Item =
            "net/minecraft/common/command/completion/CommandCompletionRegistry$Type$1";

    CommandGamePatch() {
        super(new String[]{PlayerCommandHandler, ClientPlayerCommandHandler, ServerPlayerCommandHandler,
                CommandCompletionRegistry$Item});
    }

    @Override
    public ClassNode transform(ClassNode classNode) {
        if (ClientPlayerCommandHandler.equals(classNode.name) ||
                ServerPlayerCommandHandler.equals(classNode.name)) {
            MethodNode init = TransformerUtils.getMethod(classNode, "<init>");
            TransformerUtils.insertToEndOfCode(init,
                    new MethodInsnNode(INVOKESTATIC, CommandRegistry$Internal, "register", "()V", false));
            TransformerUtils.bringSelfCallToEndOfCode(init, "reloadCommandCompletions");
        } else if (PlayerCommandHandler.equals(classNode.name)) {
            MethodNode init = TransformerUtils.getMethod(classNode, "reloadCommands");
            TransformerUtils.insertToEndOfCode(init,
                    new MethodInsnNode(INVOKESTATIC, CommandRegistry$Internal, "register", "()V", false));
        } else if (CommandCompletionRegistry$Item.equals(classNode.name)) {
            patchCommandCompletionRegistry$Item(classNode);
        }
        return classNode;
    }

    private static void patchCommandCompletionRegistry$Item(ClassNode classNode) {
        MethodNode complete = TransformerUtils.getMethod(classNode, "complete");
        MethodNode newComplete = new MethodNode(ASM_API,
                complete.access, complete.name, complete.desc, complete.signature, null);
        LabelNode codeStart = new LabelNode();
        LabelNode codeEnd = new LabelNode();
        newComplete.instructions.add(codeStart);
        newComplete.instructions.add(new MethodInsnNode(INVOKESTATIC,
                GameRegistry, "getCommandCompletionItemIDs", "()Ljava/util/Set;", false));
        newComplete.instructions.add(new MethodInsnNode(INVOKEINTERFACE,
                "java/util/Set", "iterator", "()Ljava/util/Iterator;", true));
        newComplete.instructions.add(new VarInsnNode(ASTORE, 3));
        LabelNode iterateStart = new LabelNode();
        newComplete.instructions.add(iterateStart);
        newComplete.instructions.add(new VarInsnNode(ALOAD, 3));
        newComplete.instructions.add(new MethodInsnNode(INVOKEINTERFACE,
                "java/util/Iterator", "hasNext", "()Z", true));
        LabelNode finishedIterating = new LabelNode();
        newComplete.instructions.add(new JumpInsnNode(IFEQ, finishedIterating));
        newComplete.instructions.add(new VarInsnNode(ALOAD, 3));
        newComplete.instructions.add(new MethodInsnNode(INVOKEINTERFACE,
                "java/util/Iterator", "next", "()Ljava/lang/Object;", true));
        newComplete.instructions.add(new TypeInsnNode(CHECKCAST, "java/lang/String"));
        newComplete.instructions.add(new VarInsnNode(ASTORE, 4));
        LabelNode completionLocaleStart = new LabelNode();
        newComplete.instructions.add(completionLocaleStart);
        newComplete.instructions.add(new VarInsnNode(ALOAD, 4));
        newComplete.instructions.add(new VarInsnNode(ALOAD, 2));
        newComplete.instructions.add(new MethodInsnNode(INVOKEVIRTUAL,
                "java/lang/String", "startsWith", "(Ljava/lang/String;)Z", false));
        LabelNode notStartingWith = new LabelNode();
        newComplete.instructions.add(new JumpInsnNode(IFEQ, notStartingWith));
        newComplete.instructions.add(new VarInsnNode(ALOAD, 1));
        newComplete.instructions.add(new VarInsnNode(ALOAD, 4));
        newComplete.instructions.add(new MethodInsnNode(INVOKEVIRTUAL,
                "java/util/ArrayList", "add", "(Ljava/lang/Object;)Z", false));
        newComplete.instructions.add(new InsnNode(POP));
        newComplete.instructions.add(notStartingWith);
        newComplete.instructions.add(new JumpInsnNode(GOTO, iterateStart));
        newComplete.instructions.add(finishedIterating);
        newComplete.instructions.add(new InsnNode(ACONST_NULL));
        newComplete.instructions.add(new InsnNode(ARETURN));
        newComplete.instructions.add(codeEnd);
        newComplete.localVariables.add(new LocalVariableNode("completion",
                "Ljava/lang/String;", null, completionLocaleStart, notStartingWith, 4));
        newComplete.localVariables.add(new LocalVariableNode("this",
                "L" + CommandCompletionRegistry$Item + ";", null, codeStart, codeEnd, 0));
        newComplete.localVariables.add(new LocalVariableNode("completions",
                "Ljava/util/ArrayList;", "Ljava/util/ArrayList<Ljava/lang/String;>;", codeStart, codeEnd, 1));
        newComplete.localVariables.add(new LocalVariableNode("arg",
                "Ljava/lang/String;", null, codeStart, codeEnd, 2));
        classNode.methods.remove(complete);
        classNode.methods.add(newComplete);
    }
}
