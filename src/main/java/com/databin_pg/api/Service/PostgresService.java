package com.databin_pg.api.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.sql.Array;
import java.util.Optional;

@Service
public class PostgresService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Method to execute SQL that doesn't return results (used for calling stored procedures)
    public void execute(String sql) {
        jdbcTemplate.execute(sql);
    }

    // Method to query and return a list of results
    public List<Map<String, Object>> query(String sql) {
        return jdbcTemplate.queryForList(sql);
    }
    public int update(String sql, List<Object> params) {
        return jdbcTemplate.update(sql, params.toArray());
    }
    private static final Map<String, String> TABLE_DATE_COLUMNS = Map.of(
    	    "orders", "order_date",
    	    "inventory", "restock_date",
    	    "shipment", "actual_shipment_date"
    	    // Add more mappings as needed
    	);
    public Optional<String> getDateColumnForTable(String tableName) {
        return Optional.ofNullable(TABLE_DATE_COLUMNS.get(tableName));
    }
    
    @Autowired
    private DataSource dataSource;

    public List<Map<String, Object>> queryWithArrayHandling(String sql) {
        List<Map<String, Object>> results = new java.util.ArrayList<>();

        try (
            Connection conn = dataSource.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)
        ) {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String column = metaData.getColumnLabel(i);
                    Object value = rs.getObject(i);
                    if (value instanceof Array) {
                        Object[] array = (Object[]) ((Array) value).getArray();
                        row.put(column, Arrays.asList(array));
                    } else {
                        row.put(column, value);
                    }
                }
                results.add(row);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error executing queryWithArrayHandling: " + e.getMessage(), e);
        }

        return results;
    }



}
