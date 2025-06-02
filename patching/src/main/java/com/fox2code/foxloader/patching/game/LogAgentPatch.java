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
