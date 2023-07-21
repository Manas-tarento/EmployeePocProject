package com.tarento.EmployeePocProject.service;

import com.tarento.EmployeePocProject.entity.Employee;
import com.tarento.EmployeePocProject.model.Response;

public interface IEmployeeService {

    Response getAllEmployees();

    Response getEmployeeById(int id);

    Response saveEmployee(Employee employee);

    Response updateEmployee(Integer id, Employee employee);

    String deleteEmployee(int id);
}
