package com.shopsphere.inventory.dto;

import java.util.UUID;

public class ProductDto {

    private UUID id;
    private String name;
    private Boolean active;

    public ProductDto() {
    }

    public ProductDto(UUID id, String name, Boolean active) {
        this.id = id;
        this.name = name;
        this.active = active;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
