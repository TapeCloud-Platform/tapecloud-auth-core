package com.tapecloud.auth.integration.musicbrainz;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "musicbrainz")
public class MusicbrainzProperties {

    private String baseUrl;
    private String appName;
    private String appVersion;
    private String contactEmail;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getAppName() { return appName; }
    public void setAppName(String appName) { this.appName = appName; }

    public String getAppVersion() { return appVersion; }
    public void setAppVersion(String appVersion) { this.appVersion = appVersion; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    /** MusicBrainz exige identificar la app con nombre, versión y contacto. */
    public String userAgent() {
        return "%s/%s ( %s )".formatted(appName, appVersion, contactEmail);
    }
}
