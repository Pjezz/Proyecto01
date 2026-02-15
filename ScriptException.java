package bitcoin.script;

/**
 * Excepción personalizada para errores en la ejecución de Bitcoin Script
 * 
 * @author Grupo #[X]
 * @version Fase 1 - Febrero 2026
 */
public class ScriptException extends Exception {
    
    /** Opcode que causó la excepción */
    private String opcode;
    
    /**
     * Constructor con mensaje y opcode
     * 
     * @param message descripción del error
     * @param opcode opcode que causó el error
     */
    public ScriptException(String message, String opcode) {
        super(message);
        this.opcode = opcode;
    }
    
    /**
     * Constructor solo con mensaje
     * 
     * @param message descripción del error
     */
    public ScriptException(String message) {
        super(message);
        this.opcode = "UNKNOWN";
    }
    
    /**
     * Obtiene el opcode que causó la excepción
     * 
     * @return opcode
     */
    public String getOpcode() {
        return opcode;
    }
    
    /**
     * Obtiene detalles completos del error
     * 
     * @return string con detalles
     */
    public String getDetails() {
        return String.format("Error en %s: %s", opcode, getMessage());
    }
    
    @Override
    public String toString() {
        return "ScriptException: " + getDetails();
    }
}
