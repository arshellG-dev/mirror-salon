package com.mirror.salon.orders;

import com.mirror.salon.catalogue.DeliveryArea;
import com.mirror.salon.users.Customer;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "PRODUCT_ORDER")
@Getter
@Setter
@NoArgsConstructor
public class ProductOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Integer orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id", nullable = false)
    private DeliveryArea area;

    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate;

    @Column(name = "recipient_name", length = 100, nullable = false)
    private String recipientName;

    @Column(name = "contact_no", nullable = false, columnDefinition = "CHAR(10)")
    private String contactNo;

    @Column(name = "address_line", length = 200, nullable = false)
    private String addressLine;

    @Column(name = "city", length = 60, nullable = false)
    private String city;

    @Column(name = "delivery_fee", nullable = false, precision = 8, scale = 2)
    private BigDecimal deliveryFee;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false)
    private OrderStatus orderStatus = OrderStatus.PLACED;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    public enum OrderStatus { PLACED, PAYMENT_PENDING, CONFIRMED, DISPATCHED, DELIVERED, REJECTED }
}
