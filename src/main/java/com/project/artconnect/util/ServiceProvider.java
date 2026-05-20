package com.project.artconnect.util;

import com.project.artconnect.persistence.*;
import com.project.artconnect.service.*;
import com.project.artconnect.service.impl.*;

/**
 * Service Provider managing singleton instances of all services.
 * Switch between InMemory and JDBC implementations here.
 *
 * To use InMemory (no database needed):
 *   Set USE_DATABASE = false
 *
 * To use JDBC (requires MySQL running):
 *   Set USE_DATABASE = true
 */
public class ServiceProvider {

    // ← Change this to true to use the real database
    private static final boolean USE_DATABASE = true;

    private static final ArtistService artistService;
    private static final ArtworkService artworkService;
    private static final GalleryService galleryService;
    private static final WorkshopService workshopService;
    private static final CommunityService communityService;

    static {
        if (USE_DATABASE) {
            // JDBC implementations — use real MySQL database
            JdbcArtistDao artistDao = new JdbcArtistDao();
            JdbcArtworkDao artworkDao = new JdbcArtworkDao();
            JdbcGalleryDao galleryDao = new JdbcGalleryDao();
            JdbcWorkshopDao workshopDao = new JdbcWorkshopDao();
            JdbcCommunityMemberDao memberDao = new JdbcCommunityMemberDao();

            artistService  = new JdbcArtistService(artistDao);
            artworkService = new JdbcArtworkService(artworkDao);
            galleryService = new JdbcGalleryService(galleryDao);
            workshopService = new JdbcWorkshopService(workshopDao);
            communityService = new JdbcCommunityService(memberDao);

        } else {
            // InMemory implementations — no database needed
            InMemoryArtistService inMemArtist =
                    new InMemoryArtistService();
            InMemoryArtworkService inMemArtwork =
                    new InMemoryArtworkService();
            InMemoryGalleryService inMemGallery =
                    new InMemoryGalleryService();
            InMemoryWorkshopService inMemWorkshop =
                    new InMemoryWorkshopService();
            InMemoryCommunityService inMemCommunity =
                    new InMemoryCommunityService();

            inMemArtwork.initData(inMemArtist);
            inMemGallery.initData(inMemArtwork);
            inMemWorkshop.initData(inMemArtist);
            inMemCommunity.initData(inMemArtwork);

            artistService  = inMemArtist;
            artworkService = inMemArtwork;
            galleryService = inMemGallery;
            workshopService = inMemWorkshop;
            communityService = inMemCommunity;
        }
    }

    public static ArtistService getArtistService() {
        return artistService;
    }

    public static ArtworkService getArtworkService() {
        return artworkService;
    }

    public static GalleryService getGalleryService() {
        return galleryService;
    }

    public static WorkshopService getWorkshopService() {
        return workshopService;
    }

    public static CommunityService getCommunityService() {
        return communityService;
    }
}