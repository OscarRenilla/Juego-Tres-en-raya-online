package common;

/**
 * Constantes del protocolo textual. Cada mensaje es UNA línea terminada en salto de línea:
 *   COMANDO [argumentos]
 * Compartido por cliente y servidor para que nunca se desincronicen.
 */
public final class Protocolo {
    public static final int PUERTO_POR_DEFECTO = 5050;

    // Cliente -> Servidor
    public static final String PLAY = "PLAY";
    public static final String MOVE = "MOVE";
    public static final String REMATCH = "REMATCH";
    public static final String BYE = "BYE";

    // Servidor -> Cliente
    public static final String FULL = "FULL";
    public static final String WAIT = "WAIT";
    public static final String START = "START";
    public static final String OK = "OK";
    public static final String DENIED = "DENIED";
    public static final String TURN = "TURN";
    public static final String END = "END";
    public static final String LEFT = "LEFT";

    // Argumentos de END
    public static final String WIN = "WIN";
    public static final String DRAW = "DRAW";
    public static final String ABANDONO = "ABANDONO";

    private Protocolo() { }
}
