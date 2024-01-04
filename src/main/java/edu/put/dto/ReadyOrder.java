package edu.put.dto;

import com.datastax.driver.mapping.annotations.Table;
import lombok.Data;

@Data
@Table(name = "ready_orders")
public class ReadyOrder {
    private String id;
    private String info;
}
