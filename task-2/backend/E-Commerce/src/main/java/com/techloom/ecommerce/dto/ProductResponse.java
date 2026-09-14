package com.techloom.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {
    private Long id;
    private String name;
    private String category;
    private String description;
    private Double price;
    private Integer stock;
    private Boolean active;
    private String imageUrl;
}
