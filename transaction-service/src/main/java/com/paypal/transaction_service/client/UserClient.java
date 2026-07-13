package com.paypal.transaction_service.client;

import com.paypal.transaction_service.dto.UserResolutionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "user-service", url = "${app.services.user-url}")
public interface UserClient {
    @GetMapping("/internal/resolve-pay-tag")
    UserResolutionResponse resolvePayTag(@RequestParam("payTag") String payTag);

    @GetMapping("/internal/{id}")
    UserResolutionResponse resolveUser(@PathVariable("id") Long id);
}
