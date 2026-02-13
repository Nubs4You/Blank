package com.example.buildbotmod.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public final class BuildPlan {
    private String summary;
    private List<BuildInstruction> instructions = new ArrayList<>();
}
