# Tres en raya online: cliente-servidor TCP en Java

**Autores:** Óscar Renilla y Álvaro Cotumba 

**Asignatura / curso:** Programación de Servicios y Procesos 

**Repositorio:** https://github.com/OscarRenilla/Juego-Tres-en-raya-online.git

---

## 1. Descripción del sistema

El sistema permite jugar una partida de tres en raya entre dos clientes remotos conectados por TCP. Se compone de dos programas independientes escritos en Java 17 sin dependencias externas:

- **Servidor:** escucha en un puerto TCP configurable (por defecto 5050) y actúa como árbitro. Acepta exactamente dos jugadores, guarda el estado del tablero, valida cada jugada, controla los turnos y declara el resultado. Un tercer cliente recibe `FULL Partida llena` y se cierra su conexión.
- **Cliente:** se conecta al servidor, se registra con su nombre, dibuja el tablero en consola y envía las jugadas del usuario. Muestra el estado que le notifica el servidor tras cada movimiento.

El servidor gestiona **una única partida** de dos jugadores. Cuando ya no queda nadie conectado, la partida se reinicia y admite dos jugadores nuevos.

## 2. Protocolo de comunicación

Protocolo **textual**, en UTF-8. Cada mensaje es una línea con el formato `COMANDO [argumentos]`. Las casillas se indican como `fila,col` (valores de 0 a 2) y el tablero como una cadena de 9 caracteres leída fila a fila (`X`, `O` o `.`), por ejemplo `X.O..X...`.

**Cliente → Servidor**

| Comando | Descripción |
|---|---|
| `PLAY <nombre>` | Pide entrar a la partida. Es el primer mensaje de la conexión. |
| `MOVE <fila,col>` | Quiere marcar esa casilla. |
| `REMATCH` | Pide revancha (solo al terminar la partida). |
| `BYE` | Desconexión voluntaria. |

**Servidor → Cliente**

| Comando | Descripción |
|---|---|
| `WAIT` | Falta el segundo jugador, o el rival aún no ha aceptado la revancha. |
| `START <X\|O>` | La partida empieza y esta es tu marca. |
| `TURN <X\|O>` | De quién es el turno (a ambos). |
| `OK <tablero>` | Movimiento aceptado y tablero actualizado (a ambos). |
| `DENIED <motivo>` | Movimiento o comando rechazado (solo al emisor). |
| `END WIN <X\|O>` / `END DRAW` | Fin de la partida con su resultado (a ambos). |
| `END WIN <X\|O> ABANDONO` | El rival se desconectó a mitad de partida. |
| `FULL <texto>` | Partida llena; se cierra la conexión. |
| `LEFT` | El rival se fue después de terminar la partida; se cierra la conexión. |

Los motivos de `DENIED` son: `no es tu turno`, `casilla ocupada`, `fuera del tablero`, `formato invalido`, `partida terminada`, `esperando al rival`, `se esperaba PLAY <nombre>` y `comando desconocido`.

### 2.1 Cambios respecto a la propuesta del enunciado

| Cambio | Justificación |
|---|---|
| Se añade `LEFT`. | La propuesta no cubre que un jugador se vaya *después* del fin de partida. El rival necesita saber que la revancha ya no es posible. |
| `END` admite `ABANDONO` como tercer argumento. | Distingue una victoria por abandono de una normal sin crear otro comando. |
| `WAIT` se reutiliza tras `REMATCH`. | Evita añadir un mensaje nuevo para «estás esperando al rival». |
| `FULL` lleva texto. | El enunciado pide un mensaje de error para el tercer cliente. |
| `DENIED` también cubre errores de registro y de formato. | Un único mensaje de error para el cliente, con el motivo en texto. |
| Coordenadas de 0 a 2. | Coinciden con los índices del array del tablero. |

## 3. Diagrama de flujo de una partida

*Figura 1. Conexión → espera → juego → fin.*

En palabras:

1. **Conexión.** El cliente abre el socket y envía `PLAY <nombre>`.
2. **Espera.** El primer jugador recibe `WAIT` y la marca `X`. Un tercero recibiría `FULL`.
3. **Juego.** Al llegar el segundo jugador (marca `O`), ambos reciben `START` y `TURN X`. Cada `MOVE` válido genera `OK` y `TURN` para los dos. Uno inválido genera `DENIED` solo para quien lo envió.
4. **Fin.** Con tres en línea, tablero lleno o abandono, ambos reciben `END`. Si los dos envían `REMATCH`, vuelve a `START`. Con `BYE` (o una caída), el rival recibe `LEFT` y se cierra su conexión.

## 4. Decisiones de diseño

**Arquitectura en capas.** El servidor separa responsabilidades en cuatro clases:
- `Tablero`: lógica pura del juego, sin red ni hilos. Se puede probar de forma aislada.
- `Partida`: estado y reglas de turno.
- `ManejadorJugador`: lectura de comandos de un cliente.
- `Servidor`: acepta conexiones.

Las constantes del protocolo están en `common/Protocolo.java`, compartidas por cliente y servidor, para que no se desincronicen.

**Concurrencia en el servidor.** Cada conexión tiene su propio hilo, así que un cliente bloqueado en `readLine()` no afecta al otro. `Partida` es un **monitor**: todos sus métodos públicos son `synchronized`. Dos `MOVE` simultáneos se procesan uno detrás de otro, por lo que el turno y el tablero nunca quedan en un estado intermedio. Registrar al segundo jugador y enviar `START` ocurren dentro de la misma sección crítica.

**El servidor es la única fuente de verdad.** El cliente no valida turnos ni casillas: solo muestra lo que dice el servidor. Así, cualquier cliente que cumpla el protocolo es válido y no se puede hacer trampa.

**Dos hilos en el cliente.** Uno escucha al servidor y otro atiende el teclado. Así el cliente recibe la jugada del rival mientras espera la entrada del usuario. La bandera `terminado` es `volatile` para que ambos hilos la vean. El bucle de teclado usa sondeo con `ready()` para enterarse de que el servidor ha cerrado sin quedarse bloqueado en `readLine()`.

**Gestión de errores y cierre de recursos.**
- Uso de `try-with-resources` para el socket.
- `finally` que llama a `Partida.desconectar(...)`, que es **idempotente**: es seguro llamarla varias veces.
- Si un jugador se va a mitad de partida, el rival gana por abandono (`END WIN ... ABANDONO`) y se cierra su conexión. Después, la partida queda libre.
- El servidor tolera entradas inválidas (`PLAY` mal formado, jugadas con formato incorrecto, comandos desconocidos) respondiendo con `DENIED` en vez de caerse.

**Limitaciones conocidas.**
- Una sola partida simultánea, como pide el enunciado.
- En la revancha se conservan las marcas, así que `X` empieza siempre.
- No hay temporizador de inactividad.

**Trabajo en equipo con ramas.** Trabajamos con dos ramas, `feature/servidor` y `feature/cliente`, con 4 subidas cada una y pull requests con revisión cruzada. Se fusionó con *merge commit* para conservar el historial.

## 5. Pruebas realizadas

*(Marcad como hechas solo las que hayáis probado vosotros, y adjuntad capturas en `docs/img/`.)*

| Prueba | Resultado esperado |
|---|---|
| Un solo jugador conectado | Recibe `WAIT` |
| Dos jugadores conectados | Ambos reciben `START` y `TURN X` |
| Tercer cliente | Recibe `FULL Partida llena` y se cierra |
| `MOVE` fuera de turno | `DENIED no es tu turno` |
| Casilla ocupada / fuera del tablero | `DENIED` con su motivo |
| Victoria por tres en línea | `END WIN X` a ambos |
| Empate | `END DRAW` a ambos |
| `MOVE` tras terminar la partida | `DENIED partida terminada` |
| Revancha de ambos jugadores | Nuevo `START` |
| Un jugador cierra a mitad de partida | El otro recibe `END WIN ... ABANDONO` |
| `BYE` tras terminar | El rival recibe `LEFT` |
| Dos máquinas distintas en red | Partida completa sin errores |

## 6. Reparto del trabajo

| Persona | Rama | Trabajo |
|---|---|---|
| Óscar Renilla | `feature/servidor` | `Tablero`, aceptación de jugadores, turnos y validación, revancha, abandono y cierre de recursos |
| Álvaro Cotumba | `feature/cliente` | Conexión y hilo receptor, dibujo del tablero, entrada por teclado, revancha y cierre, memoria |

El historial completo está en el repositorio (`git log --graph --oneline --all`).