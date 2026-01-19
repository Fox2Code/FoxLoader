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

    public static void patchComputeFrames(File input, File output) throws IOException {
        GamePatches.patchSlimJarComputeFrames(input, output);
    }
}
