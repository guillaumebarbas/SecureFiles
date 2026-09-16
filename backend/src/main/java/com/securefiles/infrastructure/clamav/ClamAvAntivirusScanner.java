package com.securefiles.infrastructure.clamav;

import com.securefiles.domain.file.model.AntivirusScanResult;
import com.securefiles.domain.file.model.FileFailureCodes;
import com.securefiles.domain.file.port.out.AntivirusScanner;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class ClamAvAntivirusScanner implements AntivirusScanner {

    private static final byte[] INSTREAM_COMMAND = "zINSTREAM\0".getBytes(StandardCharsets.US_ASCII);

    private final String host;
    private final int port;
    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;
    private final int chunkSize;

    public ClamAvAntivirusScanner(
            String host,
            int port,
            int connectTimeoutMillis,
            int readTimeoutMillis,
            int chunkSize) {
        this.host = requireText(host, "host");
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
        if (connectTimeoutMillis < 1 || readTimeoutMillis < 1 || chunkSize < 1) {
            throw new IllegalArgumentException("timeouts and chunkSize must be positive");
        }
        this.port = port;
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
        this.chunkSize = chunkSize;
    }

    @Override
    public AntivirusScanResult scan(InputStream content) {
        Objects.requireNonNull(content, "content must not be null");
        try (Socket socket = openSocket()) {
            sendStream(socket, content);
            return readResult(socket);
        } catch (IOException exception) {
            return AntivirusScanResult.retryableFailure(FileFailureCodes.CLAMAV_UNAVAILABLE);
        }
    }

    private Socket openSocket() throws IOException {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), connectTimeoutMillis);
        socket.setSoTimeout(readTimeoutMillis);
        return socket;
    }

    private void sendStream(Socket socket, InputStream content) throws IOException {
        DataOutputStream output = new DataOutputStream(socket.getOutputStream());
        output.write(INSTREAM_COMMAND);
        byte[] buffer = new byte[chunkSize];
        int read;
        while ((read = content.read(buffer)) != -1) {
            output.writeInt(read);
            output.write(buffer, 0, read);
        }
        output.writeInt(0);
        output.flush();
    }

    private AntivirusScanResult readResult(Socket socket) throws IOException {
        String response = readResponse(socket.getInputStream());
        if (response == null || response.isBlank()) {
            return AntivirusScanResult.retryableFailure(FileFailureCodes.CLAMAV_INVALID_RESPONSE);
        }
        if (response.endsWith("OK")) {
            return AntivirusScanResult.clean();
        }
        if (response.contains("FOUND")) {
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
