package server;

import java.util.Arrays;

/** Lógica pura del tablero 3x3. No sabe nada de red ni de hilos. */
public class Tablero {
    public static final int TAM = 3;
    public static final char VACIA = '.';

    private final char[][] celdas = new char[TAM][TAM];

    public Tablero() {
        reiniciar();
    }

    public void reiniciar() {
        for (char[] fila : celdas) {
            Arrays.fill(fila, VACIA);
        }
    }

    public boolean dentro(int fila, int col) {
        return fila >= 0 && fila < TAM && col >= 0 && col < TAM;
    }

    /** Coloca la marca si la casilla está dentro y libre. Devuelve false en caso contrario. */
    public boolean colocar(int fila, int col, char marca) {
        if (!dentro(fila, col) || celdas[fila][col] != VACIA) {
            return false;
        }
        celdas[fila][col] = marca;
        return true;
    }

    public boolean hayGanador(char m) {
        for (int i = 0; i < TAM; i++) {
            if (celdas[i][0] == m && celdas[i][1] == m && celdas[i][2] == m) return true; // fila
            if (celdas[0][i] == m && celdas[1][i] == m && celdas[2][i] == m) return true; // columna
        }
        return (celdas[0][0] == m && celdas[1][1] == m && celdas[2][2] == m)
            || (celdas[0][2] == m && celdas[1][1] == m && celdas[2][0] == m);
    }

    public boolean estaLleno() {
        for (char[] fila : celdas) {
            for (char c : fila) {
                if (c == VACIA) return false;
            }
        }
        return true;
    }

    /** 9 caracteres, fila a fila. Ej: "X.O..X...". */
    public String comoTexto() {
        StringBuilder sb = new StringBuilder(TAM * TAM);
        for (char[] fila : celdas) {
            sb.append(fila);
        }
        return sb.toString();
    }
}
