package com.databin_pg.api.Controlller.Orders;

import com.databin_pg.api.Service.PostgresService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ExportReportController {

    @Autowired
    private PostgresService postgresService;

    @GetMapping("/orders/excel")
    public ResponseEntity<byte[]> exportOrdersToExcel(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "orderType", required = false) String orderType,
            @RequestParam(name = "paymentMethod", required = false) String paymentMethod,
            @RequestParam(name = "carrier", required = false) String carrier,
            @RequestParam(name = "searchCustomer", required = false) String searchCustomer,
            @RequestParam(name = "searchOrderId", required = false) String searchOrderId,
            @RequestParam(name = "enterpriseKey", required=false) String enterpriseKey
    ) {
        try {
            // Construct the SQL query using the stored procedure
            String query = String.format("""
                SELECT * FROM get_filtered_orders(
                    %s, %s, %s, %s, %s, %s, %s, %s, %s
                )
            """,
                startDate != null ? "TIMESTAMP '" + startDate + "'" : "NULL",
                endDate != null ? "TIMESTAMP '" + endDate + "'" : "NULL",
                status != null ? "'" + status + "'" : "NULL",
                orderType != null ? "'" + orderType + "'" : "NULL",
                paymentMethod != null ? "'" + paymentMethod + "'" : "NULL",
                carrier != null ? "'" + carrier + "'" : "NULL",
                searchCustomer != null ? "'" + searchCustomer + "'" : "NULL",
                searchOrderId != null ? "'" + searchOrderId + "'" : "NULL",
                enterpriseKey != null ? "'" + enterpriseKey + "'" : "NULL"
            );

            List<Map<String, Object>> results = postgresService.query(query);

            if (results.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
            }

            // Create Excel workbook and sheet
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Filtered Orders");

            // Add header row
            Row headerRow = sheet.createRow(0);
            Map<String, Object> firstRow = results.get(0);
            int colIndex = 0;
            for (String key : firstRow.keySet()) {
                headerRow.createCell(colIndex++).setCellValue(key);
            }

            // Add data rows
            for (int i = 0; i < results.size(); i++) {
                Row row = sheet.createRow(i + 1);
                Map<String, Object> data = results.get(i);
                int j = 0;
                for (Object value : data.values()) {
                    row.createCell(j++).setCellValue(value != null ? value.toString() : "");
                }
            }

            // Write workbook to byte stream
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();

            // Return Excel as downloadable file
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDisposition(ContentDisposition.attachment().filename("filtered_orders.xlsx").build());

            return new ResponseEntity<>(outputStream.toByteArray(), headers, HttpStatus.OK);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }
}
