package com.tarento.EmployeePocProject.service.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tarento.EmployeePocProject.es.IndexService;
import com.tarento.EmployeePocProject.model.Response;
import com.tarento.EmployeePocProject.model.SearchCriteria;
import com.tarento.EmployeePocProject.service.IEmployeeServiceV3;
import org.apache.commons.collections4.MapUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class EmployeeServiceV3Impl implements IEmployeeServiceV3 {

    @Autowired
    private IndexService indexService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmployeeServiceImpl employeeService;

    private static final String ES_INDEX_NAME = "employeenew";

    @Override
    public Response getEmployeeFromEs(SearchCriteria searchCriteria) {
        Response response = new Response();
        try {
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            objectMapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE, true);

            BoolQueryBuilder boolQuery = createFilterQuery(searchCriteria.getFilters());

            if (boolQuery != null) {
                SearchResponse searchResponse = executeQueryWithFilters(ES_INDEX_NAME, null, boolQuery, searchCriteria);
                SearchHits hits = searchResponse.getHits();
                List<Map<String, Object>> employeeDocuments = new ArrayList<>();
                for (SearchHit hit : hits) {
                    Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                    if (!CollectionUtils.isEmpty(searchCriteria.getFields())) {
                        Map<String, Object> filteredMap = new HashMap<>();
                        for (String field : searchCriteria.getFields()) {
                            if (sourceAsMap.containsKey(field)) {
                                filteredMap.put(field, sourceAsMap.get(field));
                            }
                        }
                        employeeDocuments.add(filteredMap);
                    } else {
                        employeeDocuments.add(sourceAsMap);
                    }
                }
                if (!CollectionUtils.isEmpty(employeeDocuments)) {
                    response.getResponse().put("result", employeeDocuments);
                    if (!CollectionUtils.isEmpty(searchCriteria.getFacet())) {
                        Map<String, List<Map<String, Object>>> facetResults = processFacetResults(searchResponse, searchCriteria.getFacet());
                        response.getResponse().put("facets", facetResults);
                    }
                    employeeService.createSuccessResponse(response);
                } else {
                    employeeService.createErrorResponse(response, "Employee not available", HttpStatus.OK, "SUCCESS");
                }
            }
            return response;
        } catch (Exception e) {
            employeeService.createErrorResponse(response, null, HttpStatus.INTERNAL_SERVER_ERROR, "FAILED");
            return response;
        }
    }

    private BoolQueryBuilder createFilterQuery(Map<String, Object> filters) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        if (MapUtils.isNotEmpty(filters)) {
            for (Map.Entry<String, Object> entry : filters.entrySet()) {
                String fieldName = entry.getKey();
                Object filterValue = entry.getValue();
                if (filterValue instanceof String || filterValue instanceof Integer) {
                    boolQuery.filter(QueryBuilders.matchQuery(fieldName, filterValue));
                } else if (filterValue instanceof Map) {
                    Map<String, Object> rangeCriteria = (Map<String, Object>) filterValue;
                    Object startRange = rangeCriteria.get("min");
                    Object endRange = rangeCriteria.get("max");
                    if (startRange instanceof Integer && endRange instanceof Integer) {
                        int minRange = (int) startRange;
                        int maxRange = (int) endRange;
                        boolQuery.filter(QueryBuilders.rangeQuery(fieldName).gte(minRange).lte(maxRange));
                    } else if (startRange instanceof String && endRange instanceof String) {
                        try {
                            LocalDate startDate = LocalDate.parse(startRange.toString());
                            LocalDate endDate = LocalDate.parse(endRange.toString());
                            boolQuery.filter(QueryBuilders.rangeQuery(fieldName).gte(startDate).lte(endDate));
                        } catch (DateTimeParseException e) {
                            System.out.println("Error while creating filter query");
                            return null;
                        }
                    }
                }
            }
        } else {
            boolQuery.filter(QueryBuilders.matchAllQuery());
        }
        return boolQuery.hasClauses() ? boolQuery : null;
    }

    private Map<String, List<Map<String, Object>>> processFacetResults(SearchResponse searchResponse, List<String> facetFields) {
        Map<String, List<Map<String, Object>>> facetResults = new HashMap<>();
        Aggregations aggregations = searchResponse.getAggregations();
        for (String facetField : facetFields) {
            Terms aggregation = aggregations.get("by_" + facetField);
            List<Map<String, Object>> facetValues = new ArrayList<>();
            if (aggregation != null) {
                for (Terms.Bucket bucket : aggregation.getBuckets()) {
                    String name = bucket.getKeyAsString();
                    long count = bucket.getDocCount();
                    Map<String, Object> facetValue = new HashMap<>();
                    facetValue.put("value", name);
                    facetValue.put("count", count);
                    facetValues.add(facetValue);
                }
            }
            facetResults.put(facetField, facetValues);
        }
        return facetResults;
    }

    private SearchResponse executeQueryWithFilters(String index, String type, BoolQueryBuilder query, SearchCriteria searchCriteria) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        try {
            if (query != null) {
                searchSourceBuilder.query(query);
            }
            if (!CollectionUtils.isEmpty(searchCriteria.getFields())) {
                searchSourceBuilder.fetchSource(searchCriteria.getFields().toArray(new String[0]), null);
            }
            Optional.ofNullable(searchCriteria.getPageNo())
                    .ifPresentOrElse(
                            pageNo -> searchSourceBuilder.from(Integer.parseInt(pageNo)),
                            () -> searchSourceBuilder.from(0)
                    );

            Optional.ofNullable(searchCriteria.getPageSize())
                    .ifPresent(pageSize -> searchSourceBuilder.size(Integer.parseInt(pageSize)));
            if (!CollectionUtils.isEmpty(searchCriteria.getFacet())) {
                for (String facetField : searchCriteria.getFacet()) {
                    searchSourceBuilder.aggregation(AggregationBuilders.terms("by_" + facetField).field(facetField));
                }
            }
            return indexService.getEsResult(index, type, searchSourceBuilder);
        } catch (Exception e) {
            return null;
        }
    }
}