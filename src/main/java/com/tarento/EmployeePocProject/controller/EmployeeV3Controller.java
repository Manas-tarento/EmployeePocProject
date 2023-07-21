package com.tarento.EmployeePocProject.controller;

import com.tarento.EmployeePocProject.entity.Employee;
import com.tarento.EmployeePocProject.entity.EmployeeV2;
import com.tarento.EmployeePocProject.model.Response;
import com.tarento.EmployeePocProject.service.IEmployeeServiceV4;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/employee/v3")
public class EmployeeV3Controller {

    @Autowired
    private IEmployeeServiceV4 employeeServiceV4;

    @PostMapping("/create")
    public ResponseEntity<?> saveEmployee(@RequestBody EmployeeV2 employeeV2) {
        Response response = employeeServiceV4.saveEmployee(employeeV2);
        return new ResponseEntity<>(response, response.getResponseCode());
    }

    @PatchMapping("/update/{id}")
    public ResponseEntity<?> updateEmployee(@PathVariable Integer id, @RequestBody EmployeeV2 employeeV2) {
        Response response = employeeServiceV4.updateEmployee(id, employeeV2);
        return new ResponseEntity<>(response, response.getResponseCode());
    }

    @GetMapping("/read/{id}")
    public ResponseEntity<?> getEmployeeById(@PathVariable Integer id) {
        Response response = employeeServiceV4.getEmployeeById(id);
        return new ResponseEntity<>(response, response.getResponseCode());
    }
}
