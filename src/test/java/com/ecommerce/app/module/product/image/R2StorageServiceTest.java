package com.ecommerce.app.module.product.image;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class R2StorageServiceTest {
    private final S3Client s3 = mock(S3Client.class);
    private static final String PUBLIC_URL = "https://pub-9769fbcd0cd54caf8433f76b6dc9a37b.r2.dev";

    @Test
    void rejectsMissingOrPrivateImageDomainsBeforeUploading() {
        assertThrows(IllegalArgumentException.class, () -> new R2StorageService(s3, "products", null));
        for (String value : List.of("", " ",
                "https://7f4616d0ea838285b079d2f738af9173.r2.cloudflarestorage.com",
                "https://7f4616d0ea838285b079d2f738af9173.r2.cloudflarestorage.com/products",
                "https://ACCOUNT.R2.CLOUDFLARESTORAGE.COM",
                "http://images.example.com", "not-a-url",
                "https://user:password@images.example.com",
                PUBLIC_URL + "?token=test", PUBLIC_URL + "#image")) {
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> new R2StorageService(s3, "products", value));
            assertTrue(error.getMessage().contains("R2_PUBLIC_BASE_URL"));
        }
        verifyNoInteractions(s3);
    }

    @Test
    void uploadedObjectUsesPublicDomainInsteadOfStorageApi() {
        R2StorageService storage = new R2StorageService(s3, "products", PUBLIC_URL + "///");
        String key = storage.upload("art.png", new ByteArrayInputStream(new byte[] {1}), 1, "image/png");
        assertTrue(key.endsWith("-art.png"));
        assertEquals(PUBLIC_URL + "/" + key, storage.getFileUrl(key));
        verify(s3).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void existingObjectKeyBuildsTheVerifiedPublicImageUrl() {
        R2StorageService storage = new R2StorageService(s3, "products", PUBLIC_URL);
        assertEquals(PUBLIC_URL + "/42fd5480-9afb-49d1-b162-f9f05e31dde9-DNS_Journey_to_Gmail_s_Mail_Server.png",
                storage.getFileUrl("42fd5480-9afb-49d1-b162-f9f05e31dde9-DNS_Journey_to_Gmail_s_Mail_Server.png"));
    }

    @Test
    void encodesProductAndReviewKeysWithoutChangingFolderSeparators() {
        R2StorageService storage = new R2StorageService(s3, "products", " https://images.example.com/artwork/// ");
        String key = "reviews/wall photo+#?%.png";
        String url = storage.getFileUrl(key);
        assertEquals("https://images.example.com/artwork/reviews/wall%20photo%2B%23%3F%25.png", url);
        assertEquals("/artwork/" + key, URI.create(url).getPath());
        assertNull(URI.create(url).getRawQuery());
        assertNull(URI.create(url).getRawFragment());
        assertThrows(IllegalArgumentException.class, () -> storage.getFileUrl(null));
        assertThrows(IllegalArgumentException.class, () -> storage.getFileUrl(""));
    }
}
