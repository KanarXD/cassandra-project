package edu.put.dto;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.Table;
import lombok.Data;

import java.util.Date;

@Data
@Table(name = "ready_orders")
public class ReadyOrder {
    private String id;
    @Column(name = "creation_time")
    private Date creationTime;
    private String info;
}
