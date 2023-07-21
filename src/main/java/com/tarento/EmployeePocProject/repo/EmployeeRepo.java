package com.tarento.EmployeePocProject.repo;

import com.tarento.EmployeePocProject.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface EmployeeRepo extends JpaRepository<Employee, Integer> {

}
