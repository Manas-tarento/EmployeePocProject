package com.tarento.EmployeePocProject.service;

import com.tarento.EmployeePocProject.model.Response;
import com.tarento.EmployeePocProject.model.SearchCriteria;

import java.util.Map;

public interface IEmployeeServiceV2 {
    Response getAllEmployeesFromES(Map<String, Object> searchCriteria);

    Response getEmployeeFromEsByFilter(SearchCriteria searchCriteria);

    Response searchEmployeeByRange(Map<String, Object> rangeCriteria);

    Response getEmployeeCountByField(String fieldName, String searchTerm);
}
