package bpr.photo.pneu.eprel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class EprelLabelDownloader {

    private static final String BASE_URL = "https://eprel.ec.europa.eu";
    private static final String USER_AGENT = "Mozilla/5.0";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EprelLabelDownloader() {
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

        this.httpClient = HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .connectTimeout(TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public String downloadLabel(String labelCode, Path outputFile) throws Exception {
        openProductPage(labelCode);

        String address = resolveLabelAddress(labelCode);
        URI downloadUri = toAbsoluteUri(address);

        HttpRequest downloadRequest = HttpRequest.newBuilder(downloadUri)
                .GET()
                .timeout(TIMEOUT)
                .header("User-Agent", USER_AGENT)
                .header("Referer", productPageUrl(labelCode))
                .build();

        HttpResponse<byte[]> response = httpClient.send(downloadRequest, HttpResponse.BodyHandlers.ofByteArray());
        ensureSuccess(response.statusCode(), "telechargement du fichier pour " + labelCode);

        Files.createDirectories(outputFile.getParent());

        String contentType = response.headers()
                .firstValue("Content-Type")
                .orElse("")
                .toLowerCase(Locale.ROOT);

        String path = downloadUri.getPath().toLowerCase(Locale.ROOT);
        byte[] body = response.body();

        if (contentType.contains("zip") || path.endsWith(".zip")) {
            extractFirstPng(body, outputFile);
            return downloadUri.toString();
        }

        if (!contentType.contains("png") && !path.endsWith(".png")) {
            throw new IllegalStateException("Le fichier telecharge n'est pas un PNG: " + downloadUri);
        }

        Files.write(outputFile, body, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return downloadUri.toString();
    }

    private void openProductPage(String labelCode) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(productPageUrl(labelCode)))
                .GET()
                .timeout(TIMEOUT)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .build();

        HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        ensureSuccess(response.statusCode(), "ouverture de la page produit " + labelCode);
    }

    private String resolveLabelAddress(String labelCode) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(labelApiUrl(labelCode)))
                .GET()
                .timeout(TIMEOUT)
                .header("User-Agent", USER_AGENT)
                .header("Referer", productPageUrl(labelCode))
                .header("Accept", "*/*")
                .header("X-Requested-With", "XMLHttpRequest")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        ensureSuccess(response.statusCode(), "recuperation du lien d'etiquette pour " + labelCode);

        JsonNode root = objectMapper.readTree(response.body());
        String address = root.path("address").asText("");
        if (address.isBlank()) {
            throw new IllegalStateException("Aucune adresse de telechargement retournee pour " + labelCode);
        }

        return address;
    }

    private URI toAbsoluteUri(String address) {
        if (address.startsWith("http://") || address.startsWith("https://")) {
            return URI.create(address);
        }
        return URI.create(BASE_URL + address);
    }

    private void extractFirstPng(byte[] archiveBytes, Path outputFile) throws IOException {
        try (InputStream inputStream = new ByteArrayInputStream(archiveBytes);
             ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {

            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().toLowerCase(Locale.ROOT).endsWith(".png")) {
                    Files.copy(zipInputStream, outputFile, StandardCopyOption.REPLACE_EXISTING);
                    return;
                }
            }
        }

        throw new IllegalStateException("Aucun fichier PNG trouve dans l'archive telechargee");
    }

    private void ensureSuccess(int statusCode, String action) {
        if (statusCode >= 200 && statusCode < 300) {
            return;
        }
        throw new IllegalStateException("Echec pendant " + action + " (HTTP " + statusCode + ")");
    }

    private String productPageUrl(String labelCode) {
        return BASE_URL + "/screen/product/tyres/" + labelCode + "?navigatingFrom=qr";
    }

    private String labelApiUrl(String labelCode) {
        return BASE_URL + "/api/products/tyres/" + labelCode + "/labels?noRedirect=true&format=PNG";
    }
}
