package com.example.giuaky.models;

import com.google.firebase.firestore.DocumentId;

import java.util.ArrayList;
import java.util.List;

public class Order {
    @DocumentId
    private String id;
    private String userId;
    private String customerName;
    private String phone;
    private String address;
    private String paymentMethod;
    private List<OrderItem> items = new ArrayList<>();
    private double totalAmount;
    private String status = "Đang xử lý";
    private long createdAt = System.currentTimeMillis();

    // Required empty constructor for Firestore
    public Order() {
    }

    public Order(String id, String userId, String customerName, String phone, String address,
                 String paymentMethod, List<OrderItem> items, double totalAmount, String status, long createdAt) {
        this.id = id;
        this.userId = userId;
        this.customerName = customerName;
        this.phone = phone;
        this.address = address;
        this.paymentMethod = paymentMethod;
        this.items = items != null ? items : new ArrayList<>();
        this.totalAmount = totalAmount;
        this.status = status != null ? status : "Đang xử lý";
        this.createdAt = createdAt > 0 ? createdAt : System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
