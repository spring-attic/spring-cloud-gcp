package com.example;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author vinicius
 */
@ConfigurationProperties("firebase.config")
public class FirebaseConfig {

	private String apiKey;
	private String appId;

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}
}
