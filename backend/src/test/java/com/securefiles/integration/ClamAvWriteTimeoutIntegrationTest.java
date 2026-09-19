package com.securefiles.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.securefiles.domain.file.model.AntivirusScanResult;
import com.securefiles.domain.file.model.AntivirusVerdict;
import com.securefiles.infrastructure.clamav.ClamAvAntivirusScanner;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(
        named = "SECUREFILES_CLAMAV_WRITE_TIMEOUT_INTEGRATION",
        matches = "true")
class ClamAvWriteTimeoutIntegrationTest {

    private static final long CONTENT_SIZE_BYTES = 64L * 1024L * 1024L;
    private static final int CONNECT_TIMEOUT_MILLIS = 1_000;
    private static final int WRITE_TIMEOUT_MILLIS = 100;
    private static final int SCAN_TIMEOUT_MILLIS = 100;
    private static final int CHUNK_SIZE_BYTES = 16 * 1024;

    @Test
    void scan_shouldFailWithinWriteTimeout_whenServerStopsReading() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0);
                ExecutorService serverExecutor = Executors.newSingleThreadExecutor()) {
            CountDownLatch clientConnected = new CountDownLatch(1);
            CountDownLatch releaseServer = new CountDownLatch(1);
            serverExecutor.submit(() -> holdConnection(serverSocket, clientConnected, releaseServer));

            ClamAvAntivirusScanner scanner = new ClamAvAntivirusScanner(
                    "127.0.0.1",
                    serverSocket.getLocalPort(),
                    CONNECT_TIMEOUT_MILLIS,
                    WRITE_TIMEOUT_MILLIS,
                    WRITE_TIMEOUT_MILLIS * 10,
                    CHUNK_SIZE_BYTES);

            long startedAt = System.nanoTime();
            try {
                AntivirusScanResult result = scanner.scan(repeatingInputStream(CONTENT_SIZE_BYTES));
                long elapsedNanos = System.nanoTime() - startedAt;

                assertThat(clientConnected.await(1, TimeUnit.SECONDS)).isTrue();
                assertThat(result.verdict()).isEqualTo(AntivirusVerdict.RETRYABLE_FAILURE);
                assertThat(result.failureCode()).isEqualTo("CLAMAV_WRITE_TIMEOUT");
                assertThat(elapsedNanos)
                        .isLessThan(Duration.ofMillis(WRITE_TIMEOUT_MILLIS * 10L).toNanos());
            } finally {
                releaseServer.countDown();
            }
        }
    }

    @Test
    void scan_shouldFailWithinGlobalTimeout_whenContentReadStalls() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0);
                ExecutorService serverExecutor = Executors.newSingleThreadExecutor();
                PipedOutputStream contentWriter = new PipedOutputStream();
                PipedInputStream content = new PipedInputStream(contentWriter)) {
            CountDownLatch clientConnected = new CountDownLatch(1);
            CountDownLatch releaseServer = new CountDownLatch(1);
            serverExecutor.submit(() -> holdConnection(serverSocket, clientConnected, releaseServer));
            contentWriter.write(0);
            contentWriter.flush();

            ClamAvAntivirusScanner scanner = new ClamAvAntivirusScanner(
                    "127.0.0.1",
                    serverSocket.getLocalPort(),
                    CONNECT_TIMEOUT_MILLIS,
                    WRITE_TIMEOUT_MILLIS,
                    WRITE_TIMEOUT_MILLIS,
                    SCAN_TIMEOUT_MILLIS,
                    CHUNK_SIZE_BYTES);

            long startedAt = System.nanoTime();
            try {
                AntivirusScanResult result = scanner.scan(content);
                long elapsedNanos = System.nanoTime() - startedAt;

                assertThat(clientConnected.await(1, TimeUnit.SECONDS)).isTrue();
                assertThat(result.verdict()).isEqualTo(AntivirusVerdict.RETRYABLE_FAILURE);
                assertThat(result.failureCode()).isEqualTo("CLAMAV_SCAN_TIMEOUT");
                assertThat(elapsedNanos)
                        .isLessThan(Duration.ofMillis(SCAN_TIMEOUT_MILLIS * 10L).toNanos());
            } finally {
                releaseServer.countDown();
            }
        }
    }

    private void holdConnection(
            ServerSocket serverSocket,
            CountDownLatch clientConnected,
            CountDownLatch releaseServer) {
        try (Socket clientSocket = serverSocket.accept()) {
            clientSocket.setReceiveBufferSize(1_024);
            clientConnected.countDown();
            releaseServer.await(2, TimeUnit.SECONDS);
        } catch (IOException exception) {
            if (!serverSocket.isClosed()) {
                throw new IllegalStateException("The test server could not accept the connection", exception);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private InputStream repeatingInputStream(long sizeBytes) {
        return new InputStream() {
            private long remainingBytes = sizeBytes;

            @Override
            public int read(byte[] buffer, int offset, int length) {
                if (remainingBytes == 0) {
                    return -1;
                }
                int bytesToRead = (int) Math.min(remainingBytes, length);
                Arrays.fill(buffer, offset, offset + bytesToRead, (byte) 0);
                remainingBytes -= bytesToRead;
                return bytesToRead;
            }

            @Override
            public int read() {
                if (remainingBytes == 0) {
                    return -1;
                }
                remainingBytes--;
                return 0;
            }
        };
    }
}