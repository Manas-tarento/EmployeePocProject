package com.tarento.EmployeePocProject.controller;

import com.tarento.EmployeePocProject.entity.Employee;
import com.tarento.EmployeePocProject.model.Response;
import com.tarento.EmployeePocProject.service.IEmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/employee")
public class EmployeeController {

    @Autowired
    private IEmployeeService employeeService;

    @GetMapping("/read")
    public ResponseEntity<?> getAllEmployees() {
        Response response = employeeService.getAllEmployees();
        return new ResponseEntity<>(response, response.getResponseCode());
    }

    @GetMapping("/read/{id}")
    public ResponseEntity<?> getEmployeeById(@PathVariable Integer id) {
        Response response = employeeService.getEmployeeById(id);
        return new ResponseEntity<>(response, response.getResponseCode());
    }

    @PostMapping("/create")
    public ResponseEntity<?> saveEmployee(@RequestBody Employee employee) {
        Response response = employeeService.saveEmployee(employee);
        return new ResponseEntity<>(response, response.getResponseCode());
    }

    @PatchMapping("/update/{id}")
    public ResponseEntity<?> updateEmployee(@PathVariable Integer id, @RequestBody Employee employee) {
        Response response = employeeService.updateEmployee(id, employee);
        return new ResponseEntity<>(response, response.getResponseCode());
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteEmployee(@PathVariable Integer id) {
        String deleteStatus = employeeService.deleteEmployee(id);
        return new ResponseEntity<>(deleteStatus, HttpStatus.OK);
    }
}
