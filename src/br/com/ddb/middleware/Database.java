package br.com.ddb.middleware;

import java.sql.*;

public class Database {
    private Connection connection;

    public Database(String url, String user, String pass) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        int attempts = 0;
        while (attempts < 12) {
            try {
                this.connection = DriverManager.getConnection(url, user, pass);
                return;
            } catch (SQLException e) {
                attempts++;
                System.out.println("Aguardando MySQL (" + attempts + "/12)...");
                Thread.sleep(5000);
            }
        }
        throw new Exception("Falha ao conectar ao banco de dados.");
    }

    public synchronized String executeLocal(String sql) {
        try (Statement stmt = connection.createStatement()) {
            if (sql.trim().toUpperCase().startsWith("SELECT")) {
                ResultSet rs = stmt.executeQuery(sql);
                ResultSetMetaData meta = rs.getMetaData();
                StringBuilder sb = new StringBuilder();
                int cols = meta.getColumnCount();
                while (rs.next()) {
                    for (int i = 1; i <= cols; i++) sb.append(rs.getString(i)).append(" | ");
                    sb.append("\n");
                }
                return sb.length() == 0 ? "Nenhum registro." : sb.toString();
            } else {
                int rows = stmt.executeUpdate(sql);
                return "Sucesso: " + rows + " linhas afetadas.";
            }
        } catch (SQLException e) {
            return "Erro SQL Local: " + e.getMessage();
        }
    }

    // 1. Lista todas as tabelas do banco
    public java.util.List<String> getAllTables() {
        java.util.List<String> tables = new java.util.ArrayList<>();
        try {
            DatabaseMetaData meta = connection.getMetaData();
            // Pega apenas tabelas do tipo TABLE (ignora views, system tables)
            try (ResultSet rs = meta.getTables(connection.getCatalog(), null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME"));
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database] Erro ao listar tabelas: " + e.getMessage());
        }
        return tables;
    }

    // 2. Lista as colunas de uma tabela na ordem correta
    public java.util.List<String> getTableColumns(String tableName) {
        java.util.List<String> columns = new java.util.ArrayList<>();
        try {
            DatabaseMetaData meta = connection.getMetaData();
            try (ResultSet rs = meta.getColumns(connection.getCatalog(), null, tableName, null)) {
                while (rs.next()) {
                    columns.add(rs.getString("COLUMN_NAME"));
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database] Erro colunas (" + tableName + "): " + e.getMessage());
        }
        return columns;
    }
}