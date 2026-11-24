package com.example.giuaky.models;

import com.google.firebase.firestore.DocumentId;

import java.io.Serializable;

public class ShippingAddress implements Serializable {

    @DocumentId
    private String id;
    private String receiverName;
    private String phone;
    private String detailAddress;
    private boolean isDefault;

    // Required empty constructor for Firestore
    public ShippingAddress() {
    }

    public ShippingAddress(String id, String receiverName, String phone, String detailAddress, boolean isDefault) {
        this.id = id;
        this.receiverName = receiverName;
        this.phone = phone;
        this.detailAddress = detailAddress;
        this.isDefault = isDefault;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getDetailAddress() {
        return detailAddress;
    }

    public void setDetailAddress(String detailAddress) {
        this.detailAddress = detailAddress;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }
}
