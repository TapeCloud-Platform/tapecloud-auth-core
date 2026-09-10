package com.tapecloud.auth.integration.tmdb;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tmdb")
public class TmdbProperties {

    private String apiKey;
    private String baseUrl;
    private String imageBaseUrl;
    private String language;

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getImageBaseUrl() { return imageBaseUrl; }
    public void setImageBaseUrl(String imageBaseUrl) { this.imageBaseUrl = imageBaseUrl; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
}
