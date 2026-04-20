# NeoFavoriteItems Architecture Design

## Goals

This document defines the target architecture for `NeoFavoriteItems`.

The design is based on three inputs:

1. The current repository state in this workspace.
2. The desired product direction: a cleaner and more extensible "favorite/lock inventory slot" mod.
3. Lessons learned from `Yuyears/SlotLockForked` on the `arch/1.21` branch, especially its working Fabric implementation.

The purpose is **not** to clone the reference implementation. The purpose is to preserve the good ideas while improving:

- separation of concerns
- platform compatibility
- long-term maintainability
- correctness across different container GUIs
- future feature extensibility

## Current Problems In This Repository

The current codebase already has a useful skeleton, but several responsibilities are still coupled too tightly:

- configuration, state, persistence, rendering, and behavior interception are only partially separated
- `FavoritesManager` currently stores only a single in-memory slot set and is not modeled as a reusable domain service
- interaction blocking is not yet implemented, so the existing structure has not been tested against real inventory edge cases
- overlay rendering exists, but it is still directly coupled to raw slot ids and platform code
- data persistence is present, but it is still too close to global singleton state
- there is no explicit slot mapping layer to reconcile "GUI slot index" and "player inventory logical slot index"

## Core Design Principles

### 1. Domain first

Core rules should live in platform-agnostic Java classes and should not depend on Fabric/Forge/NeoForge event APIs.

### 2. Thin mixins and event hooks

Mixins and platform event listeners should gather context, call services, and then cancel or continue. They should not own business rules.

### 3. Server authoritative, client responsive

The client should provide local feedback and rendering, but server-side state should remain the final authority whenever the server mod is present.

### 4. Stable logical slot model

All slot decisions should operate on a unified logical player inventory index, not raw screen/menu slot positions.

### 5. Explicit contexts

Operations should be evaluated against a clear context:

- which player
- which world/server scope
- which logical slot
- which interaction type
- whether bypass is active
- whether the action comes from client preview or server validation

### 6. Progressive capability

The architecture should support three modes cleanly:

- client-only local persistence
- integrated singleplayer with full authority
- dedicated server authority with client sync

## Target Layering

The project should evolve toward five layers.

### A. Domain layer

Pure business model and rules. No loader-specific code.

Suggested package:

- `mycraft.yuyears.newitemfavorites.domain`

Suggested contents:

- `FavoriteState`
- `FavoriteSlotSet`
- `LogicalSlotIndex`
- `FavoriteMode`
- `InteractionType`
- `InteractionDecision`
- `OverlayDescriptor`
- `SlotMovePolicy`
- `BypassState`

This layer answers questions like:

- is this logical slot favorited?
- should this interaction be blocked?
- what should happen when an item moves?
- should an empty favorited slot auto-unlock?
- what overlay should be shown for the current state?

### B. Application layer

Use-case oriented services that coordinate domain rules.

Suggested package:

- `mycraft.yuyears.newitemfavorites.application`

Suggested services:

- `FavoriteStateService`
- `InteractionGuardService`
- `SlotTransferService`
- `OverlayPresentationService`
- `FavoritePersistenceService`
- `FavoriteSyncService`

Responsibilities:

- toggle favorite state
- evaluate interaction requests
- apply move/swap/empty-slot transitions
- expose UI-facing overlay information
- coordinate load/save/sync actions

This layer should depend on interfaces, not on platform event code.

### C. Infrastructure layer

Persistence, config loading, network packet codecs, and adapters for environment-specific storage.

Suggested package:

- `mycraft.yuyears.newitemfavorites.infrastructure`

Suggested subpackages:

- `config`
- `persistence`
- `network`
- `logging`

Suggested interfaces:

- `FavoriteRepository`
- `FavoriteSyncGateway`
- `ConfigGateway`

Suggested implementations:

- `TomlConfigGateway`
- `ClientFileFavoriteRepository`
- `WorldSaveFavoriteRepository`
- `ServerFavoriteRepository`

### D. Platform-common integration layer

Minecraft-specific but loader-neutral logic where possible.

Suggested package:

- `mycraft.yuyears.newitemfavorites.integration`

Responsibilities:

- convert `Slot` and `Menu` information into `LogicalSlotIndex`
- build interaction context objects from Minecraft runtime state
- centralize "what counts as a player inventory slot"

This is the layer that keeps platform code from duplicating slot resolution logic.

### E. Platform loader layer

Loader-specific registration and bootstrap only.

Existing modules fit this well:

- `fabric`
- `forge`
- `neoforge`

Responsibilities:

- lifecycle registration
- keybinding registration
- packet channel registration
- loader-specific mixin declarations
- render/event hookup

These modules should mostly wire services together and forward calls.

## Key Data Model

### Logical slot index

All gameplay logic should use a normalized player inventory index:

- `0..8`: hotbar
- `9..35`: main inventory
- `36..39`: armor
- `40`: offhand

This should be represented by a dedicated value object or at least a validated wrapper, not raw `int` everywhere.

Benefits:

- consistent behavior across inventory, chest, crafting, and creative screens
- easier sync and persistence
- easier debugging

### Favorite state

The state should be stored per player scope:

- client-only mode: local player + local server key
- server-authoritative mode: per player UUID on the server

Suggested structure:

- `FavoriteState { Set<LogicalSlotIndex> favoritedSlots, metadata... }`

Possible future metadata:

- timestamp
- source of change
- per-slot mode extensions

## Core Runtime Flows

### 1. Toggle favorite

Target flow:

1. Client detects key + hovered player inventory slot.
2. Slot mapping resolves hovered GUI slot into `LogicalSlotIndex`.
3. Client sends toggle request if server authority is available.
4. Server validates and updates state.
5. Server syncs changed slots back to client.
6. Client updates local presentation cache and refreshes UI.

If server authority is not available:

1. Client toggles local state directly.
2. Local repository is marked dirty and saved.

### 2. Interaction guard

Target flow:

1. A click/drop/swap/quick-move/use action enters through a mixin or event hook.
2. Integration layer resolves whether the target is a player inventory logical slot.
3. Application layer evaluates `InteractionGuardService`.
4. Result is one of:
   - `ALLOW`
   - `DENY`
   - `ALLOW_WITH_BYPASS`
   - `ALLOW_BUT_MUTATE_STATE`
5. Hook cancels or continues accordingly.

### 3. Item movement and slot transitions

The current config already hints at two policies:

- `FOLLOW_ITEM`
- `STAY_AT_POSITION`

This should become an explicit service contract, not ad hoc updates inside state storage.

Suggested responsibility:

- `SlotTransferService.applySwap(...)`
- `SlotTransferService.applyMove(...)`
- `SlotTransferService.handleSlotBecameEmpty(...)`

### 4. Persistence

Persistence should not be embedded in the same class that owns runtime state.

Suggested flow:

1. State service mutates state.
2. Mutation marks the appropriate repository as dirty.
3. Save scheduler or lifecycle event flushes state.
4. Repository implementation chooses the correct storage backend.

### 5. Overlay presentation

Rendering should consume presentation data, not recompute rules inline.

Suggested flow:

1. Render hook resolves logical slot.
2. `OverlayPresentationService.describe(slot, uiContext)` returns an `OverlayDescriptor`.
3. Platform renderer draws based on descriptor.

This makes rendering easier to share and easier to test.

## Proposed Package Structure

Suggested target structure inside `common`:

```text
common/src/main/java/mycraft/yuyears/newitemfavorites/
  NewItemFavoritesMod.java
  domain/
    FavoriteState.java
    LogicalSlotIndex.java
    InteractionType.java
    InteractionDecision.java
    OverlayDescriptor.java
  application/
    FavoriteStateService.java
    InteractionGuardService.java
    SlotTransferService.java
    OverlayPresentationService.java
    FavoriteSyncService.java
  integration/
    SlotMappingService.java
    InventoryContext.java
    InteractionContext.java
    MinecraftSlotClassifier.java
  infrastructure/
    config/
      ConfigManager.java
      NewItemFavoritesConfig.java
    persistence/
      FavoriteRepository.java
      DataPersistenceManager.java
    network/
      packets...
  render/
    OverlayRenderer.java
```

Notes:

- existing files can be migrated incrementally rather than moved all at once
- `ConfigManager` and `NewItemFavoritesConfig` can remain where they are short-term, then move under `infrastructure.config`
- `FavoritesManager` should eventually be replaced by a service-oriented state model

## Recommended Class Responsibilities

### `LogicalSlotIndex`

Purpose:

- validates allowed slot range
- documents slot semantics
- prevents accidental use of arbitrary menu slot ids

### `SlotMappingService`

Purpose:

- map a Minecraft `Slot` in a given `AbstractContainerMenu` to a `LogicalSlotIndex`
- detect whether a slot belongs to the player inventory
- handle special cases like creative wrappers or unusual menu layouts

This is one of the most important pieces to get right.

### `FavoriteStateService`

Purpose:

- own in-memory favorite state for the current authority scope
- expose `toggle`, `set`, `clear`, `isFavorited`
- avoid direct mutation by render and hook code

### `InteractionGuardService`

Purpose:

- central decision engine for blocked actions
- consult config + favorite state + bypass state

Sample method shape:

```java
InteractionDecision evaluate(InteractionContext context);
```

### `SlotTransferService`

Purpose:

- apply move/swap semantics
- implement `FOLLOW_ITEM` vs `STAY_AT_POSITION`
- auto-unlock empty slots when configured

### `FavoriteRepository`

Purpose:

- abstract storage backend
- allow switching between local file, world save, and server persistence without changing business logic

### `FavoriteSyncService`

Purpose:

- full sync on join/load
- incremental sync for changes
- no gameplay rules, only state transport

## Mixin Strategy

The reference implementation proves that correct interception requires multiple entry points. We should keep that insight but reduce mixin complexity.

### Keep mixins thin

Each mixin should do only:

- extract relevant arguments
- create integration/application context
- call a service
- cancel if needed

### Common interception targets

Likely targets:

- container click handling
- quick move / shift-click
- hotbar swap
- drop selected item
- offhand swap
- direct slot set / mutation hooks when needed for empty-slot unlock detection

### Avoid embedding rule logic in mixins

Bad direction:

- large switch statements inside mixin classes
- direct config/state mutation in several hooks

Preferred direction:

- one mixin delegates to one service

## Networking Strategy

The new mod should support both full and incremental sync, but the state model should stay simple.

Suggested packets:

- `ToggleFavoriteSlotC2SPacket`
- `SetFavoriteSlotC2SPacket` if needed later
- `FullFavoriteStateS2CPacket`
- `FavoriteStateDeltaS2CPacket`

Suggested rules:

- full sync on player join / world enter / server handshake
- delta sync after mutations
- client treats server as authoritative when server support is present
- client-only mode never waits on packets

## Persistence Strategy

### Client-only mode

Store per:

- player UUID
- world save name or server address key

### Integrated singleplayer

Prefer world save storage to keep state tied to saves.

### Dedicated server

Store per server player UUID in world/server data.

The repository should choose this behavior through a context object, not through scattered `if (worldDir != null)` checks across the codebase.

## Compatibility Considerations

### Creative inventory

Creative screen wrappers and special delete slots must be handled explicitly in slot mapping and slot classification.

### Non-player container slots

Rules should apply only to player inventory logical slots unless a future feature explicitly extends scope.

### Bypass key

Bypass should be represented as part of the interaction context, not read globally inside rule methods.

### Localization

Gameplay messages should use translation keys everywhere. Platform entrypoints should not hardcode English strings.

### Render assets

Overlay resource selection should be data-driven by `OverlayDescriptor` and config, not hardwired in several places.

## Migration Plan

The safest path is incremental.

### Phase 1: Architectural groundwork

- introduce `LogicalSlotIndex`
- introduce `SlotMappingService`
- introduce `InteractionType` and `InteractionDecision`
- replace direct raw slot id decisions in new code paths

### Phase 2: Replace state core

- turn `FavoritesManager` into `FavoriteStateService`
- isolate persistence behind repository interfaces
- keep current behavior but shift ownership out of global mutable singleton style

### Phase 3: Fabric-first interaction interception

- implement Fabric mixins/hooks using the new services
- support:
  - toggle favorite
  - render overlay
  - block click/drop/quick move/swap
  - auto-unlock empty slots

### Phase 4: Sync and authority

- add explicit packet layer
- support full sync and incremental sync
- unify client-only and server-authoritative behavior behind service interfaces

### Phase 5: Forge and NeoForge parity

- port the thin platform hooks
- reuse common services unchanged

## Immediate Recommendations For This Repository

These are the best next implementation steps.

1. Add a `LogicalSlotIndex` abstraction and a `SlotMappingService`.
2. Stop using raw `slot.id` as the long-term identity in rendering and interaction logic.
3. Refactor `FavoritesManager` toward `FavoriteStateService`.
4. Move decision logic for locking/blocking into `InteractionGuardService`.
5. Keep `OverlayRenderer` as a renderer only; move style selection into a presentation service.
6. Add translation-key based feedback messages before expanding UI behavior.
7. Implement Fabric first, but write common logic so Forge/NeoForge only need adapter code.

## Decisions From Reference Analysis

Taken from the reference project:

- use a unified logical slot model
- treat server sync as authoritative
- intercept multiple inventory interaction paths
- refresh UI after state sync

Intentionally changed from the reference project:

- avoid central static state as the main long-term architecture
- avoid mixing client, server, and shared decision logic in large god classes
- avoid reflection-heavy environment detection where cleaner wiring is possible
- avoid loader-specific behavior leaking into domain rules

## Definition Of Success

This architecture is successful when:

- Fabric implementation reaches feature parity without giant mixin classes
- Forge and NeoForge ports mostly reuse common services
- slot behavior is consistent across inventory GUIs
- the code clearly separates rule evaluation, storage, sync, and rendering
- new features can be added without expanding loader-specific branching everywhere

## Next Execution Step

The next practical step after this document is:

- implement `LogicalSlotIndex`
- implement `SlotMappingService`
- refactor the first Fabric path to use them

That will establish the foundation that all later locking, sync, and overlay work can build on.
