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
package com.fox2code.foxloader.patching.mixin;

import com.fox2code.flexver.FlexVer;
import com.fox2code.foxloader.launcher.FoxLauncher;
import com.fox2code.foxloader.loader.ModContainer;
import com.fox2code.foxloader.loader.ModLoaderInit;
import com.moulberry.mixinconstraints.util.Abstractions;

public class MixinConstraintsImpl extends Abstractions {
    public MixinConstraintsImpl() {}

    @Override
    protected boolean isDevEnvironment() {
        return FoxLauncher.DEV_MODE || FoxLauncher.DEVELOPING_FOXLOADER;
    }

    @Override
    protected String getModVersion(String modId) {
        ModContainer modContainer = ModLoaderInit.getModContainer(modId);
        return modContainer == null ? null : modContainer.getModInfo().version;
    }

    @Override
    protected boolean isVersionInRange(String version, String min, String max) {
        FlexVer versionFlex = FlexVer.parse(version);
        FlexVer minFlex = FlexVer.parse(version);
        FlexVer maxFlex = FlexVer.parse(version);
        return versionFlex.isGreaterOrEqual(minFlex) &&
                maxFlex.isGreaterOrEqual(versionFlex);
    }

    @Override
    protected String getPlatformName() {
        return "FoxLoader";
    }
}
