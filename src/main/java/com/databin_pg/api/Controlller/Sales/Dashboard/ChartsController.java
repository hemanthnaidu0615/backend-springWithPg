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

    @GetMapping("/details-grid/aww")
    public ResponseEntity<?> getDetailsGridAww(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) String fulfilmentChannel,
            @RequestParam(required = false) String enterpriseKey,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "order_date") String sortField,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestParam(required = false) String search,
            @RequestParam Map<String, String> allParams
    ) {
        return getDetailsGrid(startDate, endDate, "AWW", fulfilmentChannel, enterpriseKey, page, size, sortField, sortOrder, search, allParams);
    }

    @GetMapping("/details-grid/awd")
    public ResponseEntity<?> getDetailsGridAwd(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) String fulfilmentChannel,
            @RequestParam(required = false) String enterpriseKey,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "order_date") String sortField,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestParam(required = false) String search,
            @RequestParam Map<String, String> allParams
    ) {
        return getDetailsGrid(startDate, endDate, "AWD", fulfilmentChannel, enterpriseKey, page, size, sortField, sortOrder, search, allParams);
    }

    private ResponseEntity<?> getDetailsGrid(
            String startDateStr,
            String endDateStr,
            String enterpriseKeyFilter,
            String fulfilmentChannel,
            String enterpriseKeyParam,
            int page,
            int size,
            String sortField,
            String sortOrder,
            String search,
            Map<String, String> allParams
    ) {
        try {
            LocalDate startDate = LocalDate.parse(startDateStr.substring(0, 10), INPUT_DATE_FORMAT);
            LocalDate endDate = LocalDate.parse(endDateStr.substring(0, 10), INPUT_DATE_FORMAT);
            sortOrder = sortOrder.equalsIgnoreCase("asc") ? "asc" : "desc";

            // Allowed sort fields
            Map<String, String> allowedSort = Map.of(
                "order_date", "o.order_date",
                "fulfilment_channel", "ofe.fulfilment_channel",
                "enterprise_key", "o.enterprise_key",
                "quantity", "o.quantity",
                "unit_price", "o.unit_price",
                "subtotal", "o.subtotal",
                "shipping_fee", "o.shipping_fee",
                "tax_amount", "o.tax_amount",
                "discount_amount", "o.discount_amount",
                "total_amount", "o.total_amount"
            );
            String sortCol = allowedSort.getOrDefault(sortField, "o.order_date");

            // Filterable fields
            List<String> filterFields = List.of(
                "order_date", "fulfilment_channel", "enterprise_key", "quantity", "unit_price",
                "subtotal", "shipping_fee", "tax_amount", "discount_amount", "total_amount"
            );

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
                    .append("AND o.enterprise_key = '").append(enterpriseKeyFilter).append("' ");

            if (enterpriseKeyParam != null && !enterpriseKeyParam.isBlank()) {
                sqlBuilder.append("AND o.enterprise_key = '").append(enterpriseKeyParam).append("' ");
            }

            if (fulfilmentChannel != null && !fulfilmentChannel.isBlank()) {
                sqlBuilder.append("AND ofe.fulfilment_channel = '").append(fulfilmentChannel).append("' ");
            }

            if (search != null && !search.isBlank()) {
                String searchLower = search.toLowerCase();
                sqlBuilder.append("AND (LOWER(ofe.fulfilment_channel) LIKE '%").append(searchLower).append("%' ")
                        .append("OR LOWER(o.enterprise_key) LIKE '%").append(searchLower).append("%') ");
            }

            // Apply matchMode + value filters
            for (String field : filterFields) {
                String val = allParams.get(field + ".value");
                String mode = allParams.getOrDefault(field + ".matchMode", "contains");

                if (val != null && !val.isEmpty()) {
                    String safeVal = val.toLowerCase().replace("'", "''");
                    String condition;
                    String column = (field.equals("fulfilment_channel") ? "ofe." : "o.") + field;

                    switch (mode) {
                        case "startsWith":
                            condition = "LOWER(" + column + "::text) LIKE '" + safeVal + "%'";
                            break;
                        case "endsWith":
                            condition = "LOWER(" + column + "::text) LIKE '%" + safeVal + "'";
                            break;
                        case "notContains":
                            condition = "LOWER(" + column + "::text) NOT LIKE '%" + safeVal + "%'";
                            break;
                        case "equals":
                            condition = "LOWER(" + column + "::text) = '" + safeVal + "'";
                            break;
                        default:
                            condition = "LOWER(" + column + "::text) LIKE '%" + safeVal + "%'";
                    }
                    sqlBuilder.append(" AND ").append(condition);
                }
            }

            // Count query
            String countSql = "SELECT COUNT(*) FROM (" + sqlBuilder + ") AS count_query";
            int total = ((Number) postgresService.query(countSql).get(0).get("count")).intValue();

            // Final query with pagination
            sqlBuilder.append(" ORDER BY ").append(sortCol).append(" ").append(sortOrder);
            sqlBuilder.append(" LIMIT ").append(size).append(" OFFSET ").append(page * size);

            List<Map<String, Object>> result = postgresService.query(sqlBuilder.toString());

            if (result.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .body(Map.of("message", "No order details found."));
            }

            return ResponseEntity.ok(Map.of(
                "page", page,
                "size", size,
                "count", total,
                "totalPages", (int) Math.ceil((double) total / size),
                "data", result
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch order details grid", "details", e.getMessage()));
        }
    }

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
