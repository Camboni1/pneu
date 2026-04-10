package bpr.photo.pneu.service.impl;

import bpr.photo.pneu.config.AppProperties;
import bpr.photo.pneu.service.ImageLookupService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class ImageLookupServiceImpl implements ImageLookupService {

    private final WebClient webClient;
    private final AppProperties props;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(ImageLookupServiceImpl.class);

    public ImageLookupServiceImpl(WebClient.Builder builder, AppProperties props) {
        this.props = props;
        this.webClient = builder
                .defaultHeader(HttpHeaders.USER_AGENT, props.getRequest().getUserAgent())
                .build();
    }

    @Override
    public LookupResult findImages(String ean) {
        String url = props.getRequest().getBaseUrl().replace("{ean}", ean);

        try {
            String body = webClient.get()
                    .uri(URI.create(url))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(props.getRequest().getTimeoutSeconds()))
                    .block();

            if (body == null || body.isBlank()) {
                return new LookupResult(ean, "", List.of(), "EMPTY_RESPONSE");
            }

            String mode = props.getParsing().getMode();

            if ("json".equalsIgnoreCase(mode)) {
                return parseJson(ean, body);
            }

            return parseHtml(ean, body, url);

        } catch (Exception e) {
            log.error("Erreur pendant le traitement de l'EAN {}", ean, e);
            return new LookupResult(ean, "", List.of(), "ERROR: " + e.getClass().getSimpleName());
        }
    }

    @Override
    public void downloadImage(String imageUrl, Path outputPath) throws Exception {
        byte[] bytes = webClient.get()
                .uri(URI.create(imageUrl))
                .retrieve()
                .bodyToMono(byte[].class)
                .timeout(Duration.ofSeconds(props.getRequest().getTimeoutSeconds()))
                .block();

        if (bytes == null || bytes.length == 0) {
            throw new IllegalStateException("Image vide ou introuvable : " + imageUrl);
        }

        Files.createDirectories(outputPath.getParent());
        Files.write(
                outputPath,
                bytes,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    private LookupResult parseHtml(String ean, String body, String pageUrl) {
        Document doc = Jsoup.parse(body, pageUrl);

        Element primaryImg = doc.selectFirst(props.getParsing().getImageCssSelector());
        String primaryImageUrl = "";

        if (primaryImg != null) {
            String attr = props.getParsing().getImageAttribute();
            primaryImageUrl = primaryImg.absUrl(attr);

            if (primaryImageUrl.isBlank()) {
                primaryImageUrl = primaryImg.attr(attr);
            }
        }

        Elements secondaryAnchors = doc.select("a.colorbox[href]");
        Set<String> secondarySet = new LinkedHashSet<>();

        for (Element anchor : secondaryAnchors) {
            String href = anchor.absUrl("href");
            if (href.isBlank()) {
                href = anchor.attr("href");
            }

            if (!href.isBlank()) {
                secondarySet.add(href);
            }
        }

        if (!primaryImageUrl.isBlank()) {
            secondarySet.remove(primaryImageUrl);
        }

        List<String> secondaryImages = new ArrayList<>(secondarySet);

        if (primaryImageUrl.isBlank() && secondaryImages.isEmpty()) {
            return new LookupResult(ean, "", List.of(), "IMAGE_NOT_FOUND");
        }

        if (primaryImageUrl.isBlank() && !secondaryImages.isEmpty()) {
            primaryImageUrl = secondaryImages.get(0);
            secondaryImages.remove(0);
        }

        return new LookupResult(ean, primaryImageUrl, secondaryImages, "OK");
    }

    private LookupResult parseJson(String ean, String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        JsonNode node = root.at(props.getParsing().getJsonImagePath());

        if (node.isMissingNode() || node.isNull() || node.asText().isBlank()) {
            return new LookupResult(ean, "", List.of(), "IMAGE_NOT_FOUND");
        }

        return new LookupResult(ean, node.asText(), List.of(), "OK");
    }
}