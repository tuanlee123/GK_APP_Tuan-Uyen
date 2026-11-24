package com.example.giuaky.models;

import com.google.firebase.firestore.DocumentId;
import java.io.Serializable;

public class Product implements Serializable {
    @DocumentId
    private String id;
    private String name;
    private double price;
    private String imageUrl;
    private String categoryId;
    private String description;
    private String specifications;
    private int soldCount = 0;
    private double rating = 5.0;
    private int reviewCount = 0;

    // Required empty constructor for Firestore
    public Product() {
    }

    public Product(String id, String name, double price, String imageUrl, String categoryId, String description) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
        this.categoryId = categoryId;
        this.description = description;
        this.specifications = "";
        this.soldCount = 0;
        this.rating = 5.0;
        this.reviewCount = 0;
    }

    public Product(String id, String name, double price, String imageUrl, String categoryId, String description, int soldCount, double rating) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
        this.categoryId = categoryId;
        this.description = description;
        this.specifications = "";
        this.soldCount = soldCount;
        this.rating = rating;
        this.reviewCount = 0;
    }

    public Product(String id, String name, double price, String imageUrl, String categoryId, String description, String specifications, int soldCount, double rating, int reviewCount) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
        this.categoryId = categoryId;
        this.description = description;
        this.specifications = specifications;
        this.soldCount = soldCount;
        this.rating = rating;
        this.reviewCount = reviewCount;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSpecifications() {
        return specifications;
    }

    public void setSpecifications(String specifications) {
        this.specifications = specifications;
    }

    public int getSoldCount() {
        return soldCount;
    }

    public void setSoldCount(int soldCount) {
        this.soldCount = soldCount;
    }

    public double getRating() {
        return rating <= 0 ? 5.0 : rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(int reviewCount) {
        this.reviewCount = reviewCount;
    }
}
