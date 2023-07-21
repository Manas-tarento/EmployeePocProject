package com.tarento.EmployeePocProject.entity;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "employee_v2")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class EmployeeV2 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Type(type = "jsonb")
    @Column(name = "json_data", columnDefinition = "jsonb")
    private Map<String, Object> jsonData;

    private String type;
}
