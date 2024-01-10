package edu.put.records;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.Table;
import lombok.Data;

@Data
@Table(name = "order_confirmation")
public class Confirmation {
    @Column(name = "order_id")
    private String orderId;
    @Column(name = "restaurant_id")
    private int restaurantId;
    @Column(name = "info")
    private String order;
}
