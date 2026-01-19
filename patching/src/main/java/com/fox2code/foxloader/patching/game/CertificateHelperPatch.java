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

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

final class CertificateHelperPatch extends GamePatch {
    private static final String CertificateHelperRD = "net/minecraft/common/networking/CertificateHelper";
    private static final String CertificateHelperRD$ = "net/minecraft/common/networking/CertificateHelper$";
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
