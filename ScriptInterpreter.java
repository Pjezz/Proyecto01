import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Intérprete simplificado de Bitcoin Script basado en pila.
 * 
 * Este programa permite ejecutar scripts de validación de transacciones
 * simulando el comportamiento del lenguaje Bitcoin Script.
 * 
 * Se implementan operaciones básicas de pila, lógica, aritmética,
 * comparaciones y control de flujo.
 * 
 * Nota: La verificación criptográfica es simulada (mock).
 * 
 * @author Autony Barrios
 * @version Fase 2 - 2026
 */
public class ScriptInterpreter {
    
    /** Pila principal de ejecución - usa Stack por eficiencia O(1) en operaciones básicas */
    private Stack<byte[]> mainStack;
    
    /** Modo trace para debugging */
    private boolean traceMode;
    
    /** Contador de instrucciones ejecutadas */
    private int instructionCount;

    private Stack<Boolean> executionStack = new Stack<>();
    
    /**
     * Constructor del intérprete
     * @param traceMode si true, imprime el estado tras cada instrucción
     */
    public ScriptInterpreter(boolean traceMode) {
        this.mainStack = new Stack<>();
        this.traceMode = traceMode;
        this.instructionCount = 0;
    }

    private boolean shouldExecute() {
        for (Boolean condition : executionStack) {
            if (!condition) return false;
        }   
        return true;
    }
    
    /**
     * Ejecuta un script completo (scriptSig + scriptPubKey)
     * 
     * @param scriptSig script de desbloqueo
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

        // Ejecutar scriptSig
        if (traceMode) System.out.println("--- Ejecutando scriptSig ---");
        execute(scriptSig);

        // Ejecutar scriptPubKey
        if (traceMode) System.out.println("\n--- Ejecutando scriptPubKey ---");
        execute(scriptPubKey);

        // 🔥 VALIDACIÓN FINAL SOLO AQUÍ
        if (mainStack.isEmpty()) {
            throw new ScriptException("Stack vacío al final de la ejecución", "FINAL");
        }

        return validateFinalStack();
    }
    
    /**
    * Ejecuta un script individual de Bitcoin Script.
    * 
    * El script se procesa de izquierda a derecha y cada token
    * es interpretado como un opcode o dato.
    * 
    * @param script instrucciones separadas por espacios
    * @return true si el resultado final es válido
    * @throws ScriptException si ocurre un error durante la ejecución
    */

    public boolean execute(String script) throws ScriptException {
        // Caso especial: script vacío
        if (script == null || script.trim().isEmpty()) {
            return true;
        }

    List<String> tokens = tokenize(script);

    int i = 0;
    while (i < tokens.size()) {
        String token = tokens.get(i);

        if (traceMode) {
            System.out.println("Instrucción #" + (++instructionCount) + ": " + token);
        }

        processToken(token);

        

        if (traceMode) {
            printStack();
            System.out.println();
        }

        i++;
    }
        if (mainStack.isEmpty()) {
            return true;
        }

        return validateFinalStack();
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
    * Procesa un token individual del script.
    * Puede ser un opcode o un valor de datos.
    * 
    * @param token elemento a interpretar
    * @throws ScriptException si el opcode no es válido
    */
    private void processToken(String token) throws ScriptException {

        if (!shouldExecute()) {
            if (token.equals("OP_IF") || token.equals("OP_NOTIF") || 
                token.equals("OP_ELSE") || token.equals("OP_ENDIF")) {
            // permitir que estos pasen
            }  else {
                return; // ignorar todo lo demás
            }
        }

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
            case "OP_ADD":
                 opAdd();
                break;
            case "OP_SUB":
                opSub();
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
            case "OP_SWAP":
                opSwap();
                break;
            case "OP_OVER":
                opOver();
                break;
            case "OP_NOT":
                opNot();
                break;
            case "OP_BOOLAND":
                opBoolAnd();
                break;
            case "OP_BOOLOR":
                opBoolOr();
                break;
            case "OP_LESSTHAN":
                opLessThan();
                break;
            case "OP_GREATERTHAN":
                opGreaterThan();
                break;
            case "OP_LESSTHANOREQUAL":
                opLessThanOrEqual();
                break;
            case "OP_GREATERTHANOREQUAL":
                opGreaterThanOrEqual();
                break;
            case "OP_NUMEQUALVERIFY":
                opNumEqualVerify();
                break;
            case "OP_IF":
                opIf();
                break;
            case "OP_ENDIF":
                opEndIf();
                break;
            case "OP_NOTIF":
                opNotIf();
                break;
            case "OP_ELSE":
                opElse();
                break;
            case "OP_VERIFY":
                opVerify();
                break;
            case "OP_RETURN":
                opReturn();
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
    // Intercambia los dos elementos superiores
    private void opSwap() throws ScriptException {
        if (mainStack.size() < 2) {
            throw new ScriptException("Stack insuficiente para OP_SWAP", "OP_SWAP");
        }

        byte[] a = mainStack.pop();
        byte[] b = mainStack.pop();

        mainStack.push(a);
        mainStack.push(b);
    }

    

    private void opOver() throws ScriptException {
        if (mainStack.size() < 2) {
            throw new ScriptException("Stack insuficiente para OP_OVER", "OP_OVER");
        }

        byte[] second = mainStack.get(mainStack.size() - 2);
        mainStack.push(Arrays.copyOf(second, second.length));
    }

    // Negación lógica
    private void opNot() throws ScriptException {
        if (mainStack.isEmpty()) {
            throw new ScriptException("Stack vacío para OP_NOT", "OP_NOT");
        }

        byte[] value = mainStack.pop();
        boolean result = !isTrue(value);

        mainStack.push(result ? new byte[]{1} : new byte[0]);
    }

    // AND lógico
    private void opBoolAnd() throws ScriptException {
        if (mainStack.size() < 2) {
            throw new ScriptException("Stack insuficiente para OP_BOOLAND", "OP_BOOLAND");
        }

        boolean a = isTrue(mainStack.pop());
        boolean b = isTrue(mainStack.pop());

        mainStack.push((a && b) ? new byte[]{1} : new byte[0]);
    }
    
    // OR lógico
    private void opBoolOr() throws ScriptException {
        if (mainStack.size() < 2) {
            throw new ScriptException("Stack insuficiente para OP_BOOLOR", "OP_BOOLOR");
        }

            boolean a = isTrue(mainStack.pop());
            boolean b = isTrue(mainStack.pop());

            mainStack.push((a || b) ? new byte[]{1} : new byte[0]);
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
        mainStack.push(equal ? new byte[]{1} : new byte[0]);
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
        
        mainStack.push(valid ? new byte[]{1} : new byte[0]);
    }

    private void opNotIf() throws ScriptException {
        if (mainStack.isEmpty()) {
            throw new ScriptException("Stack vacío para OP_NOTIF", "OP_NOTIF");
        }

        boolean condition = !isTrue(mainStack.pop());
        executionStack.push(condition);
    }
    /**
    * Invierte la condición del bloque IF actual.
    * 
    * Se utiliza dentro de estructuras IF/ELSE.
    */
    private void opElse() throws ScriptException {
        if (executionStack.isEmpty()) {
            throw new ScriptException("OP_ELSE sin OP_IF", "OP_ELSE");
        }

        boolean current = executionStack.pop();
        executionStack.push(!current);
    }

    /**
    * Verifica que el valor superior de la pila sea verdadero.
    * 
    * Si es falso, se lanza una excepción.
    */
    private void opVerify() throws ScriptException {
        if (mainStack.isEmpty()) {
            throw new ScriptException("Stack vacío para OP_VERIFY", "OP_VERIFY");
        }

        byte[] value = mainStack.pop();
        if (!isTrue(value)) {
            throw new ScriptException("OP_VERIFY falló", "OP_VERIFY");
        }
    }

    private void opReturn() throws ScriptException {
        throw new ScriptException("OP_RETURN ejecutado", "OP_RETURN");
    }
    
    /**
     * Verifica si un byte array representa un valor verdadero
     * En Bitcoin Script: 0 y array vacío son false, todo lo demás es true
     */
    private boolean isTrue(byte[] data) {
        if (data.length == 0) return false;
        
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
        if (num == 0) return new byte[0];
        
        List<Byte> result = new ArrayList<>();
        boolean negative = num < 0;
        num = Math.abs(num);
        
        while (num > 0) {
            result.add((byte)(num & 0xff));
            num >>= 8;
        }
        
        // Ajustar signo si es necesario
        if ((result.get(result.size() - 1) & 0x80) != 0) {
            result.add(negative ? (byte)0x80 : (byte)0x00);
        } else if (negative) {
            result.set(result.size() - 1, (byte)(result.get(result.size() - 1) | 0x80));
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
     * @param pubKey clave pública
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
        
        return isTrue(top);
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
        if (bytes.length == 0) return "0x00 (false)";
        
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
                sb.append((char)b);
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

    /**
    * Convierte un byte array a entero.
    * 
    * Nota: Implementación simplificada para el proyecto.
    */
    private int decodeNumber(byte[] data) {
        if (data.length == 0) return 0;

        int result = 0;

        for (int i = 0; i < data.length; i++) {
            result |= (data[i] & 0xff) << (8 * i);
        }

        // Revisar signo (último byte)
        if ((data[data.length - 1] & 0x80) != 0) {
            result &= ~(0x80 << (8 * (data.length - 1)));
            result = -result;
        }

        return result;
    }

    /**
    * Suma los dos valores superiores de la pila.
    * 
    * Nota: Implementación básica, no considera overflow.
    */
    private void opAdd() throws ScriptException {
        if (mainStack.size() < 2) {
            throw new ScriptException("Stack insuficiente para OP_ADD", "OP_ADD");
        }

        int a = decodeNumber(mainStack.pop());
        int b = decodeNumber(mainStack.pop());

        int result = a + b;

        mainStack.push(encodeNumber(result));
    }

    /**
    * Resta los dos valores superiores de la pila.
    * 
    * El orden es importante: segundo - primero.
    */
    private void opSub() throws ScriptException {
        if (mainStack.size() < 2) {
            throw new ScriptException("Stack insuficiente para OP_SUB", "OP_SUB");
        }

        int a = decodeNumber(mainStack.pop());
        int b = decodeNumber(mainStack.pop());

        int result = b - a;

        mainStack.push(encodeNumber(result));
    }

    private void opLessThan() throws ScriptException {
        if (mainStack.size() < 2) {
            throw new ScriptException("Stack insuficiente para OP_LESSTHAN", "OP_LESSTHAN");
        }

        int a = decodeNumber(mainStack.pop());
        int b = decodeNumber(mainStack.pop());

        mainStack.push((b < a) ? new byte[]{1} : new byte[0]);
    }

    private void opGreaterThan() throws ScriptException {
        if (mainStack.size() < 2) {
            throw new ScriptException("Stack insuficiente para OP_GREATERTHAN", "OP_GREATERTHAN");
        }

        int a = decodeNumber(mainStack.pop());
        int b = decodeNumber(mainStack.pop());

        mainStack.push((b > a) ? new byte[]{1} : new byte[0]);
    }

    private void opLessThanOrEqual() throws ScriptException {
        if (mainStack.size() < 2) {
            throw new ScriptException("Stack insuficiente para OP_LESSTHANOREQUAL", "OP_LESSTHANOREQUAL");
        }

        int a = decodeNumber(mainStack.pop());
        int b = decodeNumber(mainStack.pop());

        mainStack.push((b <= a) ? new byte[]{1} : new byte[0]);
    }

    private void opGreaterThanOrEqual() throws ScriptException {
        if (mainStack.size() < 2) {
            throw new ScriptException("Stack insuficiente para OP_GREATERTHANOREQUAL", "OP_GREATERTHANOREQUAL");
        }

        int a = decodeNumber(mainStack.pop());
        int b = decodeNumber(mainStack.pop());

        mainStack.push((b >= a) ? new byte[]{1} : new byte[0]);
    }

    private void opNumEqualVerify() throws ScriptException {
        if (mainStack.size() < 2) {
            throw new ScriptException("Stack insuficiente para OP_NUMEQUALVERIFY", "OP_NUMEQUALVERIFY");
        }

        int a = decodeNumber(mainStack.pop());
        int b = decodeNumber(mainStack.pop());

        if (a != b) {
            throw new ScriptException("OP_NUMEQUALVERIFY falló", "OP_NUMEQUALVERIFY");
        }
    }

    /**
    * Inicia un bloque condicional IF.
    * 
    * Evalúa el valor superior de la pila y decide si
    * se ejecutarán las instrucciones siguientes.
    */
    private void opIf() throws ScriptException {
        if (mainStack.isEmpty()) {
            throw new ScriptException("Stack vacío para OP_IF", "OP_IF");
        }

        boolean condition = isTrue(mainStack.pop());
        executionStack.push(condition);
    }

    private void opEndIf() throws ScriptException {
        if (executionStack.isEmpty()) {
            throw new ScriptException("OP_ENDIF sin OP_IF", "OP_ENDIF");
        }

        executionStack.pop();
    }


}
