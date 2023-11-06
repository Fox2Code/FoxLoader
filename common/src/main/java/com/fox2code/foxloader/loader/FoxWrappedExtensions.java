package com.fox2code.foxloader.loader;

import com.fox2code.foxloader.launcher.ClassTransformer;
import com.fox2code.foxloader.launcher.FoxClassLoader;
import com.fox2code.foxloader.loader.rebuild.ClassDataProvider;
import com.fox2code.foxloader.loader.transformer.JvmCompatTransformer;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.ClassRemapper;

import java.util.logging.Logger;

public final class FoxWrappedExtensions extends FoxClassLoader.WrappedExtensions {
    private final ClassDataProvider classDataProvider;
    private final Logger logger;

    FoxWrappedExtensions(ClassDataProvider classDataProvider, Logger logger) {
        this.classDataProvider = classDataProvider;
        this.logger = logger;
    }

    @Override
    public byte[] computeFrames(byte[] classData) {
        ClassReader classReader = new ClassReader(classData);
        ClassWriter classWriter = classDataProvider.newClassWriter();
        JvmCompatTransformer jvmCompatTransformer = PreLoader.getJvmCompatTransformer();
        classReader.accept(new ClassVisitor(ClassTransformer.ASM_BUILD,
                jvmCompatTransformer == null ? classWriter : // Fix Java11 code
                        new ClassRemapper(classWriter, jvmCompatTransformer)) {
            @Override
            public MethodVisitor visitMethod(int access, final String name, final String descriptor, String signature, String[] exceptions) {
                return new MethodVisitor(ClassTransformer.ASM_BUILD, super.visitMethod(access, name, descriptor, signature, exceptions)) {
                    @Override
                    public void visitMaxs(int maxStack, int maxLocals) {
                        try {
                            super.visitMaxs(maxStack, maxLocals);
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to compute frames for " + name + descriptor, e);
                        }
                    }
                };
            }
        }, 0);
        return classWriter.toByteArray();
    }

    @Override
    public byte[] patchMixinConfig(byte[] classData) {
        ClassReader classReader = new ClassReader(classData);
        ClassWriter classWriter = new ClassWriter(classReader, 0);
        classReader.accept(new ClassVisitor(ClassTransformer.ASM_BUILD, classWriter) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                MethodVisitor methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (name.equals("select")) {
                    methodVisitor = new MethodVisitor(ClassTransformer.ASM_BUILD, methodVisitor) {
                        @Override
                        public void visitInsn(int opcode) {
                            // Without this patch all mixins are ignored (fix mixin loading)
                            super.visitInsn(opcode == Opcodes.ICONST_0 ? Opcodes.ICONST_1 : opcode);
                        }
                    };
                }
                return methodVisitor;
            }
        }, 0);
        logger.info("Patched MixinConfig!");
        return classWriter.toByteArray();
    }

    @Override
    public byte[] patchMixinPreProcessorStandard(byte[] classData) {
        ClassReader classReader = new ClassReader(classData);
        ClassWriter classWriter = new ClassWriter(classReader, 0);
        classReader.accept(new ClassVisitor(ClassTransformer.ASM_BUILD, classWriter) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                MethodVisitor methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
                // These methods check for name remapping, but we don't do that (optimize mixin loading)
                if (name.equals("transformField") || name.equals("transformMethod")) {
                    methodVisitor.visitCode();
                    methodVisitor.visitInsn(Opcodes.RETURN);
                    methodVisitor.visitMaxs(0, 2);
                    methodVisitor.visitEnd();
                    return new MethodVisitor(ClassTransformer.ASM_BUILD) {};
                }
                return methodVisitor;
            }
        }, 0);
        logger.info("Patched MixinPreProcessorStandard!");
        return classWriter.toByteArray();
    }
}
