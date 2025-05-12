package com.databin_pg.api.DTO;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExcelUtil {
    public static byte[] generateExcel(List<Map<String, Object>> data) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Report");
            if (!data.isEmpty()) {
                Row header = sheet.createRow(0);
                List<String> keys = new ArrayList<>(data.get(0).keySet());
                for (int i = 0; i < keys.size(); i++) {
                    header.createCell(i).setCellValue(keys.get(i));
                }

                for (int i = 0; i < data.size(); i++) {
                    Row row = sheet.createRow(i + 1);
                    Map<String, Object> rowData = data.get(i);
                    for (int j = 0; j < keys.size(); j++) {
                        Object value = rowData.get(keys.get(j));
                        row.createCell(j).setCellValue(value != null ? value.toString() : "");
                    }
                }
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }
}
