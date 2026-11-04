package client;

import common.Protocolo;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Cliente de consola. Uso: Cliente [host] [puerto] [nombre] */
public class Cliente {
    private static final Pattern JUGADA = Pattern.compile("^([0-2])\\s*[ ,]\\s*([0-2])$");

    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private volatile boolean terminado = false;
    private volatile String marca = "?";

    // Conexión, registro y hilo receptor

    public Cliente(String host, int puerto) throws IOException {
        socket = new Socket(host, puerto);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
    }

    public void registrar(String nombre) {
        out.println(Protocolo.PLAY + " " + nombre);
    }

    /** Hilo que escucha al servidor mientras el principal atiende el teclado. */
    private void escucharServidor() {
        try {
            String linea;
            while ((linea = in.readLine()) != null) {
                procesar(linea);
            }
            if (!terminado) System.out.println("El servidor ha cerrado la conexión.");
        } catch (IOException e) {
            if (!terminado) System.out.println("Conexión perdida: " + e.getMessage());
        } finally {
            terminado = true;
        }
    }

    // Interpretar mensajes del servidor y dibujar 

    private void procesar(String linea) {
        String[] p = linea.split(" ", 2);
        String resto = p.length > 1 ? p[1].trim() : "";
        switch (p[0]) {
            case Protocolo.WAIT -> System.out.println("Esperando al otro jugador...");
            case Protocolo.START -> {
                marca = resto;
                System.out.println("¡Empieza la partida! Juegas con " + marca);
                TableroConsola.imprimir(".........");
            }
            case Protocolo.OK -> TableroConsola.imprimir(resto);
            case Protocolo.TURN -> System.out.println(resto.equals(marca)
                    ? ">>> Tu turno. Escribe: fila columna (ej. 1 1)"
                    : "Turno del rival...");
            case Protocolo.DENIED -> System.out.println("Jugada rechazada: " + resto);
            case Protocolo.END -> mostrarFin(resto);
            case Protocolo.FULL -> {
                System.out.println("El servidor te ha rechazado: " + resto);
                terminado = true;
            }
            case Protocolo.LEFT -> {
                System.out.println("Tu rival se ha ido. Fin.");
                terminado = true;
            }
            default -> System.out.println("[servidor] " + linea);
        }
    }

    private void mostrarFin(String resto) {
        String[] r = resto.split(" ");
        boolean abandono = r.length > 2 && r[2].equals(Protocolo.ABANDONO);
        if (r[0].equals(Protocolo.DRAW)) {
            System.out.println("Fin de la partida: EMPATE.");
        } else if (r[0].equals(Protocolo.WIN)) {
            boolean yo = r.length > 1 && r[1].equals(marca);
            System.out.println((yo ? "¡HAS GANADO!" : "Has perdido.") + (abandono ? " (el rival abandonó)" : ""));
        }
        if (abandono) {
            terminado = true;
        } else {
            System.out.println("Escribe 'r' para pedir revancha o 'q' para salir.");
        }
    }

    // Entrada por teclado, revancha, BYE y cierre 

    private void bucleEntrada() throws IOException, InterruptedException {
        BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));
        while (!terminado) {
            if (!teclado.ready()) {      // sondeo: así el bucle se entera si el hilo receptor termina
                Thread.sleep(100);
                continue;
            }
            String linea = teclado.readLine();
            if (linea == null) break;
            linea = linea.trim();
            if (linea.isEmpty()) continue;

            if (linea.equalsIgnoreCase("q")) {
                out.println(Protocolo.BYE);
                break;
            }
            if (linea.equalsIgnoreCase("r")) {
                out.println(Protocolo.REMATCH);
                continue;
            }
            Matcher m = JUGADA.matcher(linea);
            if (m.matches()) {
                out.println(Protocolo.MOVE + " " + m.group(1) + "," + m.group(2));
            } else {
                System.out.println("Formato: fila columna (0-2), 'r' revancha, 'q' salir.");
            }
        }
    }

    public void jugar() {
        Thread receptor = new Thread(this::escucharServidor, "receptor");
        receptor.setDaemon(true);
        receptor.start();
        try {
            bucleEntrada();
        } catch (IOException | InterruptedException e) {
            System.out.println("Error de entrada: " + e.getMessage());
        } finally {
            cerrar();
        }
    }

    private void cerrar() {
        terminado = true;
        try {
            socket.close();   // cierra también in y out
        } catch (IOException ignorada) {
            // nada que hacer
        }
        System.out.println("Cliente cerrado.");
    }

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "localhost";
        int puerto = Protocolo.PUERTO_POR_DEFECTO;
        if (args.length > 1) {
            try {
                puerto = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Puerto inválido, uso el " + puerto);
            }
        }
        try {
            String nombre = args.length > 2 ? args[2] : null;
            if (nombre == null) {
                System.out.print("Tu nombre: ");
                nombre = new BufferedReader(new InputStreamReader(System.in)).readLine();
            }
            Cliente cliente = new Cliente(host, puerto);
            cliente.registrar(nombre == null || nombre.isBlank() ? "Jugador" : nombre.trim());
            cliente.jugar();
        } catch (IOException e) {
            System.err.println("No se pudo conectar con " + host + ":" + puerto + " -> " + e.getMessage());
        }
    }
}
