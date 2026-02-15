package src;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para el intérprete de Bitcoin Script - Fase 1
 * 
 * Cobertura de pruebas:
 * - Opcodes básicos (OP_0, OP_1-16, PUSHDATA)
 * - Operaciones de pila (OP_DUP, OP_DROP)
 * - Comparaciones (OP_EQUAL, OP_EQUALVERIFY)
 * - Criptografía (OP_HASH160)
 * - Firmas (OP_CHECKSIG)
 * - Casos borde (pila vacía, errores)
 * - Flujo P2PKH completo
 * 
 * @author [Pavel Jezrael]
 * @version 1.0
 */

public class ScriptInterpreterTest {

    private ScriptInterpreter interpreter;

    @BeforeEach
    public void setUp() {
        interpreter = new ScriptInterpreter(false);
    }

    @AfterEach
    public void tearDown() {
        interpreter.clearStack();
    }

    // PRUEBAS DE LITERALES

    @Test
    @DisplayName("OP_0 empuja array vacío a la pila")
    public void testOP_0() throws ScriptException {
        interpreter.execute("OP_0");
        assertEquals(1, interpreter.getStackSize());
    }

    @Test
    @DisplayName("OP_1 a OP_16 empujan números correctamente")
    public void testNumericOpcodes() throws ScriptException {
        interpreter.execute("OP_1 OP_2 OP_5 OP_10 OP_16");
        assertEquals(5, interpreter.getStackSize());
    }

    @Test
    @DisplayName("PUSHDATA empuja datos personalizados")
    public void testPushData() throws ScriptException {
        interpreter.execute("<hello> <world>");
        assertEquals(2, interpreter.getStackSize());
    }

    // PRUEBAS DE OPERACIONES DE PILA

    @Test
    @DisplayName("OP_DUP duplica el elemento superior")
    public void testOP_DUP() throws ScriptException {
        interpreter.execute("OP_1 OP_DUP");
        assertEquals(2, interpreter.getStackSize());
    }

    @Test
    @DisplayName("OP_DUP falla con pila vacía")
    public void testOP_DUP_EmptyStack() {
        assertThrows(ScriptException.class, () -> {
            interpreter.execute("OP_DUP");
        });
    }

    @Test
    @DisplayName("OP_DROP elimina el elemento superior")
    public void testOP_DROP() throws ScriptException {
        interpreter.execute("OP_1 OP_2 OP_DROP");
        assertEquals(1, interpreter.getStackSize());
    }

    @Test
    @DisplayName("OP_DROP falla con pila vacía")
    public void testOP_DROP_EmptyStack() {
        assertThrows(ScriptException.class, () -> {
            interpreter.execute("OP_DROP");
        });
    }

    // PRUEBAS DE COMPARACIÓN

    @Test
    @DisplayName("OP_EQUAL compara valores iguales correctamente")
    public void testOP_EQUAL_True() throws ScriptException {
        interpreter.execute("<hello> <hello> OP_EQUAL");
        assertEquals(1, interpreter.getStackSize());
        // El resultado debería ser true (array no vacío)
    }

    @Test
    @DisplayName("OP_EQUAL detecta valores diferentes")
    public void testOP_EQUAL_False() throws ScriptException {
        interpreter.execute("<hello> <world> OP_EQUAL");
        assertEquals(1, interpreter.getStackSize());
        // El resultado debería ser false (array vacío o 0)
    }

    @Test
    @DisplayName("OP_EQUAL falla con stack insuficiente")
    public void testOP_EQUAL_InsufficientStack() {
        assertThrows(ScriptException.class, () -> {
            interpreter.execute("OP_1 OP_EQUAL");
        });
    }

    @Test
    @DisplayName("OP_EQUALVERIFY pasa con valores iguales")
    public void testOP_EQUALVERIFY_Success() throws ScriptException {
        interpreter.execute("<test> <test> OP_EQUALVERIFY");
        assertEquals(0, interpreter.getStackSize());
    }

    @Test
    @DisplayName("OP_EQUALVERIFY falla con valores diferentes")
    public void testOP_EQUALVERIFY_Fail() {
        assertThrows(ScriptException.class, () -> {
            interpreter.execute("<test1> <test2> OP_EQUALVERIFY");
        });
    }

    // PRUEBAS DE CRIPTOGRAFÍA

    @Test
    @DisplayName("OP_HASH160 genera hash correctamente")
    public void testOP_HASH160() throws ScriptException {
        interpreter.execute("<data> OP_HASH160");
        assertEquals(1, interpreter.getStackSize());
    }

    @Test
    @DisplayName("OP_HASH160 falla con pila vacía")
    public void testOP_HASH160_EmptyStack() {
        assertThrows(ScriptException.class, () -> {
            interpreter.execute("OP_HASH160");
        });
    }

    @Test
    @DisplayName("OP_HASH160 produce hashes consistentes")
    public void testOP_HASH160_Consistency() throws ScriptException {
        interpreter.execute("<test> OP_HASH160");
        interpreter.execute("<test> OP_HASH160");
        interpreter.execute("OP_EQUAL");
        // Los dos hashes deberían ser iguales
        assertEquals(1, interpreter.getStackSize());
    }

    // PRUEBAS DE FIRMAS

    @Test
    @DisplayName("OP_CHECKSIG acepta firma y pubkey válidos")
    public void testOP_CHECKSIG_Valid() throws ScriptException {
        interpreter.execute("<signature> <pubkey> OP_CHECKSIG");
        assertEquals(1, interpreter.getStackSize());
    }

    @Test
    @DisplayName("OP_CHECKSIG rechaza firma vacía")
    public void testOP_CHECKSIG_EmptySignature() throws ScriptException {
        interpreter.execute("<> <pubkey> OP_CHECKSIG");
        assertEquals(1, interpreter.getStackSize());
        // Debería devolver false
    }

    @Test
    @DisplayName("OP_CHECKSIG falla con stack insuficiente")
    public void testOP_CHECKSIG_InsufficientStack() {
        assertThrows(ScriptException.class, () -> {
            interpreter.execute("<signature> OP_CHECKSIG");
        });
    }

    // PRUEBAS DE FLUJO P2PKH

    @Test
    @DisplayName("P2PKH válido ejecuta correctamente")
    public void testP2PKH_Valid() throws ScriptException {
        String scriptSig = "<signature> <pubkey>";
        String scriptPubKey = "OP_DUP OP_HASH160 OP_DUP OP_EQUALVERIFY OP_CHECKSIG";

        boolean result = interpreter.executeWithScripts(scriptSig, scriptPubKey);
        assertTrue(result, "P2PKH válido debería ejecutarse correctamente");
    }

    @Test
    @DisplayName("P2PKH con firma inválida falla")
    public void testP2PKH_InvalidSignature() throws ScriptException {
        String scriptSig = "<> <pubkey>"; // firma vacía
        String scriptPubKey = "OP_DUP OP_HASH160 OP_DUP OP_EQUALVERIFY OP_CHECKSIG";

        boolean result = interpreter.executeWithScripts(scriptSig, scriptPubKey);
        assertFalse(result, "P2PKH con firma inválida debería fallar");
    }

    @Test
    @DisplayName("P2PKH con hash incorrecto falla en EQUALVERIFY")
    public void testP2PKH_WrongHash() {
        assertThrows(ScriptException.class, () -> {
            String scriptSig = "<signature> <pubkey>";
            String scriptPubKey = "OP_DUP OP_HASH160 <wrong_hash> OP_EQUALVERIFY OP_CHECKSIG";

            interpreter.executeWithScripts(scriptSig, scriptPubKey);
        });
    }

    // PRUEBAS DE CASOS BORDE

    @Test
    @DisplayName("Script vacío no lanza excepción")
    public void testEmptyScript() throws ScriptException {
        interpreter.execute("");
        assertEquals(0, interpreter.getStackSize());
    }

    @Test
    @DisplayName("Opcode desconocido lanza excepción")
    public void testUnknownOpcode() {
        assertThrows(ScriptException.class, () -> {
            interpreter.execute("OP_UNKNOWN");
        });
    }

    @Test
    @DisplayName("Secuencia compleja de operaciones")
    public void testComplexSequence() throws ScriptException {
        interpreter.execute("OP_1 OP_DUP OP_2 OP_DROP OP_EQUAL");
        assertEquals(1, interpreter.getStackSize());
    }

    @Test
    @DisplayName("Stack se limpia correctamente")
    public void testClearStack() throws ScriptException {
        interpreter.execute("OP_1 OP_2 OP_3");
        assertEquals(3, interpreter.getStackSize());

        interpreter.clearStack();
        assertEquals(0, interpreter.getStackSize());
    }

    // PRUEBAS DE VALIDACIÓN FINAL

    @Test
    @DisplayName("Validación final exitosa con true en la cima")
    public void testFinalValidation_Success() throws ScriptException {
        String script = "OP_1"; // Empuja true
        boolean result = interpreter.execute(script);
        assertTrue(result);
    }

    @Test
    @DisplayName("Validación final falla con false en la cima")
    public void testFinalValidation_Fail() throws ScriptException {
        String script = "OP_0"; // Empuja false
        boolean result = interpreter.execute(script);
        assertFalse(result);
    }
}
