package com.tarento.EmployeePocProject.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tarento.EmployeePocProject.entity.Employee;
import com.tarento.EmployeePocProject.es.IndexService;
import com.tarento.EmployeePocProject.model.RespParam;
import com.tarento.EmployeePocProject.model.Response;
import com.tarento.EmployeePocProject.repo.EmployeeRepo;
import com.tarento.EmployeePocProject.service.IEmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.*;

@Service
public class EmployeeServiceImpl implements IEmployeeService {

    @Autowired
    private EmployeeRepo employeeRepo;

    @Autowired
    private IndexService indexService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String ES_INDEX_NAME = "employeenew";

    @Override
    public Response getAllEmployees() {
        Response response = new Response();
        try {
            List<Employee> employeeList = employeeRepo.findAll();
            if (!CollectionUtils.isEmpty(employeeList)) {
                response.getResponse().put("result", employeeList);
                response.getResponse().put("count", employeeList.size());
                createSuccessResponse(response);
            }
        } catch (Exception e) {
            createErrorResponse(response, null, HttpStatus.INTERNAL_SERVER_ERROR, "FAILED");
        }
        return response;
    }

    @Override
    public Response getEmployeeById(int id) {
        Response response = new Response();
        try {
            Optional<Employee> employeeOptional = employeeRepo.findById(id);
            if (employeeOptional.isPresent()) {
                Employee employee = employeeOptional.get();
                response.getResponse().put("result", employee);
                createSuccessResponse(response);
            } else {
                createErrorResponse(response, "Employee not available", HttpStatus.OK, "SUCCESS");
            }
            return response;
        } catch (Exception e) {
            createErrorResponse(response, null, HttpStatus.INTERNAL_SERVER_ERROR, "FAILED");
            return response;
        }
    }

    @Override
    public Response saveEmployee(Employee employee) {
        Response response = new Response();
        String errorMessage = validateEmployee(employee);
        if (StringUtils.isEmpty(errorMessage)) {
            try {
                Employee emp = employeeRepo.save(employee);
                if (!ObjectUtils.isEmpty(emp)) {
                    Map<String, Object> employeeMap = objectMapper.convertValue(employee, new TypeReference<Map<String, Object>>() {
                    });
                    indexService.addEntity(ES_INDEX_NAME, "_doc", employee.getId(), employeeMap);
                }
                response.getResponse().put("result", emp);
                createSuccessResponse(response);
            } catch (Exception e) {
                createErrorResponse(response, null, HttpStatus.INTERNAL_SERVER_ERROR, "FAILED");
            }
        } else {
            createErrorResponse(response, errorMessage, HttpStatus.BAD_REQUEST, "FAILED");
        }
        return response;
    }

    @Override
    public Response updateEmployee(Integer id, Employee updatedEmployee) {
        Response response = new Response();
        try {
            Optional<Employee> existingEmployeeOptional = employeeRepo.findById(id);
            if (existingEmployeeOptional.isPresent()) {
                Employee existingEmployee = existingEmployeeOptional.get();
                if (updatedEmployee.getName() != null) {
                    existingEmployee.setName(updatedEmployee.getName());
                }
                if (updatedEmployee.getAge() != 0) {
                    existingEmployee.setAge(updatedEmployee.getAge());
                }
                if (updatedEmployee.getDesignation() != null) {
                    existingEmployee.setDesignation(updatedEmployee.getDesignation());
                }
                Employee emp = employeeRepo.save(existingEmployee);
                if (!ObjectUtils.isEmpty(emp)) {
                    Map<String, Object> employeeMap = objectMapper.convertValue(emp, new TypeReference<Map<String, Object>>() {
                    });
                    indexService.updateEntity(ES_INDEX_NAME, "_doc", id, employeeMap);
                }
                createSuccessResponse(response);
            } else {
                createErrorResponse(response, "Employee not found with ID: " + id, HttpStatus.INTERNAL_SERVER_ERROR, "FAILED");
            }
        } catch (Exception e) {
            createErrorResponse(response, null, HttpStatus.INTERNAL_SERVER_ERROR, "FAILED");
        }
        return response;
    }

    @Override
    public String deleteEmployee(int id) {
        try {
            employeeRepo.deleteById(id);
            return "Employee details deleted successfully.";
        } catch (Exception e) {
            return "Error deleting employee: " + e.getMessage();
        }
    }

    public void createSuccessResponse(Response response) {
        response.setParams(new RespParam());
        response.setTs(LocalDate.now().toString());
        response.getParams().setStatus("SUCCESS");
        response.setResponseCode(HttpStatus.OK);
    }

    public void createErrorResponse(Response response, String errorMessage, HttpStatus httpStatus, String status) {
        response.setParams(new RespParam());
        response.setTs(LocalDate.now().toString());
        //if some functionality fail then set to err only
        response.getParams().setErrmsg(errorMessage);
        response.getParams().setStatus(status);
        response.setResponseCode(httpStatus);
    }

    private String validateEmployee(Employee employee) {
        StringBuilder str = new StringBuilder();
        List<String> errList = new ArrayList<String>();
        if (StringUtils.isEmpty(employee.getName())) {
            errList.add("name");
        }
        if (employee.getAge() == 0) {
            errList.add("age");
        }
        if (StringUtils.isEmpty(employee.getDesignation())) {
            errList.add("designation");
        }
        if (!errList.isEmpty()) {
            str.append("Failed to Register Employee. Missing Params - [").append(errList).append("]");
        }
        if (employee.getAge() != 0) {
            if (employee.getAge() <= 16 || employee.getAge() >= 60) {
                str.setLength(0);
                str.append("Sorry ! You are not eligible for register as employee | Make sure your age is between 18 to 60");
            }
        }
        return str.toString();
    }
}

