# Tiled Map Editor: Custom Properties Guide

This document serves as a reference for level designers, listing all custom properties that can be set within the Tiled editor to control the behavior of game objects, entities, and the world itself.

---

## Object-Level Properties

These properties are set directly on objects within various Object Layers.

### Layers: `playerStart` & `enemies`

Objects on these layers are spawned as dynamic characters in the game. The most important property is **`type`**, which maps to a predefined `SpawnCfg` configuration in the code.

| Property | Data Type | Description | Example Value |
| :--- | :--- | :--- | :--- |
| `type` | `String` | **(Required)** Determines the template (`SpawnCfg`) used to create the entity. Must match a predefined type. | `playerStart` or `enemy` |

### Layer: `bgMapObjects`

These are static or animated objects that are added to the game world.

| Property | Data Type | Description | Example Value |
| :--- | :--- | :--- | :--- |
| `zIndex` | `Integer` | *Optional.* Controls the rendering order. Objects with a higher `zIndex` are drawn on top of those with a lower value. | `10` |
| `sound` | `String` | *Optional.* Attaches a looping sound source to the object. The value must correspond to a `SoundType` enum (e.g., `CAMPFIRE`). | `CAMPFIRE` |
| `soundVolume` | `Float` | *Optional.* Sets the volume of the sound (default: `0.5`). | `0.8` |
| `soundAttenuationMaxDistance` | `Float` | *Optional.* The maximum distance at which the sound is audible (default: `10.0`). | `15.0` |
| `soundAttenuationMinDistance`| `Float` | *Optional.* The distance at which the sound begins to fade (default: `1.0`). | `2.0` |
| `soundAttenuationFactor` | `Float` | *Optional.* How quickly the sound fades with distance (default: `1.0`). | `1.5` |
| `type` | `String` | *Optional.* A special identifier for additional logic. Currently, `fire` is used to automatically add a fire particle effect. | `fire` |

### Layer: `collisionBoxes`

Rectangles on this layer define the static, solid collision bodies of the world.

| Property | Data Type | Description | Example Value |
| :--- | :--- | :--- | :--- |
| `floorType` | `String` | *Optional.* Defines the surface type for footstep sounds. Must correspond to a `FloorType` enum. | `WOOD` or `STONE` |

### Layer: `audioZones`

Rectangles on this layer define zones that trigger audio effects when the player enters them.

| Property | Data Type | Description | Example Value |
| :--- | :--- | :--- |:--------------|
| `effect` | `String` | **(Required)** The name of the EAX Reverb effect to activate. | `EAX_REVERB`  |
| `preset` | `String` | *Optional.* A specific preset for the effect. | `small_room`  |
| `intensity` | `Float` | *Optional.* The intensity of the effect. | `0.7`         |

### Layer: `lights`

Objects (usually points or rectangles) on this layer create dynamic light sources in the world.

| Property | Data Type | Description | Example Value |
| :--- | :--- | :--- | :--- |
| `type` | `Integer` | **(Required)** The type of light. `0` for `POINT_LIGHT`, `1` for `SPOT_LIGHT`. | `0` |
| `color` | `Color` | **(Required)** The color of the light. | `#FF5733` |
| `initialIntensity` | `Float` | *Optional.* The brightness of the light (default: `1.0`). | `1.5` |
| `distance` | `Float` | *Optional.* The range of the physical Box2D light (default: `1.0`). | `15.0` |
| `falloffProfile` | `Float` | *Optional.* The falloff profile of the light (default: `0.5`). | `0.8` |
| `shaderIntensityMultiplier`| `Float` | *Optional.* A multiplier for the visual brightness of the shader (default: `0.5`). | `1.2` |
| `isManaged` | `Boolean` | *Optional.* Whether the light should be managed automatically by the light engine (default: `true`). | `false` |
| `direction` | `Float` | *Spotlights only.* The direction of the light cone in degrees (default: `-90.0`, i.e., down). | `0.0` |
| `coneDegree` | `Float` | *Spotlights only.* The width of the light cone in degrees (default: `50.0`). | `35.0` |
| `effect` | `Integer` | *Optional.* Adds a visual effect to the light (e.g., pulsing). Must correspond to a `LightEffectType` enum. | `0` (for PULSE) |

---

## Map-Level Properties

These properties are set at the top level of the Tiled map (in the `Properties` panel for the map itself).

| Property | Data Type | Description                                                                        | Example Value |
| :--- | :--- |:-----------------------------------------------------------------------------------| :--- |
| `bgMusic` | `String` | The path to the background music file that should be played when the map loads. | `music/level_1_theme.mp3` |


## RenderLayer convention (zIndex)

These values define the rendering order of scene elements. A higher `zIndex` means the element appears visually in front.

| RenderLayer        | zIndex | Description                              |
|--------------------|--------|------------------------------------------|
| BG_SKY             | 0      | Deepest background – e.g., stars         |
| SKY                | 1000   | Sky layers, such as clouds, sun, moon    |
| BG_PARALLAX_1      | 2000   | Far distant parallax layer               |
| BG_PARALLAX_2      | 3000   | Distant parallax layer                   |
| BG_PARALLAX_3      | 4000   | Closer parallax layer                    |
| BG_PARALLAX_4      | 5000   | Foremost background parallax             |
| MAP_BG             | 6000   | Tile map background layers               |
| TILES              | 7000   | Main gameplay tile layer                 |
| BG_MAP_OBJECTS     | 8000   | Objects behind characters                |
| CHARACTERS         | 9000   | Players, enemies, NPCs                   |
| PROJECTILES        | 9500   | Projectiles, spells, ranged attacks      |
| FG_MAP_OBJECTS     | 10000  | Foreground map decorations               |
| FG_PARALLAX_1      | 11000  | Foreground parallax layer (e.g., leaves) |
| FG_PARALLAX_2      | 12000  | Closest parallax elements                |
| UI                 | 20000  | User interface elements                  |
