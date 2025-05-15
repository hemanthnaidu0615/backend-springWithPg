package com.databin_pg.api.Controlller.Sales.Dashboard;

import com.databin_pg.api.Service.PostgresService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.*;

@RestController
@RequestMapping("/api/sales/charts")
@CrossOrigin(origins = "http://localhost:5173")
public class ChartsController {

    @Autowired
    private PostgresService postgresService;

    // API for enterpriseKey AWW
    @GetMapping("/aww")
    public ResponseEntity<?> getOrderAmountByChannelAww(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate) {
        return callStoredProcedure("get_order_amount_by_fulfilment_channel_aww", startDate, endDate);
    }

    // API for enterpriseKey AWD
    @GetMapping("/awd")
    public ResponseEntity<?> getOrderAmountByChannelAwd(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate) {
        return callStoredProcedure("get_order_amount_by_fulfilment_channel_awd", startDate, endDate);
    }

    private ResponseEntity<?> callStoredProcedure(String procName, String startDate, String endDate) {
        try {
            String query = String.format(
                "SELECT * FROM %s('%s'::timestamp, '%s'::timestamp)",
                procName, startDate, endDate);

            List<Map<String, Object>> result = postgresService.query(query);

            if (result.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .body(Map.of("message", "No order data found."));
            }
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch order amounts", "details", e.getMessage()));
        }
    }
}

