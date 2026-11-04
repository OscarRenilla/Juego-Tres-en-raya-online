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

    // Movimientos y turnos 

    public synchronized void mover(Jugador jugador, String argumento) {
        if (jugadores[1] == null) {
            jugador.enviar(Protocolo.DENIED + " esperando al rival");
            return;
        }
        if (!enCurso) {
            jugador.enviar(Protocolo.DENIED + " partida terminada");
            return;
        }
        if (jugador.getMarca() != turno) {
            jugador.enviar(Protocolo.DENIED + " no es tu turno");
            return;
        }

        int fila, col;
        try {
            String[] partes = argumento.trim().split(",");
            if (partes.length != 2) throw new NumberFormatException();
            fila = Integer.parseInt(partes[0].trim());
            col = Integer.parseInt(partes[1].trim());
        } catch (NumberFormatException e) {
            jugador.enviar(Protocolo.DENIED + " formato invalido, usa MOVE fila,col");
            return;
        }

        if (!tablero.dentro(fila, col)) {
            jugador.enviar(Protocolo.DENIED + " fuera del tablero");
            return;
        }
        if (!tablero.colocar(fila, col, turno)) {
            jugador.enviar(Protocolo.DENIED + " casilla ocupada");
            return;
        }

        difundir(Protocolo.OK + " " + tablero.comoTexto());

        if (tablero.hayGanador(turno)) {
            enCurso = false;
            difundir(Protocolo.END + " " + Protocolo.WIN + " " + turno);
        } else if (tablero.estaLleno()) {
            enCurso = false;
            difundir(Protocolo.END + " " + Protocolo.DRAW);
        } else {
            turno = (turno == 'X') ? 'O' : 'X';
            difundir(Protocolo.TURN + " " + turno);
        }
    }

}