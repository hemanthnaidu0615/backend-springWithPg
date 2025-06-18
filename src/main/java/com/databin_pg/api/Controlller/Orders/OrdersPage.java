package com.databin_pg.api.Controlller.Orders;

import com.databin_pg.api.Service.PostgresService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.util.*;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
@Tag(name = "Orders - Filtered", description = "APIs for fetching filtered and paginated orders")
public class OrdersPage {

    @Autowired
    private PostgresService postgresService;

    @Operation(
        summary = "Get filtered orders with pagination",
        description = "Fetches orders filtered by date range, status, order type, payment method, carrier, customer search, order ID search, and enterprise key. Supports pagination."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Filtered orders retrieved successfully"),
        @ApiResponse(responseCode = "500", description = "Internal server error while fetching filtered orders")
    })
    @GetMapping("/filtered")
    public ResponseEntity<?> getFilteredOrders(
            @Parameter(description = "Start date for filtering orders (format: yyyy-MM-dd)", required = true)
            @RequestParam(name = "startDate") String startDate,

            @Parameter(description = "End date for filtering orders (format: yyyy-MM-dd)", required = true)
            @RequestParam(name = "endDate") String endDate,

            @Parameter(description = "Order status filter (optional)")
            @RequestParam(name = "status", required = false) String status,

            @Parameter(description = "Order type filter (optional)")
            @RequestParam(name = "orderType", required = false) String orderType,

            @Parameter(description = "Payment method filter (optional)")
            @RequestParam(name = "paymentMethod", required = false) String paymentMethod,

            @Parameter(description = "Carrier filter (optional)")
            @RequestParam(name = "carrier", required = false) String carrier,

            @Parameter(description = "Customer name search filter (optional)")
            @RequestParam(name = "searchCustomer", required = false) String searchCustomer,

            @Parameter(description = "Order ID search filter (optional)")
            @RequestParam(name = "searchOrderId", required = false) String searchOrderId,

            @Parameter(description = "Enterprise key filter (optional)")
            @RequestParam(name = "enterpriseKey", required = false) String enterpriseKey,

            @Parameter(description = "Page number for pagination (default 0)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size for pagination (default 10)")
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            int offset = page * size;

            // Build filter arguments for query
            String[] args = new String[]{
                startDate != null ? "TIMESTAMP '" + startDate + "'" : "NULL",
                endDate != null ? "TIMESTAMP '" + endDate + "'" : "NULL",
                status != null ? "'" + status + "'" : "NULL",
                orderType != null ? "'" + orderType + "'" : "NULL",
                paymentMethod != null ? "'" + paymentMethod + "'" : "NULL",
                carrier != null ? "'" + carrier + "'" : "NULL",
                searchCustomer != null ? "'" + searchCustomer + "'" : "NULL",
                searchOrderId != null ? "'" + searchOrderId + "'" : "NULL",
                enterpriseKey != null ? "'" + enterpriseKey + "'" : "NULL"
            };

            // Count query
            String countQuery = String.format("""
            	    SELECT COUNT(*) as total FROM get_filtered_orders(
            	        %s, %s, %s, %s, %s, %s, %s, %s, %s
            	    )
            	""", (Object[]) args);

            List<Map<String, Object>> countResult = postgresService.query(countQuery);
            int totalCount = 0;
            if (!countResult.isEmpty() && countResult.get(0).get("total") != null) {
                totalCount = ((Number) countResult.get(0).get("total")).intValue();
            }

            // Paginated data query
            String dataQuery = String.format("""
            	    SELECT * FROM get_filtered_orders(
            	        %s, %s, %s, %s, %s, %s, %s, %s, %s
            	    )
            	    OFFSET %d LIMIT %d
            	""", (Object[]) Arrays.copyOf(args, args.length + 2));

            List<Map<String, Object>> results = postgresService.query(dataQuery);

            return ResponseEntity.ok(Map.of(
                    "data", results,
                    "page", page,
                    "size", size,
                    "count", totalCount
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch filtered orders", "details", e.getMessage()));
        }
    }
}
