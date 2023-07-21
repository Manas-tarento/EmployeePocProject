package com.tarento.EmployeePocProject.service.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tarento.EmployeePocProject.entity.Employee;
import com.tarento.EmployeePocProject.es.IndexService;
import com.tarento.EmployeePocProject.model.Response;
import com.tarento.EmployeePocProject.model.SearchCriteria;
import com.tarento.EmployeePocProject.service.IEmployeeServiceV2;
import org.apache.commons.collections4.MapUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
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
import java.util.stream.Collectors;

@Service
public class EmployeeServiceV2Impl implements IEmployeeServiceV2 {

    @Autowired
    private IndexService indexService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmployeeServiceImpl employeeService;

    private static final String ES_INDEX_NAME = "employeenew";

    @Override
    public Response getAllEmployeesFromES(Map<String, Object> searchCriteria) {
        Response response = new Response();
        try {
            List<Employee> employeeList = new ArrayList<>();
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(QueryBuilders.matchAllQuery());
            Optional.ofNullable(searchCriteria)
                    .ifPresent(criteria -> {
                        searchSourceBuilder.from(criteria.containsKey("pageNo") ? (Integer) criteria.get("pageNo") : 0);
                        if (criteria.containsKey("pageSize")) {
                            searchSourceBuilder.size((Integer) criteria.get("pageSize"));
                        }
                    });
            SearchResponse searchResponse = indexService.getEsResult(ES_INDEX_NAME, "_doc", searchSourceBuilder);
            SearchHits hits = searchResponse.getHits();
            for (SearchHit hit : hits) {
                Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                Employee employee = objectMapper.convertValue(sourceAsMap, Employee.class);
                employeeList.add(employee);
            }
            if (!CollectionUtils.isEmpty(employeeList)) {
                response.getResponse().put("result", employeeList);
                response.getResponse().put("count", employeeList.size());
                employeeService.createSuccessResponse(response);
            } else {
                employeeService.createErrorResponse(response, "Employee Details not found", HttpStatus.OK, "SUCCESS");
            }
        } catch (Exception e) {
            employeeService.createErrorResponse(response, null, HttpStatus.INTERNAL_SERVER_ERROR, "FAILED");
        }
        return response;
    }

    @Override
    public Response getEmployeeFromEsByFilter(SearchCriteria searchCriteria) {
        Response response = new Response();
        try {
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            objectMapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE, true);
            List<String> fields = Optional.ofNullable(searchCriteria.getFields()).orElse(new ArrayList<>());
            SearchResponse searchResponse = indexService.searchEmployees(searchCriteria, ES_INDEX_NAME, "_doc", fields);
            SearchHit[] searchHits = searchResponse.getHits().getHits();
            List<Map<String, Object>> employeeDocuments = Arrays.stream(searchHits)
                    .map(hit -> {
                        Map<String, Object> sourceMap = hit.getSourceAsMap();
                        Map<String, Object> filteredMap = new HashMap<>();

                        if (!CollectionUtils.isEmpty(searchCriteria.getFields())) {
                            for (String field : fields) {
                                if (sourceMap.containsKey(field)) {
                                    filteredMap.put(field, sourceMap.get(field));
                                }
                            }
                        } else {
                            filteredMap = sourceMap;
                        }
                        return filteredMap;
                    })
                    .collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(employeeDocuments)) {
                response.getResponse().put("result", employeeDocuments);
                employeeService.createSuccessResponse(response);
            } else {
                employeeService.createErrorResponse(response, "Employee not available", HttpStatus.OK, "SUCCESS");
            }
            return response;
        } catch (Exception e) {
            employeeService.createErrorResponse(response, null, HttpStatus.INTERNAL_SERVER_ERROR, "FAILED");
            return response;
        }
    }

    @Override
    public Response searchEmployeeByRange(Map<String, Object> rangeCriteria) {
        Response response = new Response();
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        if (rangeCriteria.containsKey("startRange") && rangeCriteria.containsKey("endRange")) {
            Object startRange = rangeCriteria.get("startRange");
            Object endRange = rangeCriteria.get("endRange");

            if (startRange instanceof Integer && endRange instanceof Integer) {
                int minAge = (int) startRange;
                int maxAge = (int) endRange;

                if (minAge > 0 && maxAge > 0) {
                    searchSourceBuilder.query(QueryBuilders.rangeQuery("age").gte(minAge).lte(maxAge));
                }
            } else if (startRange instanceof String && endRange instanceof String) {
                try {
                    LocalDate startDate = LocalDate.parse(startRange.toString());
                    LocalDate endDate = LocalDate.parse(endRange.toString());

                    searchSourceBuilder.query(QueryBuilders.rangeQuery("dob").gte(startDate).lte(endDate));
                } catch (DateTimeParseException e) {
                    employeeService.createErrorResponse(response, null, HttpStatus.BAD_REQUEST, "Invalid date format");
                    return response;
                }
            }
            try {
                SearchResponse searchResponse = indexService.getEsResult(ES_INDEX_NAME, "_doc", searchSourceBuilder);
                SearchHits hits = searchResponse.getHits();
                List<Employee> employeeList = new ArrayList<>();
                for (SearchHit hit : hits) {
                    Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                    Employee employee = objectMapper.convertValue(sourceAsMap, Employee.class);
                    employeeList.add(employee);
                }
                if (!CollectionUtils.isEmpty(employeeList)) {
                    response.getResponse().put("result", employeeList);
                    response.getResponse().put("count", employeeList.size());
                    employeeService.createSuccessResponse(response);
                }
            } catch (Exception e) {
                employeeService.createErrorResponse(response, null, HttpStatus.INTERNAL_SERVER_ERROR, "FAILED");
            }
        }
        return response;
    }

    @Override
    public Response getEmployeeCountByField(String fieldName, String searchTerm) {
        Response response = new Response();
        try {
            SearchResponse searchResponse = indexService.searchEmployeesWithFacets(ES_INDEX_NAME, "_doc", fieldName, searchTerm);
            Aggregations aggregations = searchResponse.getAggregations();
            Terms aggregation = aggregations.get("by_" + fieldName);
            if (aggregation != null) {
                Map<String, Long> facetMap = new HashMap<>();
                for (Terms.Bucket bucket : aggregation.getBuckets()) {
                    String name = bucket.getKeyAsString();
                    long count = bucket.getDocCount();
                    facetMap.put(name, count);
                }
                if (!MapUtils.isEmpty(facetMap)) {
                    response.getResponse().put("result", facetMap);
                    employeeService.createSuccessResponse(response);
                }
            }
        } catch (Exception e) {
            employeeService.createErrorResponse(response, null, HttpStatus.INTERNAL_SERVER_ERROR, "FAILED");
        }
        return response;
    }
}