package com.fox2code.foxloader.patching;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.cli.Flags;
import xyz.wagyourtail.jvmdg.util.Utils;

final class FoxClassDowngrader extends ClassDowngrader {
    static final FoxClassDowngrader INSTANCE;

    private FoxClassDowngrader(@NotNull Flags flags) {
        super(flags);
    }

    @Override
    public byte[] classNodeToBytes(@NotNull ClassNode node) {
        ClassWriter cw = PreLoader.getClassDataProvider().newClassWriter();
        node.accept(cw);
        return cw.toByteArray();
    }

    static {
        Flags flags = new Flags();
        flags.classVersion = Utils.getCurrentClassVersion();
        INSTANCE = new FoxClassDowngrader(flags);
    }
}
