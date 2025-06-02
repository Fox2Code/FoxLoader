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
package com.fox2code.foxloader.spark;

import com.fox2code.foxloader.launcher.BuildConfig;
import com.fox2code.foxloader.loader.ModLoaderInit;
import me.lucko.spark.common.platform.PlatformInfo;

final class FoxLoaderSparkPlatformInfo implements PlatformInfo {
    private final Type type;

    public FoxLoaderSparkPlatformInfo(Type type) {
        this.type = type;
    }

    @Override
    public Type getType() {
        return this.type;
    }

    @Override
    public String getName() {
        return "FoxLoader";
    }

    @Override
    public String getBrand() {
        return ModLoaderInit.getModContainer("foxloader").getModName();
    }

    @Override
    public String getVersion() {
        return BuildConfig.FOXLOADER_VERSION;
    }

    @Override
    public String getMinecraftVersion() {
        return "ReIndev " + BuildConfig.REINDEV_VERSION;
    }
}
