package com.mirror.salon.catalogue;

import java.io.Serializable;
import java.util.Objects;

public class ProductImageId implements Serializable {

    private Integer product;
    private String imageUrl;

    public ProductImageId() {}

    public ProductImageId(Integer product, String imageUrl) {
        this.product = product;
        this.imageUrl = imageUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductImageId that)) return false;
        return Objects.equals(product, that.product) && Objects.equals(imageUrl, that.imageUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(product, imageUrl);
    }
}
