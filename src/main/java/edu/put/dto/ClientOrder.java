package edu.put.dto;

import com.datastax.driver.mapping.annotations.Table;
import lombok.ToString;

import java.util.Date;

@ToString
@Table(name = "client_orders")
public class ClientOrder {
    Integer client_id;
    Date creation_time;
    Integer price;
    String food;
}
