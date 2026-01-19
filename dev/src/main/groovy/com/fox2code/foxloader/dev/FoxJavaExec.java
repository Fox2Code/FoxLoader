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
package com.fox2code.foxloader.dev;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.StringJoiner;

public final class FoxJavaExec {
    private static final boolean DEBUG = false;
    private final File javaExec;
    private final StringJoiner classpath;
    private String mainClass;

    public FoxJavaExec(File javaExec) {
        this.javaExec = javaExec;
        this.classpath = new StringJoiner(File.pathSeparator);
    }

    public void addFile(File file) {
        this.classpath.add(file.getAbsolutePath());
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public void exec(String libraryRoot, String input, String output) throws IOException, InterruptedException {
        String[] command = new String[]{
                this.javaExec.getAbsolutePath(),
                "-Xmx2048m", "-XX:-UseGCOverheadLimit", "-Dfile.encoding=UTF-8",
                "-cp", this.classpath.toString(), mainClass,
                libraryRoot, input, output,
        };
        if (DEBUG) {
            StringJoiner stringJoiner = new StringJoiner(" ", "FoxJavaExec: ", "");
            for (String arg : command) {
                stringJoiner.add(arg);
            }
            System.out.println(stringJoiner);
        }
        Process process = new ProcessBuilder(command).start();
        new ThreadCopyStream(process.getInputStream(), System.out).start();
        new ThreadCopyStream(process.getErrorStream(), System.out).start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Process exited with error code: " + exitCode);
        }
    }

    private static class FlushBlockingOutputStream extends FilterOutputStream {
        public FlushBlockingOutputStream(@NotNull OutputStream out) {
            super(out);
        }

        @Override
        public void flush() {}
    }

    private static class TrackingBufferedOutputStream extends BufferedOutputStream {
        public TrackingBufferedOutputStream(@NotNull OutputStream out) {
            super(out);
        }

        public boolean hasBuffer() {
            return this.count > 0;
        }
    }

    private static class ThreadCopyStream extends Thread {
        private final InputStream inputStream;
        private final PrintStream printStream;
        private final TrackingBufferedOutputStream tracker;

        private ThreadCopyStream(InputStream inputStream, PrintStream printStream) {
            this.inputStream = inputStream;
            this.printStream = new PrintStream(
                    this.tracker = new TrackingBufferedOutputStream(
                            new FlushBlockingOutputStream(printStream)), true);
            this.setDaemon(true);
            this.setPriority(Thread.MIN_PRIORITY);
            this.setName("Thread Copy Stream");
        }

        @Override
        public void run() {
            try {
                final byte[] buffer = new byte[1];
                while (inputStream.read(buffer) > 0) {
                    printStream.write(buffer);
                }
            } catch (IOException ignored) {} finally {
                if (tracker.hasBuffer()) {
                    printStream.println();
                }
                printStream.flush();
            }
        }
    }
}
