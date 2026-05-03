package dev.enes.apigateway.controller;

import dev.enes.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/payment")
    public Mono<ApiResponse<String>> paymentFallback() {
        return Mono.just(ApiResponse.error("Odeme servisi su anda yanit vermiyor. Lutfen daha sonra tekrar deneyiniz."));
    }

    @GetMapping("/order")
    public Mono<ApiResponse<String>> orderFallback() {
        return Mono.just(ApiResponse.error("Siparis servisi su anda yanit vermiyor. Lutfen daha sonra tekrar deneyiniz."));
    }

    @GetMapping("/product")
    public Mono<ApiResponse<String>> productFallback() {
        return Mono.just(ApiResponse.error("Urun servisi su anda yanit vermiyor. Lutfen daha sonra tekrar deneyiniz."));
    }
}