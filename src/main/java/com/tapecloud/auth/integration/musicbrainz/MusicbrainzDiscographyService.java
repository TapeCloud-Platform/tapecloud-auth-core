package com.tapecloud.auth.integration.musicbrainz;

import com.tapecloud.auth.discovery.DiscoveryItem;
import com.tapecloud.auth.integration.musicbrainz.dto.MusicbrainzArtistSearchResponse;
import com.tapecloud.auth.integration.musicbrainz.dto.MusicbrainzReleaseGroupResponse;
import com.tapecloud.auth.integration.musicbrainz.dto.MusicbrainzReleaseGroupResponse.ReleaseGroup;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Service;

@Service
public class MusicbrainzDiscographyService {

    private static final org.slf4j.Logger LOG =
            org.slf4j.LoggerFactory.getLogger(MusicbrainzDiscographyService.class);

    private static final String COVER_ART_URL = "https://coverartarchive.org/release-group/%s/front-500";

    private final MusicbrainzClient client;

    public MusicbrainzDiscographyService(MusicbrainzClient client) {
        this.client = client;
    }

    /**
     * MusicBrainz sí distingue el tipo de lanzamiento, cosa que Last.fm no expone. Las portadas
     * llegan desde fuera porque Cover Art Archive no cubre todos los release-groups.
     */
    public List<DiscoveryItem> findReleases(
            String artistName, String artistMbid, Function<String, String> coverLookup, int limit) {
        List<ReleaseGroup> groups = releaseGroups(artistMbid);

        // Last.fm a veces guarda un MBID viejo que ya no tiene lanzamientos asociados.
        if (groups.isEmpty()) {
            groups = releaseGroups(resolveMbid(artistName));
        }

        return groups.stream()
                .filter(group -> group.title() != null && !group.title().isBlank())
                .sorted(Comparator.comparing(
                                ReleaseGroup::firstReleaseDate,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .reversed())
                .limit(limit)
                .map(group -> toItem(group, artistName, coverLookup))
                .toList();
    }

    private List<ReleaseGroup> releaseGroups(String artistMbid) {
        if (artistMbid == null || artistMbid.isBlank()) {
            return List.of();
        }
        try {
            MusicbrainzReleaseGroupResponse response = client.fetchReleaseGroups(artistMbid, 100);
            if (response == null || response.releaseGroups() == null) {
                return List.of();
            }
            return response.releaseGroups();
        } catch (Exception e) {
            LOG.warn("MusicBrainz no devolvió los lanzamientos de {}: {}", artistMbid, e.toString());
            return List.of();
        }
    }

    private String resolveMbid(String artistName) {
        try {
            MusicbrainzArtistSearchResponse search = client.searchArtist(artistName);
            if (search == null || search.artists() == null || search.artists().isEmpty()) {
                return null;
            }
            return search.artists().get(0).id();
        } catch (Exception e) {
            LOG.warn("MusicBrainz no encontró a {}: {}", artistName, e.toString());
            return null;
        }
    }

    private DiscoveryItem toItem(ReleaseGroup group, String artistName, Function<String, String> coverLookup) {
        String cover = coverLookup.apply(group.title());
        return new DiscoveryItem(
                group.id(),
                group.title(),
                artistName,
                describe(group),
                cover != null ? cover : COVER_ART_URL.formatted(group.id()),
                artistName,
                kindOf(group));
    }

    private static String describe(ReleaseGroup group) {
        String year = group.year();
        String type = label(group);
        return year == null ? type : "%s · %s".formatted(year, type);
    }

    private static String label(ReleaseGroup group) {
        if (group.secondaryTypes() != null && !group.secondaryTypes().isEmpty()) {
            return String.join(", ", group.secondaryTypes());
        }
        return group.primaryType() == null ? "Lanzamiento" : group.primaryType();
    }

    private static String kindOf(ReleaseGroup group) {
        if (!group.isStudioAlbum()) {
            return "other";
        }
        return Map.of("Single", "single", "EP", "single")
                .getOrDefault(group.primaryType(), "album");
    }
}
