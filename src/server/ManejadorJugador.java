package server;

import common.Protocolo;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/** Un hilo por conexión: lee las líneas del cliente y las traduce en llamadas a Partida. */
public class ManejadorJugador implements Runnable {
    private final Socket socket;
    private final Partida partida;

    public ManejadorJugador(Socket socket, Partida partida) {
        this.socket = socket;
        this.partida = partida;
    }

    @Override
    public void run() {
        Jugador jugador = null;
        try (Socket s = socket) {
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true);

            // Registro: PLAY <nombre> 
            if (partida.estaLlena()) {
                out.println(Protocolo.FULL + " Partida llena");
                return;
            }
            String linea = in.readLine();
            if (linea == null) return;
            String[] p = linea.trim().split("\\s+", 2);
            if (!p[0].equalsIgnoreCase(Protocolo.PLAY) || p.length < 2 || p[1].isBlank()) {
                out.println(Protocolo.DENIED + " se esperaba PLAY <nombre>");
                return;
            }
            jugador = new Jugador(p[1].trim(), s, out);
            if (!partida.unir(jugador)) {
                out.println(Protocolo.FULL + " Partida llena");
                jugador = null;
                return;
            }

            // Bucle de comandos 
            while ((linea = in.readLine()) != null) {
                String[] c = linea.trim().split("\\s+", 2);
                String argumento = c.length > 1 ? c[1] : "";
                switch (c[0].toUpperCase()) {
                    case Protocolo.MOVE -> partida.mover(jugador, argumento);
                    case Protocolo.REMATCH -> partida.revancha(jugador);
                    case Protocolo.BYE -> { return; }
                    default -> jugador.enviar(Protocolo.DENIED + " comando desconocido");
                }
            }
        } catch (IOException e) {
            System.out.println("[servidor] conexión cerrada: " + e.getMessage());
        } finally {
            // Pase lo que pase, avisar al rival y liberar recursos 
            if (jugador != null) {
                partida.desconectar(jugador);
            }
        }
    }
}