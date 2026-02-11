package br.com.ddb.middleware;

import com.github.shyiko.mysql.binlog.event.*;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class BinlogTranslator {

    public static String convertInsert(WriteRowsEventData data, String tableName, List<String> cols) {
        StringBuilder sql = new StringBuilder();
        for (Serializable[] row : data.getRows()) {
            sql.append("INSERT INTO ").append(tableName).append(" (");
            // Nomes das colunas
            for (int i = 0; i < cols.size(); i++) {
                sql.append(cols.get(i)).append(i < cols.size() - 1 ? ", " : "");
            }
            sql.append(") VALUES (");
            // Valores
            for (int i = 0; i < row.length && i < cols.size(); i++) {
                sql.append(format(row[i])).append(i < cols.size() - 1 ? ", " : "");
            }
            sql.append("); ");
        }
        return sql.toString();
    }

    public static String convertUpdate(UpdateRowsEventData data, String tableName, List<String> cols) {
        StringBuilder sql = new StringBuilder();
        for (Map.Entry<Serializable[], Serializable[]> row : data.getRows()) {
            Serializable[] after = row.getValue();
            sql.append("UPDATE ").append(tableName).append(" SET ");
            // Pula o índice 0 (PK) no SET
            for (int i = 1; i < cols.size(); i++) {
                sql.append(cols.get(i)).append("=").append(format(after[i]))
                        .append(i < cols.size() - 1 ? ", " : "");
            }
            // WHERE usa a coluna 0
            sql.append(" WHERE ").append(cols.get(0)).append("=").append(format(after[0])).append("; ");
        }
        return sql.toString();
    }

    public static String convertDelete(DeleteRowsEventData data, String tableName, List<String> cols) {
        StringBuilder sql = new StringBuilder();
        for (Serializable[] row : data.getRows()) {
            sql.append("DELETE FROM ").append(tableName)
                    .append(" WHERE ").append(cols.get(0)).append("=").append(format(row[0])).append("; ");
        }
        return sql.toString();
    }

    private static String format(Object val) {
        if (val == null) return "NULL";
        if (val instanceof String) return "'" + val + "'";
        if (val instanceof Date) return "'" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((Date) val) + "'";
        return val.toString();
    }
}