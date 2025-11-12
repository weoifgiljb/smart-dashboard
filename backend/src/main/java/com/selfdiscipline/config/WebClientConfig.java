package com.selfdiscipline.config;

import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Optional;

@Configuration
public class WebClientConfig {

	@Bean
	public WebClient openAIWebClient(OpenAIConfig cfg) {
		String baseUrl = Optional.ofNullable(cfg.getBaseUrl()).filter(s -> !s.isBlank())
				.orElse("https://api.aabao.vip/v1");

		HttpClient httpClient = HttpClient.create()
				.responseTimeout(Duration.ofSeconds(30))
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);

		return WebClient.builder()
				.baseUrl(baseUrl)
				.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + Optional.ofNullable(cfg.getApiKey()).orElse(""))
				.clientConnector(new ReactorClientHttpConnector(httpClient))
				.filter(retryWhen429And5xx())
				.build();
	}

	private ExchangeFilterFunction retryWhen429And5xx() {
		return (request, next) -> next.exchange(request)
				.flatMap(response -> {
					int code = response.statusCode().value();
					if (code == 429 || response.statusCode().is5xxServerError()) {
						return Mono.error(new RuntimeException("retryable_status_" + code));
					}
					return Mono.just(response);
				})
				.retryWhen(Retry.backoff(2, Duration.ofMillis(300)).maxBackoff(Duration.ofSeconds(2)));
	}
}



