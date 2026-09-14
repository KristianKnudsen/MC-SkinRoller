package dev.skinroller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

final class SkinUploader {

    private static final URI ENDPOINT = URI.create("https://api.minecraftservices.com/minecraft/profile/skins");
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    record Response(int status, String body, long retryAfterSeconds) {}

    private SkinUploader() {}

    static Response upload(byte[] png, String variant, String token) throws IOException, InterruptedException {
        String boundary = "skinroller" + Long.toHexString(ThreadLocalRandom.current().nextLong());
        HttpRequest request = HttpRequest.newBuilder(ENDPOINT)
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(multipart(boundary, variant, png)))
                .build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        long retryAfter = response.headers().firstValue("Retry-After").map(SkinUploader::seconds).orElse(-1L);
        return new Response(response.statusCode(), response.body(), retryAfter);
    }

    private static byte[] multipart(String boundary, String variant, byte[] png) {
        ByteArrayOutputStream body = new ByteArrayOutputStream(png.length + 512);
        ascii(body, "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"variant\"\r\n\r\n"
                + variant + "\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"skin.png\"\r\n"
                + "Content-Type: image/png\r\n\r\n");
        body.writeBytes(png);
        ascii(body, "\r\n--" + boundary + "--\r\n");
        return body.toByteArray();
    }

    private static void ascii(ByteArrayOutputStream out, String s) {
        out.writeBytes(s.getBytes(StandardCharsets.US_ASCII));
    }

    private static long seconds(String header) {
        try {
            return Long.parseLong(header.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
