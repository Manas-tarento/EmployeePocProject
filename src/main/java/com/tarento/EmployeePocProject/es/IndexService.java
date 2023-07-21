package com.tarento.EmployeePocProject.es;

import com.tarento.EmployeePocProject.model.SearchCriteria;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class IndexService {

    @Autowired
    private RestHighLevelClient elasticsearchClient;

    public RestStatus addEntity(String index, String indexType, int entityId, Map<String, Object> indexDocument) {
        IndexRequest indexRequest = new IndexRequest(index, indexType, String.valueOf(entityId))
                .source(indexDocument, XContentType.JSON);
        try {
            IndexResponse response = elasticsearchClient.index(indexRequest, RequestOptions.DEFAULT);
            return response.status();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public RestStatus updateEntity(String index, String indexType, int entityId, Map<String, Object> indexDocument) {
        UpdateRequest updateRequest = new UpdateRequest(index.toLowerCase(), indexType, String.valueOf(entityId))
                .doc(indexDocument);

        try {
            UpdateResponse response = elasticsearchClient.update(updateRequest, RequestOptions.DEFAULT);
            return response.status();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


/*    public SearchResponse searchEmployees(Map<String, Object> searchCriteria, String indexName, String type) throws IOException {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices(indexName);
        if (type != null && !type.isEmpty()) {
            searchRequest.types(type);
        }
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchCriteria.forEach((field, value) -> {
            searchSourceBuilder.query(QueryBuilders.matchQuery(field, value));
        });
        searchRequest.source(searchSourceBuilder);
        return elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
    }*/

    public SearchResponse searchEmployees(SearchCriteria searchCriteria, String indexName, String type, List<String> fields) throws IOException {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices(indexName);
        if (type != null && !type.isEmpty()) {
            searchRequest.types(type);
        }
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchCriteria.getFilters().forEach((field, value) -> {
            searchSourceBuilder.query(QueryBuilders.matchQuery(field, value));
        });
        searchSourceBuilder.fetchSource(fields.toArray(new String[0]), null);
        searchRequest.source(searchSourceBuilder);
        return elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
    }

    public SearchResponse searchEmployeesWithFacets(String indexName, String type, String fieldType, String searchTerm) throws IOException {
        SearchRequest searchRequest = new SearchRequest(indexName);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        if (!StringUtils.isEmpty(searchTerm)) {
            searchSourceBuilder.query(QueryBuilders.matchQuery(fieldType, searchTerm));
        } else {
            searchSourceBuilder.query(QueryBuilders.matchAllQuery());
        }
        TermsAggregationBuilder aggregationBuilder = AggregationBuilders.terms("by_" + fieldType)
                .field(fieldType + ".keyword")
                .size(10);
        searchSourceBuilder.aggregation(aggregationBuilder);
        searchRequest.source(searchSourceBuilder);
        return elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
    }

    public SearchResponse getEsResult(String indexName, String type, SearchSourceBuilder searchSourceBuilder) throws IOException {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices(indexName);
        if (!StringUtils.isEmpty(type))
            searchRequest.types(type);
        searchRequest.source(searchSourceBuilder);
        return elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
    }
}
