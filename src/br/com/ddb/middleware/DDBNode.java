package br.com.ddb.middleware;

import br.com.ddb.common.Message;
import java.io.*;
import java.net.*;
import java.util.*;

public class DDBNode {
    private String myIp;
    private String coordinatorIp;
    private List<String> allNodes;
    private Database db;
    private BullyElection election;
    private Replication replication;

    public Database getDatabase() {
        return this.db;
    }

    public DDBNode(String myIp, String nodesRaw, String dbUrl) throws Exception {
        this.myIp = myIp;
        this.allNodes = Arrays.asList(nodesRaw.split(","));
        this.db = new Database(dbUrl, "root", "root");
        this.election = new BullyElection(this);
        this.replication = new Replication(this);
        this.coordinatorIp = allNodes.get(allNodes.size() - 1);
    }

    public void start() throws IOException {
        new Thread(this::heartbeatTask).start();
        // Escuta em todas as interfaces do container
        CDCListener cdc = new CDCListener(System.getenv("DB_URL"), "root", "root");
        cdc.start();
        ServerSocket server = new ServerSocket(5000);
        System.out.println("[Nó " + myIp + "] Ouvindo na porta 5000...");
        while (true) {
            Socket s = server.accept();
            new Thread(() -> handle(s)).start();
        }
    }

    private void handle(Socket s) {
        try (ObjectInputStream in = new ObjectInputStream(s.getInputStream());
             ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream())) {

            Message msg = (Message) in.readObject();
            if (!msg.checksum.equals(Message.calculateMD5(msg.payload))) return;

            switch (msg.type) {
                case "PREPARE":
                    // Em vez de enviar String, enviamos o objeto Message esperado
                    out.writeObject(new Message("READY", myIp, "READY"));
                    out.flush();
                    break;

                case "QUERY":
                    Object result;
                    if (isWrite(msg.payload)) {
                        if (myIp.equals(coordinatorIp)) {
                            result = replication.executeTwoPhaseCommit(msg.payload);
                        } else {
                            result = forward(coordinatorIp, msg);
                        }
                    } else {
                        result = db.executeLocal(msg.payload) + "\n[Nó: " + myIp + "]";
                    }
                    // Para evitar o erro no Cliente e no Forward, enviamos o resultado
                    out.writeObject(result);
                    out.flush();
                    break;
                case "COMMIT":
                    db.executeLocal(msg.payload);
                    break;
                case "COORDINATOR":
                    this.coordinatorIp = msg.payload;
                    break;
                case "ELECTION":
                    out.writeObject(new Message("OK", myIp, ""));
                    election.startElection();
                    break;
            }
        } catch (Exception e) {
            // Log de erro para debug interno
        }
    }

    private boolean isWrite(String sql) {
        String s = sql.toUpperCase();
        return s.contains("INSERT") || s.contains("UPDATE") || s.contains("DELETE");
    }

    private Object forward(String ip, Message m) {
        return sendAndReceive(ip, m);
    }

    public Message sendAndReceive(String ip, Message m) {
        if (ip.equals(myIp)) {
            return new Message("READY", myIp, "READY");
        }

        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(ip, 5000), 2000);
            ObjectOutputStream o = new ObjectOutputStream(s.getOutputStream());
            ObjectInputStream i = new ObjectInputStream(s.getInputStream());

            o.writeObject(m);
            o.flush();

            Object response = i.readObject();

            // Verificação de segurança: se recebeu String, encapsula em Message para não dar erro
            if (response instanceof String) {
                return new Message("RESPONSE", ip, (String) response);
            }

            return (Message) response;
        } catch (Exception e) {
            System.err.println("[Erro Rede] Falha ao comunicar com " + ip + ": " + e.getMessage());
            return null;
        }
    }

    public boolean sendMessage(String ip, Message m) {
        if (ip.equals(myIp)) return true;

        try {
            Socket s = new Socket();
            s.connect(new InetSocketAddress(ip, 5000), 1000);
            ObjectOutputStream o = new ObjectOutputStream(s.getOutputStream());
            o.writeObject(m);
            o.flush();
            s.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void heartbeatTask() {
        while (true) {
            try {
                Thread.sleep(5000);
                if (!myIp.equals(coordinatorIp)) {
                    if (!sendMessage(coordinatorIp, new Message("HEARTBEAT", myIp, ""))) {
                        System.out.println("[Heartbeat] Coordenador falhou. Iniciando eleição...");
                        election.startElection();
                    }
                }
            } catch (Exception e) {}
        }
    }

    public int getIdFromIp(String ip) {
        try {
            return Integer.parseInt(ip.substring(ip.lastIndexOf('.') + 1));
        } catch (Exception e) { return 0; }
    }

    public String getMyIp() { return myIp; }
    public List<String> getAllNodes() { return allNodes; }
    public void setCoordinator(String ip) { this.coordinatorIp = ip; }

    public static void main(String[] args) throws Exception {
        String ip = System.getenv("MY_IP");
        String nodes = System.getenv("ALL_NODES");
        String dbUrl = System.getenv("DB_URL");
        new DDBNode(ip, nodes, dbUrl).start();
    }


}