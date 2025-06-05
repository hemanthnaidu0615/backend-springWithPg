package com.databin_pg.api.DTO;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

public class ExcelUtil {
    public static byte[] generateExcel(List<Map<String, Object>> data) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Report");

            if (!data.isEmpty()) {
                List<String> keys = new ArrayList<>(data.get(0).keySet());

                // Create a bold font for headers
                Font headerFont = workbook.createFont();
                headerFont.setBold(true);
                CellStyle headerStyle = workbook.createCellStyle();
                headerStyle.setFont(headerFont);

                // Write header row with formatted names
                Row headerRow = sheet.createRow(0);
                for (int i = 0; i < keys.size(); i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(toTitleCase(keys.get(i))); // Format: order_id → Order Id
                    cell.setCellStyle(headerStyle);
                }

                // Write data rows
                for (int i = 0; i < data.size(); i++) {
                    Row row = sheet.createRow(i + 1);
                    Map<String, Object> rowData = data.get(i);
                    for (int j = 0; j < keys.size(); j++) {
                        Object value = rowData.get(keys.get(j));
                        row.createCell(j).setCellValue(value != null ? value.toString() : "");
                    }
                }

                // Auto-size columns
                for (int i = 0; i < keys.size(); i++) {
                    sheet.autoSizeColumn(i);
                }
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    // Convert snake_case to Title Case (e.g., order_id → Order Id)
    private static String toTitleCase(String input) {
        String[] parts = input.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)))
                  .append(part.substring(1).toLowerCase())
                  .append(" ");
            }
        }
        return sb.toString().trim();
    }
}
