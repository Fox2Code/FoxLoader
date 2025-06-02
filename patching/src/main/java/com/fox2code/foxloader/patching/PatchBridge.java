package com.fox2code.foxloader.patching;

import com.fox2code.foxloader.patching.dev.DevelopmentSourcePatcher;
import com.fox2code.foxloader.patching.game.GamePatches;

import java.io.File;
import java.io.IOException;

// Implementation used by FoxLoaderInvoker
public final class PatchBridge {
    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.out.println("Usage: <in> <out> <unpick>");
            System.exit(1);
            return;
        }
        File input = new File(args[0]).getAbsoluteFile();
        File output = new File(args[1]).getAbsoluteFile();
        if ("dev".equals(args[2])) {
            GamePatches.patchSlimJarDev(input, output);
            return;
        }
        boolean unpick = Boolean.parseBoolean(args[2]);
        PatchBridge.patch(input, output, unpick);
    }

    public static void patch(File input, File output, boolean unpick) throws IOException {
        if (unpick) {
            DevelopmentSourcePatcher.unpickPatchedJar(input, output);
        } else {
            GamePatches.patchSlimJar(input, output);
        }
    }
}
