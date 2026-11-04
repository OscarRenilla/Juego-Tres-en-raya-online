package server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

/** Un jugador conectado: nombre, marca (X/O) y su canal de salida. */
public class Jugador {
    private final String nombre;
    private final Socket socket;
    private final PrintWriter out;
    private int indice;
    private char marca;

    public Jugador(String nombre, Socket socket, PrintWriter out) {
        this.nombre = nombre;
        this.socket = socket;
        this.out = out;
    }

    public void asignar(int indice, char marca) {
        this.indice = indice;
        this.marca = marca;
    }

    /** println con autoflush: una línea = un mensaje. */
    public void enviar(String mensaje) {
        out.println(mensaje);
    }

    public void cerrar() {
        try {
            socket.close();
        } catch (IOException ignorada) {
            // ya estaba cerrado
        }
    }

    public String getNombre() { return nombre; }
    public int getIndice() { return indice; }
    public char getMarca() { return marca; }
}
