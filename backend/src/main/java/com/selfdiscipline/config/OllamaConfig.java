package com.selfdiscipline.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OllamaConfig {

	@Value("${ollama.base-url:http://127.0.0.1:11434}")
	private String baseUrl;

	@Value("${ollama.model:llama3.1}")
	private String model;

	public String getBaseUrl() {
		return baseUrl;
	}

	public String getModel() {
		return model;
	}
}





