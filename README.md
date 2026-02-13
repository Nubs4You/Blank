# BuildBot Mod (Fabric 1.21.11, Maven)

BuildBot is a Fabric mod that spawns a visible **Builder Bot** when a player runs:

```text
/buildbot <your large build prompt>
```

The mod generates a structured block-placement plan through a configurable LLM endpoint, then executes the build progressively each tick.

## Features
- Player command-based build requests.
- In-world bot marker (armor stand) that moves as it builds.
- LLM planning with strict JSON schema expectations.
- Fallback procedural plan generation when LLM is unavailable.
- Configurable throughput (`blocksPerTick`) and radius limit (`maxRadius`).

## Configuration
On first run, the mod writes:

`config/buildbotmod.properties`

Set values like:
- `llmEndpoint`
- `llmApiKey`
- `llmModel`
- `blocksPerTick`
- `maxRadius`

## LLM Response Contract
The planner expects instruction JSON in the assistant message content:

```json
{
  "summary": "Large medieval castle",
  "instructions": [
    {"x": 0, "y": 0, "z": 0, "block": "minecraft:stone_bricks"}
  ]
}
```

Coordinates are relative to bot origin near the player.

## Notes
- This repository follows a Maven layout per request.
- Fabric development is traditionally Gradle-based; additional runtime/remapping setup may be required in a full production environment.
