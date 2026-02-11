package br.com.ddb.middleware;

import com.github.shyiko.mysql.binlog.BinaryLogClient;
import com.github.shyiko.mysql.binlog.event.*;

import java.io.IOException;

public class CDCListener {
    private String dbHost;
    private int dbPort;
    private String dbUser;
    private String dbPass;

    public CDCListener(String dbUrl, String user, String pass) {
        // Extrai host e porta da URL JDBC (ex: jdbc:mysql://10.10.0.10:3306/db)
        String cleanUrl = dbUrl.replace("jdbc:mysql://", "");
        String[] hostPort = cleanUrl.split("/")[0].split(":");

        this.dbHost = hostPort[0];
        this.dbPort = Integer.parseInt(hostPort[1]);
        this.dbUser = user;
        this.dbPass = pass;
    }

    public void start() {
        new Thread(() -> {
            BinaryLogClient client = new BinaryLogClient(dbHost, dbPort, dbUser, dbPass);

            client.setServerId(System.currentTimeMillis());

            client.registerEventListener(event -> {
                EventData data = event.getData();

                // Filtra apenas eventos de manipulação de dados (INSERT, UPDATE, DELETE)
                if (data instanceof WriteRowsEventData) {
                    System.out.println("[CDC DETECTADO] Inserção realizada: " + data);
                } else if (data instanceof UpdateRowsEventData) {
                    System.out.println("[CDC DETECTADO] Atualização realizada: " + data);
                } else if (data instanceof DeleteRowsEventData) {
                    System.out.println("[CDC DETECTADO] Remoção realizada: " + data);
                }
            });

            try {
                System.out.println("[CDC] Conectando ao Binlog do MySQL em " + dbHost + ":" + dbPort + "...");
                client.connect();
            } catch (IOException e) {
                System.err.println("[CDC Erro] Falha ao conectar no Binlog: " + e.getMessage());
            }
        }).start();
    }
}