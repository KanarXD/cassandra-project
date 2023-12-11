package edu.put.dto;

import com.datastax.driver.mapping.annotations.Table;
import lombok.Data;

import java.util.Date;

@Data
@Table(name = "orders_in_progress")
public class OrderInProgress {
    private String orderId;
    private Date creationTime;
    private String status;
    private Integer restaurantId;
    private String info;
}
