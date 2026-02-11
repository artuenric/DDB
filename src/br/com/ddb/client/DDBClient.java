package br.com.ddb.client;

import br.com.ddb.common.Message;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;

public class DDBClient extends JFrame {
    private JTextArea query = new JTextArea(5, 40);
    private JTextArea result = new JTextArea(15, 50);
    private JTextField host = new JTextField("localhost", 10);
    private JTextField port = new JTextField("5000", 5);

    public DDBClient() {
        setTitle("DDB MySQL Client");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        JPanel p = new JPanel();
        p.add(new JLabel("Host:")); p.add(host);
        p.add(new JLabel("Porta:")); p.add(port);
        JButton b = new JButton("Executar");
        b.addActionListener(e -> call());
        add(p, BorderLayout.NORTH);
        add(new JScrollPane(query), BorderLayout.CENTER);
        JPanel s = new JPanel(new BorderLayout());
        s.add(b, BorderLayout.NORTH);
        s.add(new JScrollPane(result), BorderLayout.CENTER);
        add(s, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(null);
    }

    private void call() {
        result.setText("Processando...");
        try (Socket s = new Socket(host.getText(), Integer.parseInt(port.getText()));
             ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {
            out.writeObject(new Message("QUERY", "CLIENT", query.getText()));
            result.setText(in.readObject().toString());
        } catch (Exception e) { result.setText("Erro: " + e.getMessage()); }
    }

    public static void main(String[] args) { SwingUtilities.invokeLater(() -> new DDBClient().setVisible(true)); }
}