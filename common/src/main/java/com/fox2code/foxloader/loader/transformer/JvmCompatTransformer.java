package com.fox2code.foxloader.loader.transformer;

import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Allow to run up to Java11 code on a Java8 runtime
 */
public final class JvmCompatTransformer extends Remapper implements PreClassTransformer {
    private static final HashSet<String> java9Cls = new HashSet<>(Arrays.asList(
            "java/lang/invoke/StringConcatFactory", "java/util/concurrent/Flow"
    ));
    private static final int maxClassVersionSupport = V11;
    public final int classVersionSupport;

    public JvmCompatTransformer(int jvmVersion) {
        int classVersionSupportTmp;
        switch (jvmVersion) {
            default:
                throw new IllegalStateException("JvmCompatTransformer should not be used on java" + jvmVersion);
            case 8:
                classVersionSupportTmp = V1_8;
                break;
            case 9:
                classVersionSupportTmp = V9;
                break;
            case 10:
                classVersionSupportTmp = V10;
        }
        this.classVersionSupport = classVersionSupportTmp;
    }

    @Override
    public void transform(ClassNode classNode, String className) {
        if (classNode.version <= this.classVersionSupport ||
                classNode.version > maxClassVersionSupport) {
            return;
        }
        classNode.version = this.classVersionSupport;
        if (this.classVersionSupport < V9) {
            classNode.module = null;
        }
    }

    @Override
    public String map(String internalName) {
        if (this.classVersionSupport > V9 && "notjava/util/concurrent/Flow".equals(internalName))
            return "java/util/concurrent/Flow"; // <- Used in some java11 fallbacks
        return (this.classVersionSupport < V9 && java9Cls.contains(internalName)) ||
                internalName.startsWith("java/net/http/") ? "not" + internalName : internalName;
    }
}
