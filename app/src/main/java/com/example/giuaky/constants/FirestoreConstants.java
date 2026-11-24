package com.example.giuaky.constants;

public final class FirestoreConstants {

    private FirestoreConstants() {
        // Prevent instantiation
    }

    // Collections
    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_PRODUCTS = "products";
    public static final String COLLECTION_CATEGORIES = "categories";

    // Field keys
    public static final String FIELD_UID = "uid";
    public static final String FIELD_FULL_NAME = "fullName";
    public static final String FIELD_EMAIL = "email";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_CATEGORY_ID = "categoryId";
    public static final String FIELD_PRICE = "price";
    public static final String FIELD_IMAGE_URL = "imageUrl";
    public static final String FIELD_DESCRIPTION = "description";
}
