package client;

/** Dibuja en consola un tablero recibido como 9 caracteres ("X.O..X..."). */
public final class TableroConsola {
    private TableroConsola() { }

    public static void imprimir(String t) {
        if (t == null || t.length() != 9) {
            System.out.println("Tablero inválido: " + t);
            return;
        }
        System.out.println();
        System.out.println("    0   1   2");
        for (int f = 0; f < 3; f++) {
            System.out.printf("%d   %c | %c | %c%n", f, t.charAt(f * 3), t.charAt(f * 3 + 1), t.charAt(f * 3 + 2));
            if (f < 2) System.out.println("   ---+---+---");
        }
        System.out.println();
    }
}
    