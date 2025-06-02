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
package com.fox2code.foxloader.updater;

import com.fox2code.flexver.FlexVerPredicate;
import com.fox2code.foxloader.launcher.BuildConfig;
import com.fox2code.foxloader.loader.ModContainer;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public abstract class AbstractUpdater {
    public final ModContainer modContainer;
    String latestVersion;
    boolean updateConsumed;

    protected AbstractUpdater(ModContainer modContainer) {
        this.modContainer = modContainer;
    }

    @Nullable protected abstract String findLatestVersion() throws IOException;

    protected abstract void doUpdate() throws IOException;

    public final String getLatestVersion() {
        return this.latestVersion;
    }

    public boolean hasUpdate() {
        return this.latestVersion != null &&
                !this.modContainer.getModInfo().flexver
                        .isGreaterOrEqual(this.latestVersion);
    }

    public boolean canUpdate() {
        return this.hasUpdate();
    }

    public static boolean reIndevVersionPatternMismatch(String accept) {
        return accept == null || !(accept.equals(BuildConfig.REINDEV_VERSION) ||
                FlexVerPredicate.parse(accept).match(BuildConfig.REINDEV_VERSION));
    }
}
