package edu.put.dto;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.Table;
import lombok.Data;

import java.util.Date;

@Data
@Table(name = "orders_in_progress")
public class OrderInProgress {
    @Column(name = "order_id")
    private String orderId;
    @Column(name = "creation_time")
    private Date creationTime;
    private String status;
    @Column(name = "restaurant_id")
    private Integer restaurantId;
    private String info;
}
