# Applied Thermal

Applied Thermal is a Minecraft 1.12.2 addon for AE2 Unofficial Extended Life and
Thermal Expansion.

It adds a Thermal machine augment that turns Thermal Expansion machines into
AE2UEL ME Interface hosts. The scope is intentionally limited to `TileMachineBase`
machines; dynamos, devices, storage blocks, cells, and portable containers are not
supported.

## Branches

Each supported Minecraft and AE implementation is maintained on its own long-lived
branch. Code from different implementations must not be placed on the same branch.

| Branch | Minecraft | AE implementation | Status |
| --- | --- | --- | --- |
| `1.12.2-ae2s` | 1.12.2 | Applied Energistics 2 - Supergiant | Active |
| `1.12.2-ae2uel` | 1.12.2 | AE2 Unofficial Extended Life | Active |

Future branches should follow `<minecraft-version>-<ae-implementation>` and append
the mod loader when one Minecraft version needs separate Forge, Fabric, or
NeoForge implementations.

## Installation

| Runtime | Required components |
| --- | --- |
| Cleanroom | Cleanroom 0.5.14, AE2UEL v0.56.7, Thermal Expansion 5.5.7 and its CoFH dependencies |
| Standard Forge | Forge 14.23.5.2859 or newer, MixinBooter 10.7, AE2UEL v0.56.7, Thermal Expansion 5.5.7 and its CoFH dependencies |

MixinBooter 10.7 is included by Cleanroom 0.5.14. It must be installed separately
when using standard Forge.

## Soft dependencies

| Dependency | Version | Role |
| --- | --- | --- |
| Flux_Applied | 1.0.0 | Enables FE charging from the AE network through the Flux Induction Card. |

Flux_Applied integration is loaded only when the `flux_applied` mod is present. Its
own required dependency, Terminal Interaction Integration 1.0, must also be installed.
Applied Thermal's pattern and interface features work without either optional mod.

## Usage

Install the **Pattern Provider Augment** into a Thermal Expansion machine. The machine
appears in the AE2UEL ME Interface Terminal alongside real interfaces and connects to
the adjacent AE2UEL network.

The Thermal machine GUI shows a small AE tab on the right. Click it to open the
AE2UEL ME Interface configuration window for that machine. Processing patterns can be
placed there; crafting patterns are rejected because Thermal machines are processing
targets. The window starts with 9 active pattern slots; each installed Capacity Card
adds 9 more. Items in Thermal output slots are inserted back into the connected
AE2UEL network by default.

Removing the Pattern Provider Augment ejects every stored pattern, AE upgrade card,
interface storage item, pending send item, and Flux Induction Card before its AE node
is disabled. Normal Thermal machine input and output slots remain owned by the machine.

## Build

Gradle runs on Java 25 and emits Java 8-compatible bytecode:

```powershell
.\gradlew.bat --no-daemon build --stacktrace
```

To include Flux_Applied 1.0.0 and Terminal Interaction Integration 1.0 in the local
development runtime:

```powershell
.\gradlew.bat --no-daemon build -PwithFluxApplied=true --stacktrace
```

CI runs the same build on Java 25 and uploads the runtime jar, sources jar, and
SHA-256 checksums.
