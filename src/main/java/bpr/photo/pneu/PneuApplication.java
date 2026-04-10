package bpr.photo.pneu;

import bpr.photo.pneu.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class PneuApplication {
    public static void main(String[] args) {
        SpringApplication.run(PneuApplication.class, args);
    }
}