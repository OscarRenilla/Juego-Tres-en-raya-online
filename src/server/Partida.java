package server;

import common.Protocolo;
import java.util.Arrays;

/**
 * Estado de la única partida del servidor.
 * Es un MONITOR: todos los métodos públicos son synchronized, de modo que aunque cada jugador
 * tenga su propio hilo, solo uno modifica el estado a la vez (así los turnos nunca se pisan).
 */
public class Partida {
    private final Jugador[] jugadores = new Jugador[2]; // [0] = X, [1] = O
    private final boolean[] quiereRevancha = new boolean[2];
    private final Tablero tablero = new Tablero();
    private char turno = 'X';
    private boolean enCurso = false;

    // Entrada de jugadores 

    public synchronized boolean estaLlena() {
        return jugadores[0] != null && jugadores[1] != null;
    }

    /** Registra al jugador. Devuelve false si la partida ya tiene dos jugadores. */
    public synchronized boolean unir(Jugador jugador) {
        for (int i = 0; i < jugadores.length; i++) {
            if (jugadores[i] == null) {
                jugadores[i] = jugador;
                jugador.asignar(i, i == 0 ? 'X' : 'O');
                System.out.println("[servidor] " + jugador.getNombre() + " entra como " + jugador.getMarca());
                if (i == 0) {
                    jugador.enviar(Protocolo.WAIT);
                } else {
                    empezar();
                }
                return true;
            }
        }
        return false;
    }

    private void empezar() {
        tablero.reiniciar();
        turno = 'X';
        enCurso = true;
        Arrays.fill(quiereRevancha, false);
        for (Jugador j : jugadores) {
            j.enviar(Protocolo.START + " " + j.getMarca());
        }
        difundir(Protocolo.TURN + " " + turno);
    }

    private void difundir(String mensaje) {
        for (Jugador j : jugadores) {
            if (j != null) j.enviar(mensaje);
        }
    }
}