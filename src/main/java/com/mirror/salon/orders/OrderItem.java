package com.mirror.salon.orders;

import com.mirror.salon.catalogue.Product;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "ORDER_ITEM")
@IdClass(OrderItemId.class)
@Getter
@Setter
@NoArgsConstructor
public class OrderItem {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private ProductOrder order;

    @Id
    @Column(name = "line_no", columnDefinition = "TINYINT UNSIGNED")
    private Integer lineNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "quantity", nullable = false, columnDefinition = "SMALLINT UNSIGNED")
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    public OrderItem(ProductOrder order, Integer lineNo, Product product, int quantity, BigDecimal unitPrice) {
        this.order = order;
        this.lineNo = lineNo;
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }
}
