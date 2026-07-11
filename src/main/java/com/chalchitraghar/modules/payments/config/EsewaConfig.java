package com.chalchitraghar.modules.payments.config;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
@Configuration @EnableConfigurationProperties(EsewaProperties.class)
public class EsewaConfig {
 @Bean RestClient esewaRestClient(EsewaProperties p){var f=new SimpleClientHttpRequestFactory();f.setConnectTimeout(Duration.ofMillis(p.connectTimeoutMs()));f.setReadTimeout(Duration.ofMillis(p.readTimeoutMs()));return RestClient.builder().requestFactory(f).build();}
}
