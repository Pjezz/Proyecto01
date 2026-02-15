package src;

import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Intérprete de Bitcoin Script - Prototipo Fase 1
 * Implementa un subconjunto mínimo de opcodes para validación de transacciones
 * P2PKH
 * 
 * Estructuras de datos utilizadas:
 * - Stack<byte[]>: Pila principal para operaciones. O(1) para push/pop/peek.
 * Elegida por ser la estructura nativa de Bitcoin Script (stack-based
 * language).
 * - ArrayList<String>: Para tokenizar el script. O(1) acceso por índice.
 * 
 * @author [Pavel Jezrael]
 * @version 1.0
 */

public class ScriptInterpreter {

    /**
     * Pila principal de ejecución - usa Stack por eficiencia O(1) en operaciones
     * básicas
     */
    private Stack<byte[]> mainStack;

    /** Modo trace para debugging */
    private boolean traceMode;

    /** Contador de instrucciones ejecutadas */
    private int instructionCount;

    /**
     * Constructor del intérprete
     * 
     * @param traceMode si true, imprime el estado tras cada instrucción
     */
    public ScriptInterpreter(boolean traceMode) {
        this.mainStack = new Stack<>();
        this.traceMode = traceMode;
        this.instructionCount = 0;
    }

    /**
     * Ejecuta un script completo (scriptSig + scriptPubKey)
     * 
     * @param scriptSig    script de desbloqueo
     * @param scriptPubKey script de bloqueo
     * @return true si la validación es exitosa
     * @throws ScriptException si hay error en la ejecución
     */
    public boolean executeWithScripts(String scriptSig, String scriptPubKey) throws ScriptException {
        if (traceMode) {
            System.out.println("=== Iniciando ejecución de scripts ===");
            System.out.println("ScriptSig: " + scriptSig);
            System.out.println("ScriptPubKey: " + scriptPubKey);
            System.out.println();
        }

        // Ejecutar scriptSig primero
        if (traceMode)
            System.out.println("--- Ejecutando scriptSig ---");
        execute(scriptSig);

        // Luego ejecutar scriptPubKey
        if (traceMode)
            System.out.println("\n--- Ejecutando scriptPubKey ---");
        execute(scriptPubKey);

        // Validar resultado final
        return validateFinalStack();
    }

    /**
     * Ejecuta un script individual
     * 
     * @param script string con las instrucciones separadas por espacios
     * @return true si la ejecución fue exitosa
     * @throws ScriptException si hay error en la ejecución
     */
    public boolean execute(String script) throws ScriptException {
        // Tokenizar el script usando ArrayList para acceso O(1)
        List<String> tokens = tokenize(script);

        int i = 0;
        while (i < tokens.size()) {
            String token = tokens.get(i);

            if (traceMode) {
                System.out.println("Instrucción #" + (++instructionCount) + ": " + token);
            }

            // Procesar el token
            processToken(token);

            if (traceMode) {
                printStack();
                System.out.println();
            }

            i++;
        }

        return true;
    }

    /**
     * Tokeniza el script en instrucciones individuales
     * Usa ArrayList por su eficiencia en acceso secuencial O(1)
     * 
     * @param script script completo
     * @return lista de tokens
     */
    private List<String> tokenize(String script) {
        List<String> tokens = new ArrayList<>();
        String[] parts = script.trim().split("\\s+");

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];

            // Si es un dato entre <>, capturar todo el contenido
            if (part.startsWith("<")) {
                StringBuilder data = new StringBuilder(part);
                while (!part.endsWith(">") && i + 1 < parts.length) {
                    i++;
                    part = parts[i];
                    data.append(" ").append(part);
                }
                tokens.add(data.toString());
            } else {
                tokens.add(part);
            }
        }

        return tokens;
    }

    /**
     * Procesa un token individual (opcode o dato)
     * 
     * @param token token a procesar
     * @throws ScriptException si el opcode no es reconocido o falla
     */
    private void processToken(String token) throws ScriptException {
        // Datos entre <>
        if (token.startsWith("<") && token.endsWith(">")) {
            String data = token.substring(1, token.length() - 1);
            pushData(data.getBytes(StandardCharsets.UTF_8));
            return;
        }

        // Opcodes numéricos
        if (token.equals("OP_0") || token.equals("OP_FALSE")) {
            pushData(new byte[0]);
            return;
        }

        // OP_1 a OP_16
        if (token.matches("OP_([1-9]|1[0-6])")) {
            int num = Integer.parseInt(token.substring(3));
            pushData(encodeNumber(num));
            return;
        }

        // Opcodes de operación
        switch (token) {
            case "OP_DUP":
                opDup();
                break;
            case "OP_DROP":
                opDrop();
                break;
            case "OP_EQUAL":
                opEqual();
                break;
            case "OP_EQUALVERIFY":
                opEqualVerify();
                break;
            case "OP_HASH160":
                opHash160();
                break;
            case "OP_CHECKSIG":
                opCheckSig();
                break;
            default:
                throw new ScriptException("Opcode no reconocido: " + token, token);
        }
    }

    /**
     * OP_DUP: Duplica el elemento en la cima de la pila
     * Complejidad: O(1) - peek() y push() son O(1) en Stack
     */
    private void opDup() throws ScriptException {
        if (mainStack.isEmpty()) {
            throw new ScriptException("Stack vacío para OP_DUP", "OP_DUP");
        }
        byte[] top = mainStack.peek();
        mainStack.push(Arrays.copyOf(top, top.length));
    }

    /**
     * OP_DROP: Elimina el elemento en la cima de la pila
     * Complejidad: O(1) - pop() es O(1) en Stack
     */
    private void opDrop() throws ScriptException {
        if (mainStack.isEmpty()) {
            throw new ScriptException("Stack vacío para OP_DROP", "OP_DROP");
        }
        mainStack.pop();
    }

    /**
     * OP_EQUAL: Compara los dos elementos superiores y empuja true/false
     * Complejidad: O(n) donde n es el tamaño de los arrays a comparar
     */
    private void opEqual() throws ScriptException {
        if (mainStack.size() < 2) {
            throw new ScriptException("Stack insuficiente para OP_EQUAL", "OP_EQUAL");
        }

        byte[] a = mainStack.pop();
        byte[] b = mainStack.pop();

        boolean equal = Arrays.equals(a, b);
        mainStack.push(equal ? new byte[] { 1 } : new byte[0]);
    }

    /**
     * OP_EQUALVERIFY: Igual que OP_EQUAL pero falla si no son iguales
     * Complejidad: O(n) donde n es el tamaño de los arrays
     */
    private void opEqualVerify() throws ScriptException {
        opEqual();

        byte[] result = mainStack.pop();
        if (!isTrue(result)) {
            throw new ScriptException("OP_EQUALVERIFY falló: valores no iguales", "OP_EQUALVERIFY");
        }
    }

    /**
     * OP_HASH160: SHA-256 seguido de RIPEMD-160
     * Complejidad: O(n) donde n es el tamaño del dato a hashear
     */
    private void opHash160() throws ScriptException {
        if (mainStack.isEmpty()) {
            throw new ScriptException("Stack vacío para OP_HASH160", "OP_HASH160");
        }

        byte[] data = mainStack.pop();
        byte[] hash = hash160(data);
        mainStack.push(hash);
    }

    /**
     * OP_CHECKSIG: Verifica firma (simulada en esta fase)
     * Complejidad: O(1) - verificación mock
     */
    private void opCheckSig() throws ScriptException {
        if (mainStack.size() < 2) {
            throw new ScriptException("Stack insuficiente para OP_CHECKSIG", "OP_CHECKSIG");
        }

        byte[] pubKey = mainStack.pop();
        byte[] signature = mainStack.pop();

        // Verificación simulada (mock)
        boolean valid = verifySignatureMock(signature, pubKey);

        mainStack.push(valid ? new byte[] { 1 } : new byte[0]);
    }

    /**
     * Verifica si un byte array representa un valor verdadero
     * En Bitcoin Script: 0 y array vacío son false, todo lo demás es true
     */
    private boolean isTrue(byte[] data) {
        if (data.length == 0)
            return false;

        for (int i = 0; i < data.length; i++) {
            if (data[i] != 0) {
                // Verificar que no sea negative zero
                if (i == data.length - 1 && data[i] == 0x80) {
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Empuja datos a la pila
     * Complejidad: O(1) - push() es O(1) en Stack
     */
    private void pushData(byte[] data) {
        mainStack.push(data);
    }

    /**
     * Codifica un número como byte array (formato Bitcoin Script)
     */
    private byte[] encodeNumber(int num) {
        if (num == 0)
            return new byte[0];

        List<Byte> result = new ArrayList<>();
        boolean negative = num < 0;
        num = Math.abs(num);

        while (num > 0) {
            result.add((byte) (num & 0xff));
            num >>= 8;
        }

        // Ajustar signo si es necesario
        if ((result.get(result.size() - 1) & 0x80) != 0) {
            result.add(negative ? (byte) 0x80 : (byte) 0x00);
        } else if (negative) {
            result.set(result.size() - 1, (byte) (result.get(result.size() - 1) | 0x80));
        }

        byte[] array = new byte[result.size()];
        for (int i = 0; i < result.size(); i++) {
            array[i] = result.get(i);
        }
        return array;
    }

    /**
     * Calcula HASH160 (SHA-256 + RIPEMD-160)
     * Para la Fase 1, usamos solo SHA-256 como aproximación
     */
    private byte[] hash160(byte[] data) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] hash = sha256.digest(data);
            // En producción, aquí se aplicaría RIPEMD-160
            // Por ahora tomamos los primeros 20 bytes del SHA-256
            return Arrays.copyOf(hash, 20);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 no disponible", e);
        }
    }

    /**
     * Verificación de firma simulada (mock)
     * En la Fase 1, aceptamos cualquier firma como válida
     * 
     * @param signature firma a verificar
     * @param pubKey    clave pública
     * @return siempre true en esta fase (mock)
     */
    private boolean verifySignatureMock(byte[] signature, byte[] pubKey) {
        // Mock: verificación simulada
        // En una implementación real, aquí se verificaría la firma ECDSA

        // Para la fase 1, consideramos válida si ambos arrays no están vacíos
        return signature.length > 0 && pubKey.length > 0;
    }

    /**
     * Valida que el estado final de la pila sea correcto
     * La pila debe tener exactamente un elemento y debe ser true
     */
    private boolean validateFinalStack() throws ScriptException {
        if (mainStack.isEmpty()) {
            throw new ScriptException("Stack vacío al final de la ejecución", "FINAL");
        }

        if (mainStack.size() > 1) {
            if (traceMode) {
                System.out.println("ADVERTENCIA: Stack tiene " + mainStack.size() + " elementos (se esperaba 1)");
            }
        }

        byte[] top = mainStack.peek();
        boolean result = isTrue(top);

        if (traceMode) {
            System.out.println("\n=== Resultado Final ===");
            System.out.println("Stack top: " + bytesToHex(top));
            System.out.println("Validación: " + (result ? "EXITOSA ✓" : "FALLIDA ✗"));
        }

        return result;
    }

    /**
     * Imprime el estado actual de la pila (para modo trace)
     */
    private void printStack() {
        System.out.println("Stack (" + mainStack.size() + " elementos):");
        if (mainStack.isEmpty()) {
            System.out.println("  [vacío]");
            return;
        }

        for (int i = mainStack.size() - 1; i >= 0; i--) {
            byte[] element = mainStack.get(i);
            String hex = bytesToHex(element);
            String ascii = bytesToAscii(element);
            System.out.printf("  [%d] %s (%s)%n", i, hex, ascii);
        }
    }

    /**
     * Convierte byte array a hexadecimal
     */
    private String bytesToHex(byte[] bytes) {
        if (bytes.length == 0)
            return "0x00 (false)";

        StringBuilder sb = new StringBuilder("0x");
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Convierte byte array a ASCII (si es imprimible)
     */
    private String bytesToAscii(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            if (b >= 32 && b < 127) {
                sb.append((char) b);
            } else {
                sb.append('.');
            }
        }
        return sb.length() > 0 ? "\"" + sb.toString() + "\"" : "";
    }

    /**
     * Obtiene el tamaño actual de la pila
     */
    public int getStackSize() {
        return mainStack.size();
    }

    /**
     * Limpia la pila
     */
    public void clearStack() {
        mainStack.clear();
        instructionCount = 0;
    }
}
