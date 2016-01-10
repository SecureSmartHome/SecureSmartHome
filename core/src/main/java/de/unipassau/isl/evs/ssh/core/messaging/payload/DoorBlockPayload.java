package de.unipassau.isl.evs.ssh.core.messaging.payload;

/**
 * @author Leon Sell
 */
public class DoorBlockPayload extends DoorPayload {
    private boolean block;

    public DoorBlockPayload(boolean block, String moduleName) {
        super(moduleName);
        this.block = block;
    }

    public boolean isBlock() {
        return block;
    }
}
