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
package com.fox2code.foxloader.launcher;

import com.fox2code.foxloader.utils.Platform;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;

public class ClientAndServerMain {
    static {
        System.setProperty("foxloader.logger-distinguish-side", "true");
    }

    public static void main(String[] args) throws Throwable {
        ArrayList<String> command = new ArrayList<>();
        if (Platform.getPlatform() == Platform.WINDOWS) {
            command.add(new File(System.getProperty("java.home") + "\\bin\\javaw.exe").getAbsolutePath());
        } else {
            command.add(new File(System.getProperty("java.home") + "/bin/java").getAbsolutePath());
        }
        command.addAll(ManagementFactory.getRuntimeMXBean().getInputArguments());
        command.add("-Dfoxloader.logger-distinguish-side=true");
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add("com.fox2code.foxloader.launcher.ServerMain");
        Process serverProcess = new ProcessBuilder(command).inheritIO().start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (serverProcess.isAlive()) {
                serverProcess.destroy();
            }
        }, "Server subprocess killer"));
        ClientMain.main(args);
        System.exit(serverProcess.waitFor());
    }
}
