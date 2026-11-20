package com.ecommerce.app.module.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductSearchIndexerTest {
    @Test
    void disabledClientSkipsIndexAndCatalogRead() {
        try (MockedStatic<ElasticsearchConfig> config = mockStatic(ElasticsearchConfig.class)) {
            config.when(ElasticsearchConfig::client).thenReturn(null);
            ProductSearchIndexer indexer = new ProductSearchIndexer();
            assertFalse(indexer.ensureIndex());
            assertEquals(0, indexer.reindexAll());
        }
    }

    @Test
    void unavailableElasticsearchStopsBeforeDatabaseRead() {
        ElasticsearchClient client = mock(ElasticsearchClient.class);
        when(client.indices()).thenThrow(new IllegalStateException("Connection refused"));
        try (MockedStatic<ElasticsearchConfig> config = mockStatic(ElasticsearchConfig.class)) {
            config.when(ElasticsearchConfig::client).thenReturn(client);
            assertEquals(0, new ProductSearchIndexer().reindexAll());
            verify(client, times(1)).indices();
        }
    }
}
