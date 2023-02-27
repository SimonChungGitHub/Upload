package com.simon.upload.event;

import java.util.EventObject;

public class BlockIndexChangeEvent extends EventObject {
    /**
     * Constructs a prototypical Event.
     *
     * @param source the object on which the Event initially occurred
     * @throws IllegalArgumentException if source is null
     */
    public BlockIndexChangeEvent(Object source) {
        super(source);
    }
}
