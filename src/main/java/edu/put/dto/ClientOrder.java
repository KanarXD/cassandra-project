package edu.put.dto;

import com.datastax.driver.mapping.annotations.Table;
import lombok.Data;

import java.util.Date;

@Data
@Table(name = "client_orders")
public class ClientOrder {
    private String food_category;
    private Date creation_time;
    private Integer user_id;
    private String food;
    private String order_id;
}
