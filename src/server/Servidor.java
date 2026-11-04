package server;

import common.Protocolo;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/** Arranca el servidor: acepta conexiones y lanza un hilo por cada una. Uso: Servidor [puerto] */
public class Servidor {
    public static void main(String[] args) {
        int puerto = Protocolo.PUERTO_POR_DEFECTO;
        if (args.length > 0) {
            try {
                puerto = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Puerto inválido, uso el " + puerto);
            }
        }

        Partida partida = new Partida();
        try (ServerSocket servidor = new ServerSocket(puerto)) {
            System.out.println("[servidor] escuchando en el puerto " + puerto);
            while (true) {
                Socket socket = servidor.accept();
                System.out.println("[servidor] conexión desde " + socket.getRemoteSocketAddress());
                new Thread(new ManejadorJugador(socket, partida)).start();
            }
        } catch (IOException e) {
            System.err.println("[servidor] error: " + e.getMessage());
        }
    }
}
