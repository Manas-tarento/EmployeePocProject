package com.tarento.EmployeePocProject.service;

import com.tarento.EmployeePocProject.model.Response;
import com.tarento.EmployeePocProject.model.SearchCriteria;

public interface IEmployeeServiceV3 {
    Response getEmployeeFromEs(SearchCriteria searchCriteria);

}
