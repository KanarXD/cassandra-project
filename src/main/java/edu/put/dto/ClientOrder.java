package edu.put.dto;

import com.datastax.driver.mapping.annotations.Table;
import lombok.Data;

import java.util.Date;

@Data
@Table(name = "client_orders")
public class ClientOrder {
    private String foodCategory;
    private Date creationTime;
    private String orderId;
    private Integer userId;
    private String food;
}
