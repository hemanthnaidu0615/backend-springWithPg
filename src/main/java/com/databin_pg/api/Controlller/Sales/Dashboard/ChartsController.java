package com.databin_pg.api.Controlller.Sales.Dashboard;

import com.databin_pg.api.Service.PostgresService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/sales/charts")
@CrossOrigin(origins = "*")
public class ChartsController {

    @Autowired
    private PostgresService postgresService;

    private static final DateTimeFormatter INPUT_DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    // Existing endpoints...
    @GetMapping("/aww")
    public ResponseEntity<?> getOrderAmountByChannelAww(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate) {
        return callStoredProcedure("get_order_amount_by_fulfilment_channel_aww", startDate, endDate);
    }

    @GetMapping("/awd")
    public ResponseEntity<?> getOrderAmountByChannelAwd(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate) {
        return callStoredProcedure("get_order_amount_by_fulfilment_channel_awd", startDate, endDate);
    }

    // New detailed grid endpoints for AWD and AWW with pagination, filtering, sorting, searching
    @GetMapping("/details-grid/aww")
    public ResponseEntity<?> getDetailsGridAww(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) String fulfilmentChannel,
            @RequestParam(required = false) String enterpriseKey,
            @RequestParam(defaultValue = "0") int page,            // 0-based page number
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "order_date") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir,
            @RequestParam(required = false) String search          // text search on fulfillment_channel or enterprise_key
    ) {
        return getDetailsGrid(startDate, endDate, "AWW", fulfilmentChannel, enterpriseKey, page, size, sortBy, sortDir, search);
    }

    @GetMapping("/details-grid/awd")
    public ResponseEntity<?> getDetailsGridAwd(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) String fulfilmentChannel,
            @RequestParam(required = false) String enterpriseKey,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "order_date") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir,
            @RequestParam(required = false) String search
    ) {
        return getDetailsGrid(startDate, endDate, "AWD", fulfilmentChannel, enterpriseKey, page, size, sortBy, sortDir, search);
    }

    private ResponseEntity<?> getDetailsGrid(
            String startDateStr,
            String endDateStr,
            String enterpriseKeyFilter,
            String fulfilmentChannel,
            String enterpriseKeyParam,
            int page,
            int size,
            String sortBy,
            String sortDir,
            String search
    ) {
        try {
            // Parse dates
            LocalDate startDate = LocalDate.parse(startDateStr.substring(0, 10), INPUT_DATE_FORMAT);
            LocalDate endDate = LocalDate.parse(endDateStr.substring(0, 10), INPUT_DATE_FORMAT);

            // Validate sort direction
            sortDir = sortDir.equalsIgnoreCase("ASC") ? "ASC" : "DESC";

            // Build base SQL query with joins
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("SELECT ")
                    .append("o.order_date, ")
                    .append("ofe.fulfilment_channel, ")
                    .append("o.enterprise_key, ")
                    .append("o.quantity, ")
                    .append("o.unit_price, ")
                    .append("o.subtotal, ")
                    .append("o.shipping_fee, ")
                    .append("o.tax_amount, ")
                    .append("o.discount_amount, ")
                    .append("o.total_amount ")
                    .append("FROM orders o ")
                    .append("JOIN order_fulfillment_event ofe ON o.order_id = ofe.order_id ")
                    .append("WHERE o.order_date::DATE BETWEEN '").append(startDate).append("' AND '").append(endDate).append("' ")
                    // enterpriseKey fixed filter for AWW/AWD
                    .append("AND o.enterprise_key = '").append(enterpriseKeyFilter).append("' ");

            // Optional enterpriseKey param filter (overrides enterpriseKeyFilter if provided)
            if (enterpriseKeyParam != null && !enterpriseKeyParam.isBlank()) {
                sqlBuilder.append("AND o.enterprise_key = '").append(enterpriseKeyParam).append("' ");
            }

            // Optional fulfilmentChannel filter
            if (fulfilmentChannel != null && !fulfilmentChannel.isBlank()) {
                sqlBuilder.append("AND ofe.fulfilment_channel = '").append(fulfilmentChannel).append("' ");
            }

            // Optional search on fulfillment_channel or enterprise_key
            if (search != null && !search.isBlank()) {
                String searchLower = search.toLowerCase();
                sqlBuilder.append("AND (LOWER(ofe.fulfilment_channel) LIKE '%").append(searchLower).append("%' ")
                        .append("OR LOWER(o.enterprise_key) LIKE '%").append(searchLower).append("%') ");
            }

            // Count total records for pagination metadata
            String countSql = "SELECT COUNT(*) FROM (" + sqlBuilder.toString() + ") AS count_query";

            List<Map<String, Object>> countResult = postgresService.query(countSql);
            int total = 0;
            if (!countResult.isEmpty()) {
                total = ((Number) countResult.get(0).get("count")).intValue();
            }

            // Add ORDER BY and LIMIT OFFSET for pagination
            sqlBuilder.append("ORDER BY ").append(sortBy).append(" ").append(sortDir).append(" ");
            sqlBuilder.append("LIMIT ").append(size).append(" OFFSET ").append(page * size);

            List<Map<String, Object>> result = postgresService.query(sqlBuilder.toString());

            if (result.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .body(Map.of("message", "No order details found."));
            }

            // Return paged response with metadata
            Map<String, Object> response = new HashMap<>();
            response.put("startDate", startDate);
            response.put("endDate", endDate);
            response.put("page", page);
            response.put("size", size);
            response.put( "count", total);
            response.put("totalPages", (int) Math.ceil((double) total / size));
            response.put("data", result);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch order details grid", "details", e.getMessage()));
        }
    }

    // Your existing callStoredProcedure method unchanged
    private ResponseEntity<?> callStoredProcedure(String procName, String startDate, String endDate) {
        try {
            LocalDate start = LocalDate.parse(startDate.substring(0, 10), INPUT_DATE_FORMAT);
            LocalDate end = LocalDate.parse(endDate.substring(0, 10), INPUT_DATE_FORMAT);

            String query = String.format(
                "SELECT * FROM %s('%s'::timestamp, '%s'::timestamp)",
                procName, start, end);

            List<Map<String, Object>> result = postgresService.query(query);

            if (result.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .body(Map.of("message", "No order data found."));
            }

            return ResponseEntity.ok(Map.of(
                "startDate", start,
                "endDate", end,
                "data", result
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch order amounts", "details", e.getMessage()));
        }
    }
}
