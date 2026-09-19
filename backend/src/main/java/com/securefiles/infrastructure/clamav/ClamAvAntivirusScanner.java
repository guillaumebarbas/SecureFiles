package com.securefiles.infrastructure.clamav;

import com.securefiles.domain.file.model.AntivirusScanResult;
import com.securefiles.domain.file.model.FileFailureCodes;
import com.securefiles.domain.file.port.out.AntivirusScanner;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

public final class ClamAvAntivirusScanner implements AntivirusScanner {

    private static final byte[] INSTREAM_COMMAND = "zINSTREAM\0".getBytes(StandardCharsets.US_ASCII);

    private final String host;
    private final int port;
    private final int connectTimeoutMillis;
    private final int writeTimeoutMillis;
    private final int readTimeoutMillis;
    private final int scanTimeoutMillis;
    private final int chunkSize;

    public ClamAvAntivirusScanner(
            String host,
            int port,
            int connectTimeoutMillis,
            int readTimeoutMillis,
            int chunkSize) {
        this(
                host,
                port,
                connectTimeoutMillis,
                readTimeoutMillis,
                readTimeoutMillis,
                readTimeoutMillis,
                chunkSize);
    }

    public ClamAvAntivirusScanner(
            String host,
            int port,
            int connectTimeoutMillis,
            int writeTimeoutMillis,
            int readTimeoutMillis,
            int chunkSize) {
        this(
                host,
                port,
                connectTimeoutMillis,
                writeTimeoutMillis,
                readTimeoutMillis,
                readTimeoutMillis,
                chunkSize);
    }

    public ClamAvAntivirusScanner(
            String host,
            int port,
            int connectTimeoutMillis,
            int writeTimeoutMillis,
            int readTimeoutMillis,
            int scanTimeoutMillis,
            int chunkSize) {
        this.host = requireText(host, "host");
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
        if (connectTimeoutMillis < 1
                || writeTimeoutMillis < 1
                || readTimeoutMillis < 1
                || scanTimeoutMillis < 1
                || chunkSize < 1) {
            throw new IllegalArgumentException("timeouts and chunkSize must be positive");
        }
        this.port = port;
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.writeTimeoutMillis = writeTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
        this.scanTimeoutMillis = scanTimeoutMillis;
        this.chunkSize = chunkSize;
    }

    @Override
    public AntivirusScanResult scan(InputStream content) {
        Objects.requireNonNull(content, "content must not be null");
        AtomicReference<SocketChannel> socketReference = new AtomicReference<>();
        CompletableFuture<AntivirusScanResult> scanResult = new CompletableFuture<>();
        Thread scanThread = Thread.ofVirtual().start(() -> completeScan(content, socketReference, scanResult));
        try {
            return scanResult.get(scanTimeoutMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException exception) {
            cancelScan(content, socketReference, scanThread);
            return AntivirusScanResult.retryableFailure(FileFailureCodes.CLAMAV_SCAN_TIMEOUT);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            cancelScan(content, socketReference, scanThread);
            return AntivirusScanResult.retryableFailure(FileFailureCodes.ANTIVIRUS_UNAVAILABLE);
        } catch (ExecutionException exception) {
            return AntivirusScanResult.retryableFailure(FileFailureCodes.ANTIVIRUS_UNAVAILABLE);
        }
    }

    private void completeScan(
            InputStream content,
            AtomicReference<SocketChannel> socketReference,
            CompletableFuture<AntivirusScanResult> scanResult) {
        try {
            scanResult.complete(scanContent(content, socketReference));
        } catch (RuntimeException exception) {
            scanResult.completeExceptionally(exception);
        }
    }

    private AntivirusScanResult scanContent(
            InputStream content,
            AtomicReference<SocketChannel> socketReference) {
        try (SocketChannel socket = openSocket(socketReference)) {
            sendStream(socket, content);
            return readResult(socket);
        } catch (ClamAvWriteTimeoutException exception) {
            return AntivirusScanResult.retryableFailure(FileFailureCodes.CLAMAV_WRITE_TIMEOUT);
        } catch (IOException exception) {
            return AntivirusScanResult.retryableFailure(FileFailureCodes.CLAMAV_UNAVAILABLE);
        } finally {
            socketReference.set(null);
        }
    }

    private void cancelScan(
            InputStream content,
            AtomicReference<SocketChannel> socketReference,
            Thread scanThread) {
        closeContent(content);
        closeSocket(socketReference.get());
        scanThread.interrupt();
    }

    private void closeContent(InputStream content) {
        try {
            content.close();
        } catch (IOException ignored) {
        }
    }

    private void closeSocket(SocketChannel socket) {
        if (socket == null) {
            return;
        }
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    private SocketChannel openSocket(AtomicReference<SocketChannel> socketReference) throws IOException {
        SocketChannel socket = SocketChannel.open();
        socketReference.set(socket);
        try {
            socket.socket().connect(new InetSocketAddress(host, port), connectTimeoutMillis);
            socket.configureBlocking(false);
            return socket;
        } catch (IOException exception) {
            socket.close();
            throw exception;
        }
    }

    private void sendStream(SocketChannel socket, InputStream content) throws IOException {
        try (Selector selector = Selector.open()) {
            socket.register(selector, java.nio.channels.SelectionKey.OP_WRITE);
            writeFully(socket, selector, ByteBuffer.wrap(INSTREAM_COMMAND));
            writeContent(socket, selector, content);
            writeFully(socket, selector, endOfStreamBuffer());
        }
    }

    private void writeContent(SocketChannel socket, Selector selector, InputStream content)
            throws IOException {
        byte[] buffer = new byte[chunkSize];
        int read;
        while ((read = content.read(buffer)) != -1) {
            if (read == 0) {
                continue;
            }
            writeFully(socket, selector, lengthBuffer(read));
            writeFully(socket, selector, ByteBuffer.wrap(buffer, 0, read));
        }
    }

    private ByteBuffer lengthBuffer(int length) {
        return ByteBuffer.allocate(Integer.BYTES).putInt(length).flip();
    }

    private ByteBuffer endOfStreamBuffer() {
        return lengthBuffer(0);
    }

    private void writeFully(SocketChannel socket, Selector selector, ByteBuffer buffer)
            throws IOException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(writeTimeoutMillis);
        while (buffer.hasRemaining()) {
            if (socket.write(buffer) > 0) {
                continue;
            }
            long remainingNanos = deadline - System.nanoTime();
            if (remainingNanos <= 0) {
                throw new ClamAvWriteTimeoutException();
            }
            selector.select(Math.max(1, TimeUnit.NANOSECONDS.toMillis(remainingNanos)));
            selector.selectedKeys().clear();
        }
    }

    private AntivirusScanResult readResult(SocketChannel socket) throws IOException {
        socket.configureBlocking(true);
        socket.socket().setSoTimeout(readTimeoutMillis);
        String response = readResponse(socket.socket().getInputStream());
        if (response == null || response.isBlank()) {
            return AntivirusScanResult.retryableFailure(FileFailureCodes.CLAMAV_INVALID_RESPONSE);
        }
        String normalizedResponse = response.trim();
        if (normalizedResponse.equals("stream: OK")) {
            return AntivirusScanResult.clean();
        }
        if (normalizedResponse.startsWith("stream: ")
                && normalizedResponse.endsWith(" FOUND")
                && normalizedResponse.length() > "stream:  FOUND".length()) {
            return AntivirusScanResult.infected();
        }
        return AntivirusScanResult.retryableFailure(FileFailureCodes.CLAMAV_INVALID_RESPONSE);
    }

    private String readResponse(InputStream input) throws IOException {
        StringBuilder response = new StringBuilder();
        int value;
        while ((value = input.read()) != -1) {
            if (value == '\n' || value == 0) {
                break;
            }
            response.append((char) value);
        }
        return response.toString();
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
