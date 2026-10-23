package com.shopsphere.inventory.exception;

public class InvalidInventoryQuantityException extends RuntimeException {

    public InvalidInventoryQuantityException(String message) {
        super(message);
    }
}
