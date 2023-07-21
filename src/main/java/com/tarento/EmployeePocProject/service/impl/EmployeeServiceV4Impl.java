package com.tarento.EmployeePocProject.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.tarento.EmployeePocProject.cache.CacheService;
import com.tarento.EmployeePocProject.entity.EmployeeV2;
import com.tarento.EmployeePocProject.es.IndexService;
import com.tarento.EmployeePocProject.model.Response;
import com.tarento.EmployeePocProject.repo.EmployeeRepoV2;
import com.tarento.EmployeePocProject.service.IEmployeeServiceV4;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class EmployeeServiceV4Impl implements IEmployeeServiceV4 {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmployeeRepoV2 employeeRepoV2;

    @Autowired
    private EmployeeServiceImpl employeeService;

    @Autowired
    private IndexService indexService;

    @Autowired
    private CacheService cacheService;

    private static final String ES_INDEX_NAME = "employeenew";

    @Override
    public Response saveEmployee(EmployeeV2 employee) {
        Response response = new Response();
        String errorMessage = validateEmployeeSchema(employee.getJsonData());
        if (!StringUtils.isEmpty(errorMessage)) {
            employeeService.createErrorResponse(response, errorMessage, HttpStatus.BAD_REQUEST, "FAILED");
            return response;
        }
        try {
            EmployeeV2 savedEmployee = employeeRepoV2.save(employee);
            if (!ObjectUtils.isEmpty(savedEmployee)) {
                Map<String, Object> employeeMap = objectMapper.convertValue(employee.getJsonData(), new TypeReference<>() {
                });
                employeeMap.put("id", employee.getId());
                indexService.addEntity(ES_INDEX_NAME, "_doc", employee.getId(), employeeMap);
                cacheService.putCache(employee.getId(), employee);
            }
            response.getResponse().put("result", savedEmployee);
            employeeService.createSuccessResponse(response);
        } catch (Exception e) {
            employeeService.createErrorResponse(response, null, HttpStatus.INTERNAL_SERVER_ERROR, "FAILED");
        }
        return response;
    }

    @Override
    public Response getEmployeeById(int id) {
        Response response = new Response();
        try {
            String employeeJson = cacheService.getCache(String.valueOf(id));
            if (employeeJson != null) {
                EmployeeV2 employee = objectMapper.readValue(employeeJson, EmployeeV2.class);
                response.getResponse().put("result-cache", employee);
                employeeService.createSuccessResponse(response);
            } else {
                Optional<EmployeeV2> employeeOptional = employeeRepoV2.findById(id);
                if (employeeOptional.isPresent()) {
                    EmployeeV2 employeeV2 = employeeOptional.get();
                    cacheService.putCache(id, employeeV2);
                    response.getResponse().put("result-db", employeeV2);
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

   /* private String validateEmployeeSchema(EmployeeV2 employee) {
        try {
            Map<String, Object> employeeJson = employee.getJsonData();
            List<String> errList = new ArrayList<>();

            if (employeeJson.get("name") == null || StringUtils.isEmpty(employeeJson.get("name").toString())) {
                errList.add("name");
            }
            if (employeeJson.get("age") == null || StringUtils.isEmpty(employeeJson.get("age").toString())) {
                errList.add("age");
            }
            if (employeeJson.get("designation") == null || StringUtils.isEmpty(employeeJson.get("designation").toString())) {
                errList.add("designation");
            }
            if (employeeJson.get("dob") == null || StringUtils.isEmpty(employeeJson.get("dob").toString())) {
                errList.add("dob");
            }
            if (!errList.isEmpty()) {
                return "Failed to register Employee. Missing Params - " + errList;
            }
        } catch (Exception e) {
            return "Invalid JSON format";
        }

        return "";
    }*/

    @Override
    public Response updateEmployee(Integer id, EmployeeV2 updatedEmployee) {
        Response response = new Response();
        try {
            Optional<EmployeeV2> existingEmployeeOptional = employeeRepoV2.findById(id);
            if (existingEmployeeOptional.isPresent()) {
                EmployeeV2 existingEmployee = existingEmployeeOptional.get();
                existingEmployee.setJsonData(updatedEmployee.getJsonData());
                EmployeeV2 updatedEmployeeEntity = employeeRepoV2.save(existingEmployee);
                if (!ObjectUtils.isEmpty(updatedEmployeeEntity)) {
                    Map<String, Object> employeeMap = objectMapper.convertValue(updatedEmployeeEntity.getJsonData(), new TypeReference<>() {
                    });
                    updatedEmployeeEntity.setId(id);
                    indexService.updateEntity(ES_INDEX_NAME, "_doc", id, employeeMap);
                    cacheService.putCache(id, updatedEmployeeEntity);
                }
                employeeService.createSuccessResponse(response);
            } else {
                employeeService.createErrorResponse(response, "Employee not found with ID: " + id, HttpStatus.INTERNAL_SERVER_ERROR, "FAILED");
            }
        } catch (Exception e) {
            employeeService.createErrorResponse(response, null, HttpStatus.INTERNAL_SERVER_ERROR, "FAILED");
        }
        return response;
    }

    private String validateEmployeeSchema(Map<String, Object> employee) {
        try {
            File schemaFile = new File("/home/manas/POC/BackUp/EmployeePocProject/schema-validator.json");
            JsonNode schemaNode = JsonLoader.fromFile(schemaFile);
            JsonSchemaFactory schemaFactory = JsonSchemaFactory.byDefault();
            JsonSchema schema = schemaFactory.getJsonSchema(schemaNode);
            String employeeJson = objectMapper.writeValueAsString(employee);
            JsonNode employeeNode = JsonLoader.fromString(employeeJson);
            ProcessingReport report = schema.validateUnchecked(employeeNode);
            if (!report.isSuccess()) {
                Set<String> missingProperties = StreamSupport.stream(schemaNode.get("required").spliterator(), false)
                        .map(JsonNode::asText).collect(Collectors.toSet());
                employeeNode.fieldNames().forEachRemaining(missingProperties::remove);
                if (!missingProperties.isEmpty()) {
                    return "Failed to register Employee. Missing Params - " +
                            String.join(", ", missingProperties);
                }
            }
        } catch (IOException | ProcessingException e) {
            return "Invalid JSON format or schema";
        }
        return "";
    }
}
