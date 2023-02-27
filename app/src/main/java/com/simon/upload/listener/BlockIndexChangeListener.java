package com.simon.upload.listener;

import com.simon.upload.event.BlockIndexChangeEvent;

import java.util.EventListener;

public interface BlockIndexChangeListener extends EventListener {
    void onBlockIndexChangeEvent(BlockIndexChangeEvent event);
}
