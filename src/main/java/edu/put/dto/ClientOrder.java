package edu.put.dto;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "client_orders")
public class ClientOrder {
    @Column(name = "food_category")
    private String category;
    @Column(name = "creation_time")
    private Date creationTime;
    @Column(name = "order_id")
    private String orderId;
    @Column(name = "user_id")
    private Integer userId;
    private String food;
}
