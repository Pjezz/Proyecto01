public class Main {

    public static void main(String[] args) {
        ScriptInterpreter interpreter = new ScriptInterpreter(true); // trace activado

        try {

            // ===============================
            // DEMO 1: P2PKH (válido)
            // ===============================
            System.out.println("=== DEMO P2PKH VALIDO ===");

            String scriptSig = "<signature> <pubkey>";
            String scriptPubKey = "OP_DUP OP_HASH160 OP_DUP OP_EQUALVERIFY OP_CHECKSIG";

            boolean result = interpreter.executeWithScripts(scriptSig, scriptPubKey);
            System.out.println("Resultado: " + result);

            // limpiar pila para siguiente demo
            interpreter.clearStack();

            // ===============================
            // DEMO 2: IF / ELSE
            // ===============================
            System.out.println("\n=== DEMO IF TRUE ===");
            interpreter.execute("OP_1 OP_IF OP_2 OP_ELSE OP_3 OP_ENDIF");

            interpreter.clearStack();

            System.out.println("\n=== DEMO IF FALSE ===");
            interpreter.execute("OP_0 OP_IF OP_2 OP_ELSE OP_3 OP_ENDIF");

            interpreter.clearStack();

            // ===============================
            // DEMO 3: OP_RETURN
            // ===============================
            System.out.println("\n=== DEMO OP_RETURN ===");
            interpreter.execute("OP_RETURN");

        } catch (ScriptException e) {
            System.out.println("Error: " + e.getDetails());
        }
    }
}