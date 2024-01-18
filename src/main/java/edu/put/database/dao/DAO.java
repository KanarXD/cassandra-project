package edu.put.database.dao;

import com.datastax.oss.driver.api.mapper.annotations.DaoFactory;
import com.datastax.oss.driver.api.mapper.annotations.Mapper;

@Mapper
public interface DAO {
    @DaoFactory
    Restaurants restaurants();

    @DaoFactory
    Orders orders();

    @DaoFactory
    Ready ready();

    @DaoFactory
    ConfirmDelivery confirm_delivery();

    @DaoFactory
    ConfirmOrder confirm_order();

    @DaoFactory
    Delivery delivery();
}
