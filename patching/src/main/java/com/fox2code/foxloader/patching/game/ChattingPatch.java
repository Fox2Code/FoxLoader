package com.fox2code.foxloader.patching.game;

import com.fox2code.foxloader.patching.TransformerUtils;
import org.objectweb.asm.tree.*;

import java.util.Objects;

final class ChattingPatch extends GamePatch {
    private static final String EntityPlayerSP = "net/minecraft/client/player/EntityPlayerSP";
    private static final String EntityPlayer = "net/minecraft/common/entity/player/EntityPlayer";
    private static final String EntityPlayerMP = "net/minecraft/server/entity/player/EntityPlayerMP";
    private static final String NetServerHandler = "net/minecraft/server/networking/NetServerHandler";
    private static final String InternalPlayerHooks = "com/fox2code/foxloader/internal/InternalPlayerHooks";

    ChattingPatch() {
        super(new String[]{EntityPlayerSP, NetServerHandler});
    }

    @Override
    public ClassNode transform(ClassNode classNode) {
        boolean client;
        if ((client = EntityPlayerSP.equals(classNode.name)) ||
                NetServerHandler.equals(classNode.name)) {
            MethodNode sendChatMessage = TransformerUtils.getMethod(
                    classNode, client ? "sendChatMessage" : "handleChat");
            AbstractInsnNode subStartInsn = null;
            if (client) {
                subStartInsn = TransformerUtils.nextCodeInsn(sendChatMessage.instructions.getFirst());
            } else {
                for (AbstractInsnNode abstractInsnNode : sendChatMessage.instructions) {
                    if (abstractInsnNode.getOpcode() == NEW &&
                            ((TypeInsnNode) abstractInsnNode).desc.equals("java/lang/StringBuilder")) {
                        subStartInsn = abstractInsnNode;
                        break;
                    }
                }
            }
            Objects.requireNonNull(subStartInsn, "subStartInsn");
            AbstractInsnNode subEndInsn = null;
            boolean check = false;
            for (AbstractInsnNode abstractInsnNode : sendChatMessage.instructions) {
                if (!check) {
                    check = abstractInsnNode == subStartInsn;
                } else if (client ? abstractInsnNode.getOpcode() == INVOKEVIRTUAL && // Client check
                        ((MethodInsnNode) abstractInsnNode).owner.equals(EntityPlayerSP) :
                        abstractInsnNode.getOpcode() == ASTORE && // Server check
                                ((VarInsnNode) abstractInsnNode).var == 2) {
                    subEndInsn = abstractInsnNode;
                    break;
                }
            }
            LabelNode endLabel = null;
            AbstractInsnNode itr4Label = TransformerUtils.previousCodeInsn(sendChatMessage.instructions.getLast());
            while ((itr4Label = itr4Label.getPrevious()) != null) {
                if (itr4Label instanceof LabelNode) {
                    endLabel = (LabelNode) itr4Label;
                    break;
                }
            }
            int messageVarIndex = client ? 1 : 2;
            InsnList injection = new InsnList();
            if (!client) {
                injection.add(new VarInsnNode(ALOAD, 0));
                injection.add(new FieldInsnNode(GETFIELD, NetServerHandler,
                        "playerEntity", "L" + EntityPlayerMP + ";"));
            }
            injection.add(new VarInsnNode(ALOAD, messageVarIndex));
            injection.add(new MethodInsnNode(INVOKESTATIC, InternalPlayerHooks,
                    "sendPlayerChatEvent", "(L" + EntityPlayer + ";Ljava/lang/String;)Ljava/lang/String;"));
            injection.add(new VarInsnNode(ASTORE, messageVarIndex));
            injection.add(new VarInsnNode(ALOAD, messageVarIndex));
            injection.add(new JumpInsnNode(IFNULL, endLabel));
            if (client) {
                injection.add(new VarInsnNode(ALOAD, 0));
                injection.add(new VarInsnNode(ALOAD, messageVarIndex));
            }
            AbstractInsnNode toRemove = subStartInsn;
            AbstractInsnNode injectionPoint;
            if (client) {
                injectionPoint = subStartInsn;
                AbstractInsnNode toRemoveNext = toRemove.getNext();
                while ((toRemove = toRemoveNext) != null) {
                    if (toRemove == subEndInsn) break;
                    toRemoveNext = toRemove.getNext();
                    sendChatMessage.instructions.remove(toRemove);
                }
            } else {
                injectionPoint = subStartInsn.getPrevious();
                AbstractInsnNode toRemoveNext = toRemove;
                while ((toRemove = toRemoveNext) != null) {
                    toRemoveNext = toRemove.getNext();
                    if (!(toRemove instanceof LabelNode)) {
                        sendChatMessage.instructions.remove(toRemove);
                    }
                    if (toRemove == subEndInsn) break;
                }
            }
            sendChatMessage.instructions.insert(injectionPoint, injection);
        }
        return classNode;
    }
}
