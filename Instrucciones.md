# Cómo jugar al tres en raya online (guía desde cero)
 
Para jugar hacen falta **dos jugadores** y **un servidor**. El servidor se arranca en uno de los dos ordenadores. Por eso hay dos papeles:
 
| Papel | Qué hace | Ordenador |
|---|---|---|
| **Jugador 1 (anfitrión)** | Arranca el servidor y también juega | El que hace de servidor |
| **Jugador 2 (invitado)** | Solo se conecta y juega | Otro ordenador de la misma red (o el mismo, para pruebas) |

> Si probáis los dos en un único ordenador, solo se hace la parte del anfitrión y el invitado abre otra terminal en esa misma máquina (usando `localhost`).

---

## Parte común: lo que tienen que hacer LOS DOS

### 1. Instalar lo necesario

1. **JDK 17 o superior** (no basta con el JRE, hace falta `javac`). Descárgalo de <https://adoptium.net>.
2. **Git**: <https://git-scm.com/downloads>.
3. *(Opcional)* **VS Code** con la extensión *Extension Pack for Java*.

Comprueba en PowerShell que funcionan los tres comandos:

```powershell
java -version
javac -version
git --version
```

Si alguno da error, reinicia la terminal. Si sigue fallando, reinstala marcando la opción de añadir al `PATH`.

### 2. Descargar el proyecto

Descarga la rama `main`, que es la que tiene todo fusionado:

```powershell
cd Desktop
git clone <URL-DEL-REPOSITORIO> Juego-Tres-en-raya-online
cd Juego-Tres-en-raya-online
```

Si ya lo tenías clonado, actualízalo:

```powershell
git switch main
git pull
```

### 3. Compilar

Desde la carpeta del proyecto:

```powershell
javac -d out src/common/*.java src/server/*.java src/client/*.java
```

Si no sale ningún mensaje, ha ido bien. Se creará una carpeta `out`.

> **Importante:** si alguien cambia el código, hay que volver a compilar. Además, si hay un servidor ya arrancado, hay que reiniciarlo para que use el código nuevo.

---

## Jugador 1 (anfitrión): arrancar el servidor y jugar

### 4. Averiguar tu IP

En PowerShell:

```powershell
ipconfig
```

Busca **Dirección IPv4** del adaptador que usas (Wi-Fi o Ethernet). Será algo como `192.168.1.35`. **Pásasela al Jugador 2.**

### 5. Permitir el puerto en el cortafuegos

Solo hace falta si el Jugador 2 juega desde **otro ordenador**. Abre PowerShell **como administrador** y ejecuta:

```powershell
netsh advfirewall firewall add rule name="TresEnRaya" dir=in action=allow protocol=TCP localport=5050
```

Si al arrancar el servidor Windows muestra una ventana de permisos de Java, pulsa **Permitir acceso**.

### 6. Arrancar el servidor

En la terminal 1:

```powershell
java -cp out server.Servidor 5050
```

Debe salir:

```
[servidor] escuchando en el puerto 5050
```

**Deja esta terminal abierta y no escribas en ella.** El servidor no lee el teclado: solo escucha y va mostrando mensajes.

### 7. Entrar como jugador

Abre **otra terminal** (botón `+` en VS Code) y ejecuta:

```powershell
java -cp out client.Cliente localhost 5050 TuNombre
```

Verás `Esperando al otro jugador...`. Tú eres la **X** y empiezas.

---

## Jugador 2 (invitado): conectarse

Haz primero la parte común (pasos 1, 2 y 3). Después:

### 4. Comprobar que llegas al servidor (recomendado)

Sustituye la IP por la que te dio el Jugador 1:

```powershell
Test-NetConnection 192.168.1.35 -Port 5050
```

Debe aparecer `TcpTestSucceeded : True`. Si sale `False`, mira la sección de problemas más abajo.

### 5. Conectarse

```powershell
java -cp out client.Cliente 192.168.1.35 5050 TuNombre
```

Sustituye `192.168.1.35` por la IP real del Jugador 1. Cuando te conectes, los dos recibiréis `START`. Tú eres la **O**.

> Si los dos jugáis en el **mismo ordenador**, usa `localhost` en lugar de la IP.

---

## Cómo se juega

Al empezar, cada uno ve el tablero vacío:

```
    0   1   2
0   . | . | .
   ---+---+---
1   . | . | .
   ---+---+---
2   . | . | .
```

Las filas y columnas van de **0 a 2**. Cuando salga `>>> Tu turno`, escribe la fila y la columna separadas por un espacio y pulsa Enter:

```
1 1
```

Eso marca el centro. También vale `1,1`.

| Escribes | Qué hace |
|---|---|
| `fila columna` (por ejemplo `0 2`) | Marca esa casilla |
| `r` | Pide revancha (solo al terminar la partida) |
| `q` | Sale del juego |

**Reglas**
- Empieza siempre la `X` (el primero que se conectó).
- Gana quien consiga tres en línea: horizontal, vertical o diagonal.
- Si se llena el tablero sin ganador, es empate.
- Si juegas fuera de turno, en una casilla ocupada o fuera del tablero, recibes `Jugada rechazada` y no pierdes el turno.
- Al terminar, los dos pueden escribir `r` para jugar otra vez. La revancha empieza cuando **los dos** la piden.
- Si uno sale con `q` o se cierra su ventana a mitad de partida, el otro gana por abandono.

---

## Orden recomendado (resumen)

1. Los dos instalan, clonan y compilan.
2. El anfitrión arranca el servidor.
3. El anfitrión entra como jugador y pasa su IP al invitado.
4. El invitado se conecta.
5. Juegan. Al acabar, `r` para revancha o `q` para salir.
6. El anfitrión para el servidor con `Ctrl+C` en su terminal.

---

## Si algo falla

| Mensaje o síntoma | Causa | Solución |
|---|---|---|
| `javac` no se reconoce | No está instalado el JDK, o falta en el `PATH` | Instala el JDK y abre una terminal nueva |
| `Address already in use: bind` | Ya hay un servidor arrancado en ese puerto | Cierra las terminales antiguas con `Ctrl+C`, o usa otro puerto en el servidor **y** en los clientes. Para ver quién lo usa: `netstat -ano \| findstr :5050` |
| `No se pudo conectar con 5050:5050` | Argumentos del cliente en orden incorrecto | El orden es **host, puerto, nombre**: `java -cp out client.Cliente localhost 5050 Ana` |
| `Network is unreachable` / `Connection timed out` | No hay ruta hasta el servidor | Comprueba que están en la misma red, que la IP es correcta y que el puerto está abierto en el cortafuegos |
| `Connection refused` | El servidor no está arrancado, o el puerto no coincide | Arranca el servidor y usa el mismo puerto en el cliente |
| `Partida llena` | Ya hay dos jugadores conectados | Esperad a que acabe, o reiniciad el servidor |
| La terminal del servidor no deja escribir | Es lo normal | El servidor no lee el teclado. Los jugadores van en **otras** terminales |
| Los cambios de código no se notan | El servidor sigue con las clases antiguas | Recompila y reinicia el servidor |
| `Test-NetConnection` da `False` | Cortafuegos o red que aísla equipos | Abre el puerto (paso 5 del anfitrión). Si es una wifi pública o de invitados, probad con el móvil como punto de acceso |

> En redes de centros educativos puede haber **aislamiento entre equipos**: aunque todo esté bien configurado, no se ven entre sí. Probad la conexión con antelación, antes de la demo.