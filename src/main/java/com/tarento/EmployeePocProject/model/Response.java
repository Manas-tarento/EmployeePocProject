package com.tarento.EmployeePocProject.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Response {

    private String ts;
    private RespParam params;
    private HttpStatus responseCode;
    private Map<String, Object> response = new HashMap<>();

}
