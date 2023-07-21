package com.tarento.EmployeePocProject.controller;

import com.tarento.EmployeePocProject.model.Response;
import com.tarento.EmployeePocProject.model.SearchCriteria;
import com.tarento.EmployeePocProject.service.IEmployeeServiceV2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/search/employee")
public class EmployeeSearchController {

    @Autowired
    private IEmployeeServiceV2 employeeServiceV2;

    @PostMapping("/all")
    public ResponseEntity<?> getAllEmployees(@RequestBody Map<String, Object> searchCriteria) {
        Response response = employeeServiceV2.getAllEmployeesFromES(searchCriteria);
        return new ResponseEntity<>(response, response.getResponseCode());
    }

    @PostMapping("/fields")
    public ResponseEntity<?> getEmployeeByFields(@RequestBody SearchCriteria searchCriteria) {
        Response response = employeeServiceV2.getEmployeeFromEsByFilter(searchCriteria);
        return new ResponseEntity<>(response, response.getResponseCode());
    }

    @PostMapping("/range")
    public ResponseEntity<?> getEmployeeDetailsByRange(@RequestBody Map<String, Object> rangeCriteria) {
        Response response = employeeServiceV2.searchEmployeeByRange(rangeCriteria);
        return new ResponseEntity<>(response, response.getResponseCode());
    }

    @GetMapping("/count")
    public ResponseEntity<?> getEmployeeCountByFieldType(@RequestParam String fieldType, @RequestParam(required = false) String searchTerm) {
        Response response = employeeServiceV2.getEmployeeCountByField(fieldType, searchTerm);
        return new ResponseEntity<>(response, response.getResponseCode());
    }
}