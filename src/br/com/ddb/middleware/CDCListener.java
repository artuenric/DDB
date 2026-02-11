package br.com.ddb.middleware;

import com.github.shyiko.mysql.binlog.BinaryLogClient;
import com.github.shyiko.mysql.binlog.event.*;
import java.io.IOException;
import java.util.*;

public class CDCListener {
    private DDBNode node;
    private String dbHost, dbUser, dbPass;
    private int dbPort;

    // Cache: Nome da Tabela -> Lista de Colunas
    private Map<String, List<String>> schemaCache = new HashMap<>();
    // Mapeamento Dinâmico: ID do Binlog -> Nome da Tabela
    private Map<Long, String> tableIdMap = new HashMap<>();

    public CDCListener(DDBNode node, String dbUrl, String user, String pass) {
        this.node = node;
        String cleanUrl = dbUrl.replace("jdbc:mysql://", "");
        String[] parts = cleanUrl.split("/")[0].split(":");
        this.dbHost = parts[0];
        this.dbPort = Integer.parseInt(parts[1]);
        this.dbUser = user;
        this.dbPass = pass;
    }

    public void start() {
        new Thread(() -> {
            // 1. Carrega estrutura do banco (Metadados)
            System.out.println("[CDC] Escaneando tabelas...");
            for (String table : node.getDatabase().getAllTables()) {
                schemaCache.put(table, node.getDatabase().getTableColumns(table));
                System.out.println("   -> " + table + ": " + schemaCache.get(table));
            }

            BinaryLogClient client = new BinaryLogClient(dbHost, dbPort, dbUser, dbPass);
            client.setServerId(System.currentTimeMillis()); // ID Aleatório

            client.registerEventListener(event -> {
                EventData data = event.getData();

                // 2. Aprende qual ID pertence a qual tabela
                if (data instanceof TableMapEventData) {
                    TableMapEventData tm = (TableMapEventData) data;
                    tableIdMap.put(tm.getTableId(), tm.getTable());
                }

                // 3. Processa dados usando o Cache
                else if (data instanceof WriteRowsEventData) {
                    processar((WriteRowsEventData) data, "INSERT");
                } else if (data instanceof UpdateRowsEventData) {
                    processar((UpdateRowsEventData) data, "UPDATE");
                } else if (data instanceof DeleteRowsEventData) {
                    processar((DeleteRowsEventData) data, "DELETE");
                }
            });

            try {
                System.out.println("[CDC] Conectado ao Binlog em " + dbHost);
                client.connect();
            } catch (IOException e) { e.printStackTrace(); }
        }).start();
    }

    private void processar(EventData data, String tipo) {
        // Descobre o nome da tabela pelo ID do evento
        long tableId = 0;
        if (data instanceof WriteRowsEventData) tableId = ((WriteRowsEventData) data).getTableId();
        else if (data instanceof UpdateRowsEventData) tableId = ((UpdateRowsEventData) data).getTableId();
        else if (data instanceof DeleteRowsEventData) tableId = ((DeleteRowsEventData) data).getTableId();

        String tableName = tableIdMap.get(tableId);
        List<String> cols = schemaCache.get(tableName);

        if (tableName != null && cols != null) {
            String sql = "";
            switch (tipo) {
                case "INSERT": sql = BinlogTranslator.convertInsert((WriteRowsEventData) data, tableName, cols); break;
                case "UPDATE": sql = BinlogTranslator.convertUpdate((UpdateRowsEventData) data, tableName, cols); break;
                case "DELETE": sql = BinlogTranslator.convertDelete((DeleteRowsEventData) data, tableName, cols); break;
            }

            System.out.println("[CDC -> SQL] Detectado: " + sql);
            node.propagarAlteracaoCDC(sql);
        }
    }
}