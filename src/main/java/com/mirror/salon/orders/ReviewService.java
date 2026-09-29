package com.mirror.salon.orders;

import com.mirror.salon.users.Customer;
import com.mirror.salon.users.CustomerRepository;
import com.mirror.salon.users.Staff;
import com.mirror.salon.users.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final CustomerRepository customerRepository;
    private final StaffRepository staffRepository;

    public static class ReviewException extends RuntimeException {
        public ReviewException(String message) { super(message); }
    }

    @Transactional(readOnly = true)
    public List<Review> visibleReviews() {
        return reviewRepository.findVisibleWithCustomer();
    }

    @Transactional(readOnly = true)
    public BigDecimal averageRating() {
        List<Review> visible = visibleReviews();
        if (visible.isEmpty()) {
            return BigDecimal.ZERO;
        }
        double avg = visible.stream().mapToInt(Review::getRating).average().orElse(0);
        return BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP);
    }

    public Review submit(Integer customerId, int rating, String comment) {
        if (rating < 1 || rating > 5) {
            throw new ReviewException("Rating must be between 1 and 5.");
        }
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ReviewException("Customer not found."));
        Review review = new Review();
        review.setCustomer(customer);
        review.setRating(rating);
        review.setComment(comment);
        review.setReviewedAt(LocalDateTime.now());
        review.setHidden(false);
        return reviewRepository.save(review);
    }

    @Transactional(readOnly = true)
    public List<Review> allForModeration() {
        return reviewRepository.findAllWithCustomer();
    }

    public void setHidden(Integer reviewId, boolean hidden, Integer staffId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewException("Review not found."));
        review.setHidden(hidden);
        review.setModeratedBy(staffRepository.findById(staffId).orElse(null));
        reviewRepository.save(review);
    }

    public void delete(Integer reviewId) {
        reviewRepository.deleteById(reviewId);
    }

    public void reply(Integer reviewId, String replyText, Integer staffId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewException("Review not found."));
        Staff staff = staffRepository.findById(staffId).orElseThrow();
        review.setReplyText(replyText);
        review.setRepliedAt(LocalDateTime.now());
        review.setModeratedBy(staff);
        reviewRepository.save(review);
    }
}
