package com.tarento.EmployeePocProject.repo;

import com.tarento.EmployeePocProject.entity.EmployeeV2;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface EmployeeRepoV2 extends JpaRepository<EmployeeV2, Integer> {

}
