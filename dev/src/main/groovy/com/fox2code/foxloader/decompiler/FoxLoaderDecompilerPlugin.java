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
package com.fox2code.foxloader.decompiler;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.java.decompiler.api.java.JavaPassRegistrar;
import org.jetbrains.java.decompiler.api.plugin.Plugin;
import org.jetbrains.java.decompiler.main.extern.IVariableNamingFactory;
import org.jetbrains.java.decompiler.main.plugins.PluginContext;

public class FoxLoaderDecompilerPlugin implements Plugin {
    @Override
    public String id() {
        return "FoxLoader";
    }

    @Override
    public String description() {
        return "FoxLoader Decompiler plugin";
    }

    @Override
    public void registerJavaPasses(JavaPassRegistrar registrar) {
        if (PluginContext.getCurrentContext().getVariableRenamer() != FoxVariableNamingFactory.INSTANCE) {
            throw new RuntimeException("FoxLoader Variable renaming factory was not selected!");
        }
    }

    @Override
    public @Nullable IVariableNamingFactory getRenamingFactory() {
        return FoxVariableNamingFactory.INSTANCE;
    }
}
