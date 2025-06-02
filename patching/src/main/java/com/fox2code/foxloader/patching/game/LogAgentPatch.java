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

final class LogAgentPatch extends GamePatch {
    private static final String LogAgent = "net/minecraft/common/util/logging/LogAgent";
    private static final String InternalLoggingHooks = "com/fox2code/foxloader/internal/InternalLoggingHooks";


    LogAgentPatch() {
        super(LogAgent);
    }

    @Override
    public ClassNode transform(ClassNode classNode) {
        if (LogAgent.equals(classNode.name)) {
            TransformerUtils.makeFieldPublic(classNode, "logger");
            TransformerUtils.makeFieldPublic(classNode, "logFile");
            TransformerUtils.makeFieldPublic(classNode, "loggerName");
            MethodNode oldSetupLogger = TransformerUtils.getMethod(classNode, "setupLogger", "()V");
            classNode.methods.remove(oldSetupLogger);
            MethodNode newSetupLogger = new MethodNode(
                    oldSetupLogger.access, "setupLogger", "()V", null, null);
            newSetupLogger.instructions.add(new VarInsnNode(ALOAD, 0));
            newSetupLogger.instructions.add(new MethodInsnNode(
                    INVOKESTATIC, InternalLoggingHooks,
                    "setupLogAgentFoxLoader", "(L" + LogAgent + ";)V", false));
            newSetupLogger.instructions.add(new InsnNode(RETURN));
            classNode.methods.add(newSetupLogger);
        }
        return classNode;
    }
}
