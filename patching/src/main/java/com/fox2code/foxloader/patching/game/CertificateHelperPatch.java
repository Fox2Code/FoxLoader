package com.fox2code.foxloader.patching.game;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

final class CertificateHelperPatch extends GamePatch {
    private static final String CertificateHelperRD = "net/minecraft/client/networking/CertificateHelper";
    private static final String CertificateHelperRD$ = "net/minecraft/client/networking/CertificateHelper$";
    private static final String CertificateHelperFL = "com/fox2code/foxloader/utils/io/CertificateHelper";

    CertificateHelperPatch() {
        super(ALL_CLASSES);
    }

    @Override
    public ClassNode transform(ClassNode classNode) {
        if (CertificateHelperRD.equals(classNode.name) ||
                classNode.name.startsWith(CertificateHelperRD$)) {
            return null;
        }
        for (MethodNode methodNode : classNode.methods) {
            if (methodNode.instructions == null) continue;
            for (AbstractInsnNode abstractInsnNode : methodNode.instructions) {
                if (abstractInsnNode instanceof MethodInsnNode) {
                    MethodInsnNode methodInsnNode = (MethodInsnNode) abstractInsnNode;
                    if (CertificateHelperRD.equals(methodInsnNode.owner)) {
                        methodInsnNode.owner = CertificateHelperFL;
                        if ("installCertificates".equals(methodInsnNode.name)) {
                            methodInsnNode.name = "initialize";
                        }
                    }
                }
            }
        }

        return classNode;
    }
}
