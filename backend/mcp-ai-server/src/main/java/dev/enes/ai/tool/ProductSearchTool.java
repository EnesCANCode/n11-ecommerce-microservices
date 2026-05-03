package dev.enes.ai.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class ProductSearchTool {

    private final RestClient restClient;

    public ProductSearchTool(RestClient.Builder builder) {
        this.restClient = builder
                .baseUrl("http://localhost:8082")
                .build();
    }

    @Tool(description = "Search products by keyword in the N11 marketplace")
    public String searchProducts(@ToolParam(description = "Search keyword") String query) {
        return restClient.get()
                .uri("/api/v1/products/search?q={q}&page=0&size=10", query)
                .retrieve()
                .body(String.class);
    }

    @Tool(description = "Get product details by ID")
    public String getProduct(@ToolParam(description = "Product UUID") String productId) {
        return restClient.get()
                .uri("/api/v1/products/{id}", productId)
                .retrieve()
                .body(String.class);
    }

    @Tool(description = "List all product categories")
    public String getCategories() {
        return restClient.get()
                .uri("/api/v1/categories")
                .retrieve()
                .body(String.class);
    }

    @Tool(description = "Get products by category ID")
    public String getProductsByCategory(@ToolParam(description = "Category UUID") String categoryId) {
        return restClient.get()
                .uri("/api/v1/products/category/{id}?page=0&size=20", categoryId)
                .retrieve()
                .body(String.class);
    }
}
