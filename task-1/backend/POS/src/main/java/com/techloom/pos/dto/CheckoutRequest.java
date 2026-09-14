package com.techloom.pos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutRequest {

    @NotBlank(message = "Customer name is required")
    private String customerName;

    private String cartId;

    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    private List<CheckoutItemRequest> items;
}
