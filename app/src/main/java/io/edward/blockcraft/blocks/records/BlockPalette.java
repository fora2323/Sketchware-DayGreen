package io.edward.blockcraft.blocks.records;

import java.util.ArrayList;

import ma.swblockeditor.core.BlockBean;

public record BlockPalette(String name, int color, ArrayList<BlockBean> blocks) {
    public BlockPalette(String name, int color) {
        this(name, color, new ArrayList<>());
    }
}
