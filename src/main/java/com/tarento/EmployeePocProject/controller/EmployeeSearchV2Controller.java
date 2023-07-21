package com.tarento.EmployeePocProject.controller;

import com.tarento.EmployeePocProject.model.Response;
import com.tarento.EmployeePocProject.model.SearchCriteria;
import com.tarento.EmployeePocProject.service.IEmployeeServiceV3;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/employee/v2")
public class EmployeeSearchV2Controller {

    @Autowired
    private IEmployeeServiceV3 employeeServiceV3;

    @PostMapping("/search")
    public ResponseEntity<?> getEmployeeByFields(@RequestBody SearchCriteria searchCriteria) {
        Response response = employeeServiceV3.getEmployeeFromEs(searchCriteria);
        return new ResponseEntity<>(response, response.getResponseCode());
    }
}
