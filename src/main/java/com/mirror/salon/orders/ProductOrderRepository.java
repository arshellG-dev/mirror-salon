package com.mirror.salon.orders;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductOrderRepository extends JpaRepository<ProductOrder, Integer> {

    List<ProductOrder> findByCustomer_CustomerIdOrderByOrderDateDesc(Integer customerId);

    @Query("SELECT DISTINCT o FROM ProductOrder o "
            + "LEFT JOIN FETCH o.items oi LEFT JOIN FETCH oi.product "
            + "LEFT JOIN FETCH o.area "
            + "WHERE o.customer.customerId = :customerId ORDER BY o.orderDate DESC")
    List<ProductOrder> findByCustomerIdWithDetails(@Param("customerId") Integer customerId);

    @Query("SELECT DISTINCT o FROM ProductOrder o "
            + "LEFT JOIN FETCH o.items oi LEFT JOIN FETCH oi.product "
            + "LEFT JOIN FETCH o.area LEFT JOIN FETCH o.customer "
            + "WHERE o.orderId = :orderId")
    Optional<ProductOrder> findByIdWithDetails(@Param("orderId") Integer orderId);
}
