package com.fox2code.foxloader.patching.game;

import com.fox2code.foxloader.patching.TransformerUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

/**
 * GamePatch that are registered at {@link GamePatches}
 */
abstract class GamePatch implements Opcodes {
    static final Void ALL_CLASSES = null;
    public static final String LAMBDA_ARGS = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;";
    public static final String String = "java/lang/String";
    public static final String StringBuilder = "java/lang/StringBuilder";
    public static final String Collection = "java/util/Collection";
    public static final String Set = "java/util/Set";
    public static final int ASM_API = TransformerUtils.ASM_BUILD;
    final String[] targets;

    protected GamePatch(Void ignored) {
        this.targets = null;
    }

    protected GamePatch(String target) {
        this.targets = target == null ? null : new String[]{target};
    }

    protected GamePatch(String[] targets) {
        this.targets = targets == null || targets.length == 0 ? null : targets;
    }

    private void checkAccess() {

    }

    public abstract ClassNode transform(ClassNode classNode);
}
