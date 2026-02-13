package com.example.buildbotmod.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public final class BuildInstruction {
    private int x;
    private int y;
    private int z;
    private String blockId;
}
