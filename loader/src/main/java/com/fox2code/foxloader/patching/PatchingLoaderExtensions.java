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
package com.fox2code.foxloader.patching;

import com.fox2code.foxloader.launcher.BuildConfig;
import com.fox2code.foxloader.launcher.FileInfo;
import com.fox2code.foxloader.launcher.FoxClassLoader;
import com.fox2code.foxloader.launcher.FoxLauncher;
import com.fox2code.foxloader.loader.ModLoaderInit;
import com.fox2code.foxloader.utils.Platform;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import xyz.wagyourtail.jvmdg.ClassDowngrader;

import java.io.IOException;
import java.lang.instrument.IllegalClassFormatException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

final class PatchingLoaderExtensions extends FoxClassLoader.WrappedExtensions {
    private static final boolean SKIP_DOWNGRADE_CLASS = // Skip class downgrade when unsupported
            Platform.getJvmVersion() >= BuildConfig.JVM_DOWNGRADER_JAVA_SUPPORT_MAX;
    private final FoxClassLoader foxClassLoader;
    private final ClassDowngrader classDowngrader;
    private final MixinEnvironment defalutMixinEnvironment;
    private final IMixinTransformer mixinTransformer;
    private final ArrayList<ClassTransformer> preMixinTransformers;
    private final ArrayList<ClassTransformer> transformers;

    PatchingLoaderExtensions(IMixinTransformer mixinTransformer) {
        this.foxClassLoader = Objects.requireNonNull(FoxLauncher.getFoxClassLoader());
        this.defalutMixinEnvironment = MixinEnvironment.getEnvironment(MixinEnvironment.Phase.DEFAULT);
        this.mixinTransformer = mixinTransformer;
        this.classDowngrader = FoxClassDowngrader.INSTANCE;
        this.preMixinTransformers = new ArrayList<>();
        this.transformers = new ArrayList<>();
    }

    @Override
    public Map<String, byte[]> downgradeClass(final String className,final byte[] classData) {
        if (SKIP_DOWNGRADE_CLASS || classData == null) return null;
        try {
            return this.classDowngrader.downgrade(new AtomicReference<>(className), classData, true, s -> {
                String queriedClassName = s.replace('.', '/');
                if (queriedClassName.equals(className)) {
                    return classData;
                }
                try {
                    return this.foxClassLoader.accessGetRawClassBytes(queriedClassName);
                } catch (IOException e) {
                    return null;
                }
            });
        } catch (IllegalClassFormatException e) {
            return null;
        }
    }

    @Override
    public byte[] transformClass(FileInfo fileInfo, String className, byte[] classData) {
        ClassNode classNode;
        if (classData == null) {
            classData = this.mixinTransformer.generateClass(this.defalutMixinEnvironment, className);
            if (classData == null) return null;
            new ClassReader(classData).accept(classNode = new ClassNode(), 0);
        } else {
            new ClassReader(classData).accept(classNode = new ClassNode(), 0);
            for (ClassTransformer classTransformer : this.preMixinTransformers) {
                classNode = classTransformer.transform(fileInfo, classNode, className);
            }
            this.mixinTransformer.transformClass(this.defalutMixinEnvironment, className, classNode);
        }
        // Mods would usually use Mixins to transform their API
        for (ClassTransformer classTransformer : this.transformers) {
            classNode = classTransformer.transform(fileInfo, classNode, className);
        }
        if (classNode == null) return null;
        ClassWriter classWriter;
        if (FoxClassLoader.isGameClassName(className) ||
                (classNode.access & ClassTransformer.ACC_COMPUTE_FRAMES) != 0) {
            classNode.access &= ~ClassTransformer.ACC_COMPUTE_FRAMES;
            classWriter = PreLoader.getClassDataProvider().newClassWriter();
        } else {
            // Always check for maxLocals being at least argument size to fix mixin edge case
            for (MethodNode methodNode : classNode.methods) {
                if ((methodNode.access & Opcodes.ACC_ABSTRACT) != 0) continue;
                int maxLocalsMin = (methodNode.access & Opcodes.ACC_STATIC) == 0 ? 1 : 0;
                for (Type type : Type.getArgumentTypes(methodNode.desc)) {
                    maxLocalsMin += type.getSize();
                }
                methodNode.maxLocals = Math.max(methodNode.maxLocals, maxLocalsMin);
            }
            classWriter = new ClassWriter(0);
        }
        classNode.accept(classWriter);
        return classWriter.toByteArray();
    }

    @Override
    public void info(String message) {
        ModLoaderInit.getModLoaderLogger().info(message);
    }

    public ClassNode transformClassForMixins(FileInfo fileInfo, String className, ClassNode classNode) {
        for (ClassTransformer classTransformer : this.preMixinTransformers) {
            classNode = classTransformer.transform(fileInfo, classNode, className);
        }

        return classNode;
    }

    static byte[] patchMixinInfo(byte[] bytes) {
        ClassNode classNode = new ClassNode();
        ClassReader classReader = new ClassReader(bytes);
        classReader.accept(classNode, 0);
        MethodNode shouldApplyMixin = TransformerUtils.getMethod(classNode, "shouldApplyMixin");
        InsnList insnList = new InsnList();
        insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
        insnList.add(new FieldInsnNode(Opcodes.GETFIELD,
                "org/spongepowered/asm/mixin/transformer/MixinInfo",
                "className", "Ljava/lang/String;"));
        insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                "com/moulberry/mixinconstraints/MixinConstraints",
                "shouldApplyMixin", "(Ljava/lang/String;)Z"));
        LabelNode applyMixinOk = new LabelNode();
        insnList.add(new JumpInsnNode(Opcodes.IFNE, applyMixinOk));
        insnList.add(new InsnNode(Opcodes.ICONST_0));
        insnList.add(new InsnNode(Opcodes.IRETURN));
        insnList.add(applyMixinOk);
        TransformerUtils.insertToBeginningOfCode(shouldApplyMixin, insnList);
        ClassWriter classWriter = PreLoader.getClassDataProvider().newClassWriter();
        classNode.accept(classWriter);
        return classWriter.toByteArray();
    }

    void addClassTransformer(ClassTransformer classTransformer) {
        ArrayList<ClassTransformer> target =
                classTransformer.preMixinTransformer ?
                this.preMixinTransformers : this.transformers;
        int i = Collections.binarySearch(target, classTransformer);
        target.add(i < 0 ? -i - 1 : i, classTransformer);
    }

    byte[] downgradeClassBytes(byte[] rawClassBytes, String className) {
        String internalName = className.replace('.','/');
        Map<String, byte[]> downgraded = this.downgradeClass(internalName, rawClassBytes);
        if (downgraded != null && downgraded.size() == 1) {
            return downgraded.getOrDefault(internalName, rawClassBytes);
        }
        return rawClassBytes;
    }
}
