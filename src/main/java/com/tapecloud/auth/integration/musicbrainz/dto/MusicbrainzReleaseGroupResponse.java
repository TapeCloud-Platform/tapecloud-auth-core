package com.tapecloud.auth.integration.musicbrainz.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MusicbrainzReleaseGroupResponse(
        @JsonProperty("release-groups") List<ReleaseGroup> releaseGroups
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ReleaseGroup(
            String id,
            String title,
            @JsonProperty("first-release-date") String firstReleaseDate,
            @JsonProperty("primary-type") String primaryType,
            @JsonProperty("secondary-types") List<String> secondaryTypes
    ) {

        /** Sin tipos secundarios el release-group es un álbum de estudio, no un compilado o directo. */
        public boolean isStudioAlbum() {
            return secondaryTypes == null || secondaryTypes.isEmpty();
        }

        public String year() {
            if (firstReleaseDate == null || firstReleaseDate.isBlank()) {
                return null;
            }
            return firstReleaseDate.substring(0, Math.min(4, firstReleaseDate.length()));
        }
    }
}
