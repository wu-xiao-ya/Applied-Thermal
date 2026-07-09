# Applied Thermal

Applied Thermal is a Cleanroom/Forge 1.12.2 addon for Applied Energistics 2 - Supergiant and Thermal Expansion.

It adds a Thermal machine augment that turns Thermal Expansion machines into AE2S pattern-provider hosts. The scope is intentionally limited to `TileMachineBase` machines; dynamos, devices, storage blocks, cells, and portable containers are not supported.

## Branches

Each supported Minecraft and AE implementation is maintained on its own long-lived branch. Code from different implementations must not be placed on the same branch.

| Branch | Minecraft | AE implementation | Status |
| --- | --- | --- | --- |
| `1.12.2-ae2s` | 1.12.2 | Applied Energistics 2 - Supergiant | Active |
| `1.12.2-ae2uel` | 1.12.2 | AE2 Unofficial Extended Life | Planned |

Future branches should follow `<minecraft-version>-<ae-implementation>` and append the mod loader when one Minecraft version needs separate Forge, Fabric, or NeoForge implementations.

## Usage

Install the Pattern Provider Augment into a Thermal Expansion machine. The machine exposes an AE2S grid node on adjacent sides and appears as a pattern provider when connected to an AE2S cable.

The Thermal machine GUI shows a small AE tab on the right. Click it to open the AE2S pattern-provider window for that Thermal machine. Processing patterns can be placed there; crafting patterns are rejected because Thermal machines are processing targets. The window uses AE2S' native capacity-card support, so the machine starts with 9 active pattern slots and gains 9 more active slots per installed pattern expansion card. Items in Thermal output slots are inserted back into the connected AE2S network by default.

Removing the Pattern Provider Augment ejects every stored pattern, provider upgrade card, pending input, and return-buffer item at the machine before its AE node is disabled.

## Build

Use Java 25:

```powershell
.\gradlew.bat --no-daemon build --stacktrace
```

CI runs the same build and uploads the generated jars.
