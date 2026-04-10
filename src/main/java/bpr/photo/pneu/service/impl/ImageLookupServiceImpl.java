package bpr.photo.pneu.service.impl;

import bpr.photo.pneu.config.AppProperties;
import bpr.photo.pneu.service.ImageLookupService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.time.Duration;

@Service
public class ImageLookupServiceImpl implements ImageLookupService {

    private final WebClient webClient;
    private final AppProperties props;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ImageLookupServiceImpl(WebClient.Builder builder, AppProperties props) {
        this.props = props;
        this.webClient = builder
                .defaultHeader(HttpHeaders.USER_AGENT, props.getRequest().getUserAgent())
                .build();
    }

    public LookupResult findImageUrl(String ean) {
        String url = props.getRequest().getBaseUrl().replace("{ean}", ean);

        try {
            String body = webClient.get()
                    .uri(URI.create(url))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(props.getRequest().getTimeoutSeconds()))
                    .block();

            if (body == null || body.isBlank()) {
                return new LookupResult(ean, "", "EMPTY_RESPONSE");
            }

            String mode = props.getParsing().getMode();

            if ("json".equalsIgnoreCase(mode)) {
                return parseJson(ean, body);
            }

            return parseHtml(ean, body, url);

        } catch (Exception e) {
            return new LookupResult(ean, "", "ERROR: " + e.getClass().getSimpleName());
        }
    }

    private LookupResult parseHtml(String ean, String body, String pageUrl) {
        Document doc = Jsoup.parse(body, pageUrl);
        Element img = doc.selectFirst(props.getParsing().getImageCssSelector());

        if (img == null) {
            return new LookupResult(ean, "", "IMAGE_NOT_FOUND");
        }

        String attr = props.getParsing().getImageAttribute();
        String imageUrl = img.absUrl(attr);

        if (imageUrl == null || imageUrl.isBlank()) {
            imageUrl = img.attr(attr);
        }

        if (imageUrl == null || imageUrl.isBlank()) {
            return new LookupResult(ean, "", "IMAGE_ATTR_EMPTY");
        }

        return new LookupResult(ean, imageUrl, "OK");
    }

    private LookupResult parseJson(String ean, String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        JsonNode node = root.at(props.getParsing().getJsonImagePath());

        if (node.isMissingNode() || node.isNull() || node.asText().isBlank()) {
            return new LookupResult(ean, "", "IMAGE_NOT_FOUND");
        }

        return new LookupResult(ean, node.asText(), "OK");
    }

    public record LookupResult(String ean, String imageUrl, String status) {}
}