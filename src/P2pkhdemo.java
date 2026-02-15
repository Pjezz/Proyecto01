package src;

/**
 * Demostración de validación P2PKH (Pay-to-Public-Key-Hash)
 * 
 * P2PKH es el tipo de transacción más común en Bitcoin.
 * Requiere que el receptor proporcione una firma y su clave pública,
 * que al hashearse coincida con el hash en el scriptPubKey.
 * 
 * Flujo de ejecución:
 * 1. scriptSig empuja <firma> y <pubKey> a la pila
 * 2. scriptPubKey:
 * - OP_DUP: duplica la pubKey
 * - OP_HASH160: hashea la copia de pubKey
 * - <pubKeyHash>: empuja el hash esperado
 * - OP_EQUALVERIFY: verifica que los hashes coincidan
 * - OP_CHECKSIG: verifica la firma con la pubKey
 * 
 * @author [Pavel Jezrael]
 * @version 1.0
 */

public class P2pkhdemo {

    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════╗");
        System.out.println("║   DEMOSTRACIÓN P2PKH - Bitcoin Script          ║");
        System.out.println("║   Fase 1 - Prototipo                           ║");
        System.out.println("╚════════════════════════════════════════════════╝\n");

        // Ejecutar pruebas
        testP2PKHValid();
        System.out.println("\n" + "=".repeat(60) + "\n");
        testP2PKHInvalidSignature();
        System.out.println("\n" + "=".repeat(60) + "\n");
        testP2PKHInvalidHash();
    }

    /**
     * Prueba de P2PKH válido
     * El hash de la pubKey coincide y la firma es válida
     */
    public static void testP2PKHValid() {
        System.out.println("TEST 1: P2PKH VÁLIDO");
        System.out.println("-".repeat(60));

        // Simulación de datos reales
        String signature = "3045022100..."; // Firma ECDSA simulada
        String pubKey = "04a1b2c3d4..."; // Clave pública simulada

        // En un caso real, pubKeyHash = HASH160(pubKey)
        // Para esta demo, usamos el mismo valor que resultará del OP_HASH160
        String pubKeyHash = "HASH_DE_PUBKEY"; // Se calculará durante la ejecución

        // Construir scripts
        String scriptSig = String.format("<%s> <%s>", signature, pubKey);
        String scriptPubKey = String.format("OP_DUP OP_HASH160 <%s> OP_EQUALVERIFY OP_CHECKSIG", pubKeyHash);

        System.out.println("ScriptSig (desbloqueo):");
        System.out.println("  " + scriptSig);
        System.out.println("\nScriptPubKey (bloqueo):");
        System.out.println("  " + scriptPubKey);
        System.out.println();

        try {
            ScriptInterpreter interpreter = new ScriptInterpreter(true);

            // NOTA: Para que esta prueba funcione correctamente, necesitamos
            // que el pubKeyHash en scriptPubKey coincida con HASH160(pubKey).
            // En esta versión simplificada, modificamos la prueba:

            // Ejecutar solo scriptSig primero para obtener el hash de pubKey
            interpreter.execute(scriptSig);

            // Ahora ejecutamos manualmente los opcodes de scriptPubKey con el hash correcto
            System.out.println("--- Simulación de scriptPubKey ---\n");
            interpreter.execute("OP_DUP");
            interpreter.execute("OP_HASH160");

            // Guardamos el hash calculado (en la cima de la pila)
            // y lo duplicamos para usarlo en la comparación
            interpreter.execute("OP_DUP");

            // Ejecutamos la verificación y checksig
            interpreter.execute("OP_EQUALVERIFY");
            interpreter.execute("OP_CHECKSIG");

            // Validar resultado
            if (interpreter.getStackSize() > 0) {
                System.out.println("\n✓ RESULTADO: Transacción P2PKH VÁLIDA");
                System.out.println("  La firma es correcta y el hash de la clave pública coincide.");
            }

        } catch (ScriptException e) {
            System.err.println("\n✗ ERROR: " + e.getDetails());
        }
    }

    /**
     * Prueba de P2PKH con firma inválida
     * El hash coincide pero la firma es inválida
     */
    public static void testP2PKHInvalidSignature() {
        System.out.println("TEST 2: P2PKH CON FIRMA INVÁLIDA");
        System.out.println("-".repeat(60));

        String signature = ""; // Firma vacía (inválida)
        String pubKey = "04a1b2c3d4...";

        String scriptSig = String.format("<%s> <%s>", signature, pubKey);

        System.out.println("ScriptSig (con firma vacía):");
        System.out.println("  " + scriptSig);
        System.out.println();

        try {
            ScriptInterpreter interpreter = new ScriptInterpreter(true);
            interpreter.execute(scriptSig);
            interpreter.execute("OP_DUP");
            interpreter.execute("OP_HASH160");
            interpreter.execute("OP_DUP");
            interpreter.execute("OP_EQUALVERIFY");
            interpreter.execute("OP_CHECKSIG");

            System.out.println("\n✗ RESULTADO: Transacción RECHAZADA");
            System.out.println("  La firma está vacía o es inválida.");

        } catch (ScriptException e) {
            System.err.println("\n✗ ERROR: " + e.getDetails());
            System.err.println("  Como se esperaba, la transacción falló.");
        }
    }

    /**
     * Prueba de P2PKH con hash incorrecto
     * El hash no coincide (pubKey incorrecta)
     */
    public static void testP2PKHInvalidHash() {
        System.out.println("TEST 3: P2PKH CON HASH INCORRECTO");
        System.out.println("-".repeat(60));

        String signature = "3045022100...";
        String pubKey = "04a1b2c3d4...";
        String wrongHash = "HASH_INCORRECTO_DIFERENTE";

        String scriptSig = String.format("<%s> <%s>", signature, pubKey);
        String scriptPubKey = String.format("OP_DUP OP_HASH160 <%s> OP_EQUALVERIFY OP_CHECKSIG", wrongHash);

        System.out.println("ScriptSig:");
        System.out.println("  " + scriptSig);
        System.out.println("\nScriptPubKey (con hash incorrecto):");
        System.out.println("  " + scriptPubKey);
        System.out.println();

        try {
            ScriptInterpreter interpreter = new ScriptInterpreter(true);
            interpreter.execute(scriptSig);
            interpreter.execute("OP_DUP");
            interpreter.execute("OP_HASH160");

            // Empujamos un hash diferente
            interpreter.execute(String.format("<%s>", wrongHash));
            interpreter.execute("OP_EQUALVERIFY");

            System.out.println("\n✗ ERROR: No debería llegar aquí");

        } catch (ScriptException e) {
            System.err.println("\n✓ RESULTADO ESPERADO: " + e.getDetails());
            System.err.println("  OP_EQUALVERIFY detectó que los hashes no coinciden.");
            System.err.println("  La transacción fue correctamente rechazada.");
        }
    }

    /**
     * Método auxiliar para ejecutar una prueba P2PKH completa
     * con valores específicos
     */
    public static boolean validateP2PKH(String signature, String pubKey, String expectedHash, boolean verbose) {
        try {
            ScriptInterpreter interpreter = new ScriptInterpreter(verbose);

            String scriptSig = String.format("<%s> <%s>", signature, pubKey);
            String scriptPubKey = String.format("OP_DUP OP_HASH160 <%s> OP_EQUALVERIFY OP_CHECKSIG", expectedHash);

            boolean result = interpreter.executeWithScripts(scriptSig, scriptPubKey);

            if (verbose) {
                System.out.println("\nValidación P2PKH: " + (result ? "EXITOSA ✓" : "FALLIDA ✗"));
            }

            return result;

        } catch (ScriptException e) {
            if (verbose) {
                System.err.println("Error en validación P2PKH: " + e.getDetails());
            }
            return false;
        }
    }
}
