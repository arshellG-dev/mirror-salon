package com.mirror.salon.catalogue;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "PRODUCT_IMAGE")
@IdClass(ProductImageId.class)
@Getter
@Setter
@NoArgsConstructor
public class ProductImage {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Id
    @Column(name = "image_url", length = 255)
    private String imageUrl;

    public ProductImage(Product product, String imageUrl) {
        this.product = product;
        this.imageUrl = imageUrl;
    }
}
