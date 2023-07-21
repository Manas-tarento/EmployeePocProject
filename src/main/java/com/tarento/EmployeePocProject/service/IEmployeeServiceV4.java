package com.tarento.EmployeePocProject.service;

import com.tarento.EmployeePocProject.entity.EmployeeV2;
import com.tarento.EmployeePocProject.model.Response;

public interface IEmployeeServiceV4 {

    Response saveEmployee(EmployeeV2 employee);

    Response updateEmployee(Integer id, EmployeeV2 updatedEmployee);

    Response getEmployeeById(int id);

}
