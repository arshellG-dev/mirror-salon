package com.mirror.salon.orders;

import com.mirror.salon.catalogue.DeliveryArea;
import com.mirror.salon.catalogue.DeliveryAreaRepository;
import com.mirror.salon.catalogue.Product;
import com.mirror.salon.catalogue.ProductRepository;
import com.mirror.salon.users.Customer;
import com.mirror.salon.users.CustomerRepository;
import com.mirror.salon.users.Notification;
import com.mirror.salon.users.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderCheckoutService {

    private final ProductRepository productRepository;
    private final DeliveryAreaRepository deliveryAreaRepository;
    private final CustomerRepository customerRepository;
    private final ProductOrderRepository productOrderRepository;
    private final PaymentRepository paymentRepository;
    private final BankTransferPaymentRepository bankTransferPaymentRepository;
    private final CodPaymentRepository codPaymentRepository;
    private final FileStorageService fileStorageService;
    private final NotificationRepository notificationRepository;

    private void queueOrderPlaced(ProductOrder order) {
        Notification notification = new Notification();
        notification.setCustomerId(order.getCustomer().getCustomerId());
        notification.setOrderId(order.getOrderId());
        notification.setNotifType(Notification.NotifType.ORDER_PLACED);
        notification.setChannel(Notification.Channel.SMS);
        notification.setMessage("Your order #" + order.getOrderId() + " has been placed. Thank you!");
        notification.setScheduledAt(java.time.LocalDateTime.now());
        notificationRepository.save(notification);
    }

    public static class CheckoutException extends RuntimeException {
        public CheckoutException(String message) { super(message); }
    }

    public record CheckoutForm(Integer customerId, Map<Integer, Integer> cartItems, Integer areaId,
                                String recipientName, String contactNo, String addressLine, String city) {}

    @Transactional
    public ProductOrder placeCodOrder(CheckoutForm form) {
        ProductOrder order = buildOrder(form);
        productOrderRepository.save(order);

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentMethod(Payment.PaymentMethod.COD);
        payment.setAmount(orderTotal(order));
        payment.setPaymentStatus(Payment.PaymentStatus.PENDING);
        paymentRepository.save(payment);

        CodPayment cod = new CodPayment();
        cod.setPayment(payment);
        codPaymentRepository.save(cod);

        queueOrderPlaced(order);
        return order;
    }

    @Transactional
    public ProductOrder placeBankTransferOrder(CheckoutForm form, MultipartFile slip, String bankReference) {
        if (slip == null || slip.isEmpty()) {
            throw new CheckoutException("Please upload your deposit slip.");
        }
        ProductOrder order = buildOrder(form);
        order.setOrderStatus(ProductOrder.OrderStatus.PAYMENT_PENDING);
        productOrderRepository.save(order);

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentMethod(Payment.PaymentMethod.BANK_TRANSFER);
        payment.setAmount(orderTotal(order));
        payment.setPaymentStatus(Payment.PaymentStatus.PENDING);
        paymentRepository.save(payment);

        String slipPath = fileStorageService.storeSlip(slip);
        BankTransferPayment bt = new BankTransferPayment();
        bt.setPayment(payment);
        bt.setSlipFilePath(slipPath);
        bt.setBankReference(bankReference);
        bt.setUploadedAt(LocalDateTime.now());
        bankTransferPaymentRepository.save(bt);

        queueOrderPlaced(order);
        return order;
    }

    private ProductOrder buildOrder(CheckoutForm form) {
        if (form.cartItems().isEmpty()) {
            throw new CheckoutException("Your cart is empty.");
        }
        Customer customer = customerRepository.findById(form.customerId())
                .orElseThrow(() -> new CheckoutException("Customer not found."));
        DeliveryArea area = deliveryAreaRepository.findById(form.areaId())
                .orElseThrow(() -> new CheckoutException("Please select a delivery area."));

        ProductOrder order = new ProductOrder();
        order.setCustomer(customer);
        order.setArea(area);
        order.setOrderDate(LocalDateTime.now());
        order.setRecipientName(form.recipientName());
        order.setContactNo(form.contactNo());
        order.setAddressLine(form.addressLine());
        order.setCity(form.city());
        order.setDeliveryFee(area.getDeliveryFee());
        order.setOrderStatus(ProductOrder.OrderStatus.PLACED);

        int lineNo = 1;
        for (Map.Entry<Integer, Integer> entry : form.cartItems().entrySet()) {
            Product product = productRepository.findById(entry.getKey())
                    .orElseThrow(() -> new CheckoutException("A product in your cart is no longer available."));
            if (entry.getValue() > product.getStockQty()) {
                throw new CheckoutException("Not enough stock for " + product.getProductName() + ".");
            }
            OrderItem item = new OrderItem(order, lineNo++, product, entry.getValue(), product.getPrice());
            order.getItems().add(item);
        }
        return order;
    }

    private BigDecimal orderTotal(ProductOrder order) {
        BigDecimal itemsTotal = order.getItems().stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return itemsTotal.add(order.getDeliveryFee());
    }
}
