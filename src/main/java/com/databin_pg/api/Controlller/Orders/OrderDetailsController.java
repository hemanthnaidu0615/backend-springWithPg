package com.databin_pg.api.Controlller.Orders;

import com.databin_pg.api.Service.PostgresService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.*;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
@Tag(name = "Orders - Details", description = "APIs to retrieve detailed order information")
public class OrderDetailsController {

    @Autowired
    private PostgresService postgresService;

    @Operation(
        summary = "Get detailed order data by order ID",
        description = "Fetches complete order details including customer, products, and fulfillment info as a JSON object."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Order details retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Order not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error while fetching order details")
    })
    @GetMapping("/{orderId}/details")
    public ResponseEntity<?> getOrderDetails(
        @Parameter(description = "Unique identifier for the order") @PathVariable("orderId") int orderId) {

        try {
            String query = String.format("SELECT get_order_details(%d);", orderId);
            List<Map<String, Object>> result = postgresService.query(query);

            if (result.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Collections.singletonMap("message", "Order not found"));
            }

            // The result is in the form of a single JSON column
            return ResponseEntity.ok(result.get(0).get("get_order_details"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Failed to fetch order details"));
        }
    }
}
