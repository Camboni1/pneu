package bpr.photo.pneu.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Request request = new Request();
    private Parsing parsing = new Parsing();
    private Excel excel = new Excel();

    public Request getRequest() { return request; }
    public void setRequest(Request request) { this.request = request; }

    public Parsing getParsing() { return parsing; }
    public void setParsing(Parsing parsing) { this.parsing = parsing; }

    public Excel getExcel() { return excel; }
    public void setExcel(Excel excel) { this.excel = excel; }

    public static class Request {
        private String baseUrl;
        private int timeoutSeconds = 20;
        private String userAgent = "Mozilla/5.0";

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }

        public String getUserAgent() { return userAgent; }
        public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    }

    public static class Parsing {
        private String mode = "html";
        private String imageCssSelector = "img";
        private String imageAttribute = "src";
        private String jsonImagePath = "/image";

        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }

        public String getImageCssSelector() { return imageCssSelector; }
        public void setImageCssSelector(String imageCssSelector) { this.imageCssSelector = imageCssSelector; }

        public String getImageAttribute() { return imageAttribute; }
        public void setImageAttribute(String imageAttribute) { this.imageAttribute = imageAttribute; }

        public String getJsonImagePath() { return jsonImagePath; }
        public void setJsonImagePath(String jsonImagePath) { this.jsonImagePath = jsonImagePath; }
    }

    public static class Excel {
        private int sheetIndex = 0;
        private String eanColumnName = "ean";
        private String outputImageColumnName = "image_url";
        private String outputStatusColumnName = "status";

        public int getSheetIndex() { return sheetIndex; }
        public void setSheetIndex(int sheetIndex) { this.sheetIndex = sheetIndex; }

        public String getEanColumnName() { return eanColumnName; }
        public void setEanColumnName(String eanColumnName) { this.eanColumnName = eanColumnName; }

        public String getOutputImageColumnName() { return outputImageColumnName; }
        public void setOutputImageColumnName(String outputImageColumnName) { this.outputImageColumnName = outputImageColumnName; }

        public String getOutputStatusColumnName() { return outputStatusColumnName; }
        public void setOutputStatusColumnName(String outputStatusColumnName) { this.outputStatusColumnName = outputStatusColumnName; }
    }
}