package com.tarento.EmployeePocProject.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SearchCriteria {

    private Map<String, Object> filters;

    private List<String> fields;

    private List<String> facet;

    private String pageNo;

    private String pageSize;
}
