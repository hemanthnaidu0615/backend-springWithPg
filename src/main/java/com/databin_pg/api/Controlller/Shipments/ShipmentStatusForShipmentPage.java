package com.databin_pg.api.Controlller.Shipments;


import com.databin_pg.api.Service.PostgresService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shipment-status")
@CrossOrigin(origins = "*")
public class ShipmentStatusForShipmentPage {

    @Autowired
    private PostgresService postgresService;

    @GetMapping("/distribution")
    public ResponseEntity<?> getShipmentStatusDistribution(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate,
            @RequestParam(name = "carrier", required = false) String carrier,
            @RequestParam(name = "shippingMethod", required = false) String shippingMethod,
            @RequestParam(name = "enterpriseKey", required = false) String enterpriseKey
    ) {
        try {
            String query = String.format("""
                SELECT * FROM get_shipment_status_distribution(
                    '%s'::TIMESTAMP,
                    '%s'::TIMESTAMP,
                    %s,
                    %s,
                    %s
                )
            """,
                startDate,
                endDate,
                carrier != null ? "'" + carrier + "'" : "NULL",
                shippingMethod != null ? "'" + shippingMethod + "'" : "NULL",
                enterpriseKey != null ? "'" + enterpriseKey + "'" : "NULL"
            );

            List<Map<String, Object>> data = postgresService.query(query);

            if (data.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "delivered", "0.0%",
                    "in_transit", "0.0%",
                    "delayed", "0.0%",
                    "cancelled", "0.0%"
                ));
            }

            Map<String, Object> row = data.get(0);

            return ResponseEntity.ok(Map.of(
                "delivered", row.get("delivered"),
                "in_transit", row.get("in_transit"),
                "delayed", row.get("delayed"),
                "cancelled", row.get("cancelled")
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch shipment status distribution"));
        }
    }
    
    @GetMapping("/details-grid")
    public ResponseEntity<?> getShipmentDetailsGrid(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "carrier", required = false) String carrier,
            @RequestParam(name = "shippingMethod", required = false) String shippingMethod,
            @RequestParam(name = "enterpriseKey", required = false) String enterpriseKey,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sortField", defaultValue = "shipment_id") String sortField,
            @RequestParam(name = "sortOrder", defaultValue = "asc") String sortOrder,
            @RequestParam Map<String, String> allParams
    ) {
        try {
            int offset = page * size;

            // Allowlist of valid sortable fields
            Map<String, String> allowedSortFields = Map.of(
                    "shipment_id", "shipment_id",
                    "order_id", "order_id",
                    "carrier", "carrier",
                    "tracking_number", "tracking_number",
                    "shipment_status", "shipment_status",
                    "shipment_cost", "shipment_cost",
                    "shipping_method", "shipping_method",
                    "estimated_shipment_date", "estimated_shipment_date",
                    "actual_shipment_date", "actual_shipment_date"
            );

            // Allowlist of searchable fields
            List<String> filterableFields = List.of(
                    "shipment_id", "order_id", "carrier", "tracking_number", "shipment_status","shipment_cost" , "shipping_method", "estimated_shipment_date","actual_shipment_date" 
            );

            String sortColumn = allowedSortFields.getOrDefault(sortField, "shipment_id");
            String sortDirection = sortOrder.equalsIgnoreCase("desc") ? "DESC" : "ASC";

            StringBuilder whereClause = new StringBuilder("WHERE 1=1");
            for (String field : filterableFields) {
                String value = allParams.get(field + ".value");
                String matchMode = allParams.getOrDefault(field + ".matchMode", "contains");

                if (value != null && !value.isEmpty()) {
                    value = value.toLowerCase().replace("'", "''");
                    String condition;
                    switch (matchMode) {
                        case "startsWith" -> condition = "LOWER(" + field + "::text) LIKE '" + value + "%'";
                        case "endsWith" -> condition = "LOWER(" + field + "::text) LIKE '%" + value + "'";
                        case "notContains" -> condition = "LOWER(" + field + "::text) NOT LIKE '%" + value + "%'";
                        case "equals" -> condition = "LOWER(" + field + "::text) = '" + value + "'";
                        default -> condition = "LOWER(" + field + "::text) LIKE '%" + value + "%'";
                    }
                    whereClause.append(" AND ").append(condition);
                }
            }

            String baseQuery = String.format("""
                SELECT * FROM get_shipment_status_details(
                    '%s'::TIMESTAMP,
                    '%s'::TIMESTAMP,
                    %s,
                    %s,
                    %s,
                    %s
                )
            """,
                    startDate,
                    endDate,
                    status != null ? "'" + status + "'" : "NULL",
                    carrier != null ? "'" + carrier + "'" : "NULL",
                    shippingMethod != null ? "'" + shippingMethod + "'" : "NULL",
                    enterpriseKey != null ? "'" + enterpriseKey + "'" : "NULL"
            );

            String countQuery = String.format("SELECT COUNT(*) AS total FROM (%s) AS result %s",
                    baseQuery, whereClause);

            int totalCount = ((Number) postgresService.query(countQuery).get(0).get("total")).intValue();

            String dataQuery = String.format("""
                SELECT * FROM (%s) AS result
                %s
                ORDER BY %s %s
                OFFSET %d LIMIT %d
            """,
                    baseQuery,
                    whereClause,
                    sortColumn,
                    sortDirection,
                    offset,
                    size
            );

            List<Map<String, Object>> result = postgresService.query(dataQuery);

            return ResponseEntity.ok(Map.of(
                    "data", result,
                    "page", page,
                    "size", size,
                    "count", totalCount
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch shipment details", "details", e.getMessage()));
        }
    }
}