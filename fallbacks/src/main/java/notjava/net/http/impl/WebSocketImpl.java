package notjava.net.http.impl;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import notjava.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class WebSocketImpl implements WebSocket {
    private static final int OPCODE_CONTINUATION = 0x0;
    private static final int OPCODE_TEXT = 0x1;
    private static final int OPCODE_BINARY = 0x2;
    private static final int OPCODE_CLOSE = 0x8;
    private static final int OPCODE_PING = 0x9;
    private static final int OPCODE_PONG = 0xA;
    private final ReceiveThread receiveThread = new ReceiveThread();
    private volatile boolean running;
    private final HttpURLConnection httpURLConnection;
    private final URI uri;
    private final String subprotocol;
    private final Listener listener;
    private boolean closed;
    public DataOutputStream out;
    public boolean previousLast = true;

    public WebSocketImpl(HttpURLConnection httpURLConnection, URI uri, String subprotocol, Listener listener)
            throws IOException {
        this.httpURLConnection = httpURLConnection;
        this.uri = uri;
        this.subprotocol = subprotocol;
        this.listener = listener;
        this.receiveThread.start();
        this.out = new DataOutputStream(new BufferedOutputStream(httpURLConnection.getOutputStream()));
    }

    @Override
    public CompletableFuture<WebSocket> sendText(CharSequence data, boolean last) {
        Objects.requireNonNull(data);
        final boolean previousLast = this.previousLast;
        this.previousLast = last;

        return CompletableFuture.supplyAsync(() -> {
            try {
                sendRawMessage(previousLast ? OPCODE_CONTINUATION : OPCODE_TEXT,
                        data.toString().getBytes(StandardCharsets.UTF_8), last);
            } catch (IOException e) {
                listener.onError(WebSocketImpl.this, e);
                FallbackHttpUtils.sneakyThrow(e);
                throw new IOError(e);
            }
            return this;
        });
    }

    @Override
    public CompletableFuture<WebSocket> sendBinary(ByteBuffer data, boolean last) {
        Objects.requireNonNull(data);
        final boolean previousLast = this.previousLast;
        this.previousLast = last;

        return CompletableFuture.supplyAsync(() -> {
            try {
                sendRawMessage(previousLast ? OPCODE_CONTINUATION : OPCODE_BINARY, data.array(), last);
            } catch (IOException e) {
                listener.onError(WebSocketImpl.this, e);
                FallbackHttpUtils.sneakyThrow(e);
                throw new IOError(e);
            }
            return this;
        });
    }

    @Override
    public CompletableFuture<WebSocket> sendPing(ByteBuffer message) {
        Objects.requireNonNull(message);

        return CompletableFuture.supplyAsync(() -> {
            try {
                sendRawMessage(OPCODE_PING, message.array(), false);
            } catch (IOException e) {
                listener.onError(WebSocketImpl.this, e);
                FallbackHttpUtils.sneakyThrow(e);
                throw new IOError(e);
            }
            return this;
        });
    }

    @Override
    public CompletableFuture<WebSocket> sendPong(ByteBuffer message) {
        Objects.requireNonNull(message);

        return CompletableFuture.supplyAsync(() -> {
            try {
                sendRawMessage(OPCODE_PONG, message.array(), false);
            } catch (IOException e) {
                listener.onError(WebSocketImpl.this, e);
                FallbackHttpUtils.sneakyThrow(e);
                throw new IOError(e);
            }
            return this;
        });
    }

    @Override
    public CompletableFuture<WebSocket> sendClose(int statusCode, String reason) {
        Objects.requireNonNull(reason);

        return CompletableFuture.supplyAsync(() -> {
            try {
                sendRawMessageImpl(OPCODE_CLOSE, reason.getBytes(StandardCharsets.UTF_8), statusCode, false);
            } catch (IOException e) {
                listener.onError(WebSocketImpl.this, e);
                FallbackHttpUtils.sneakyThrow(e);
                throw new IOError(e);
            }
            return this;
        });
    }

    private void sendRawMessage(int opcode, byte[] data, boolean last) throws IOException {
        sendRawMessageImpl(opcode, data, 0, last);
    }

    private synchronized void sendRawMessageImpl(int opcode, byte[] data, int code, boolean last) throws IOException {
        char firstChar = (char) (opcode << 8);
        if (last) {
            firstChar |= 0x8000;
        }
        int len = data.length;
        if (opcode == OPCODE_CLOSE) {
            len += 4;
        }
        if (len < 126) {
            firstChar |= len;
        } else if (len < 0x10000) {
            firstChar |= 126;
        } else {
            firstChar |= 127;
        }
        out.writeChar(firstChar);
        if (len >= 126) {
            if (len < 65536) {
                out.writeChar((char) len);
            } else {
                out.writeLong(len);
            }
        }
        if (opcode == OPCODE_CLOSE) {
            out.writeChar((char) code);
        }
        out.write(data);
        out.flush();
    }

    @Override
    public void request(long n) {}

    @Override
    public String getSubprotocol() {
        return this.subprotocol;
    }

    @Override
    public boolean isOutputClosed() {
        return this.closed || !this.httpURLConnection.getDoOutput();
    }

    @Override
    public boolean isInputClosed() {
        return this.closed || !this.httpURLConnection.getDoInput();
    }

    @Override
    public void abort() {
        if (!this.closed) {
            this.closed = true;
            this.receiveThread.interrupt();
            try {
                this.httpURLConnection.getOutputStream().close();
            } catch (IOException ignored) {}
        }
    }

    private class ReceiveThread extends Thread {
        boolean lastText = true;
        byte[] receiveBuffer = FallbackHttpUtils.NULL_BUFFER;

        @Override
        public void run() {
            running = true;
            try {
                loop();
            } catch (IOException e) {
                listener.onError(WebSocketImpl.this, e);
                e.printStackTrace();
            } finally {
                running = false;
            }
        }

        public void loop() throws IOException {
            DataInputStream dataInputStream = new DataInputStream(
                    httpURLConnection.getInputStream());
            while (!closed && !this.isInterrupted()) {
                char frame = dataInputStream.readChar();
                int opcode = (frame >> 8) & 0xFF;
                int len = frame & 0x7f;
                if (len == 126) {
                    len = dataInputStream.readChar();
                } else if (len == 127) {
                    len = (int) dataInputStream.readLong();
                }
                if ((frame & 0x80) != 0) {
                    dataInputStream.skipBytes(4);
                }
                if (len > Short.MAX_VALUE) {
                    dataInputStream.skipBytes(len);
                    continue;
                }
                if (opcode == OPCODE_CONTINUATION) {
                    opcode = lastText ? OPCODE_TEXT : OPCODE_BINARY;
                }
                switch (opcode) {
                    default:
                        dataInputStream.skipBytes(len);
                        break;
                    case OPCODE_TEXT:
                        lastText = true;
                        if (receiveBuffer.length < len) {
                            receiveBuffer = new byte[Math.max(len, 2048)];
                        }
                        if (dataInputStream.read(receiveBuffer, 0, len) != len) {
                            throw new EOFException();
                        }
                        listener.onText(WebSocketImpl.this, new String(
                                receiveBuffer, 0, len, StandardCharsets.UTF_8),
                                (frame & 0x8000) != 0);
                        break;
                    case OPCODE_BINARY:
                        lastText = false;
                        if (receiveBuffer.length < len) {
                            receiveBuffer = new byte[Math.max(len, 2048)];
                        }
                        if (dataInputStream.read(receiveBuffer, 0, len) != len) {
                            throw new EOFException();
                        }
                        listener.onBinary(WebSocketImpl.this,
                                ByteBuffer.wrap(receiveBuffer, 0, len),
                                (frame & 0x8000) != 0);
                    case OPCODE_CLOSE:
                        listener.onClose(WebSocketImpl.this, 0, "");
                        WebSocketImpl.this.abort();
                }
            }
        }
    }
}
