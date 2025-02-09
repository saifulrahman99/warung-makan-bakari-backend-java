package com.enigma.wmb_api.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentRequest {
    @JsonProperty("transaction_details")
    private PaymentDetailRequest paymentDetails;

    @JsonProperty("item_details")
    private List<PaymentItemDetailRequest> paymentItemDetails;

    @JsonProperty("enabled_payments")
    private List<String> paymentMethod;
}
