package com.databin_pg.api.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
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


}
