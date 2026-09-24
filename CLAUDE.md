# Cartographyr

Persistent geographic foundation mod for a modular Minecraft project (Minecraft Java Edition). Part of a suite of intercommunicating mods that expose public APIs so other mods — the user's own and third parties' — can integrate.

## Context directory — read this first

`context/` is a **separate private repo** (https://github.com/CerealKlla/cartographyr-context), not part of this one — it's listed in `.gitignore` here and must never be committed to this repo. It's cloned as a subdirectory at `context/` for local convenience. If this directory is missing (e.g. a fresh clone of just this repo), restore it with:

```
git clone https://github.com/CerealKlla/cartographyr-context.git context
```

Before searching source for architecture, ownership boundaries, API shape, or "why does this work this way," check `context/` first. It's maintained specifically to answer those questions cheaply:

- `context/design-document.md` — the authoritative design spec: architecture, entity model, full public API surface, storage design, implementation phases, testing strategy. Start here for anything about intended shape or scope.
- `context/decisions.md` — dated log of decisions made during implementation that extend or override the design document, with rationale. Check this for anything that looks like it contradicts design-document.md — the doc should already reflect the current decision, but this explains why.
- `context/classes/` — one short markdown file per implemented class: public surface, key state, collaborators. Read the relevant file here before opening the actual source, and before editing a class update its file to match.

**Keep this system current as you work:**
- When a design decision is made that conflicts with or is absent from design-document.md, update design-document.md directly and add a dated entry to decisions.md explaining the change.
- When a class is added or its public surface changes, add or update its file in `context/classes/`.
- Don't let source and these docs drift — treat updating them as part of finishing the change, not optional cleanup.
- `context/` has its own git history, independent of this repo's commits. Commit and push changes there separately (`git -C context add . && git -C context commit -m "..." && git -C context push`) — editing the files alone doesn't back them up.

## Status

Phase 0 (Version/Loader Lock) — decided 2026-09-23, see [context/decisions.md](context/decisions.md):
- Loader: **NeoForge**
- Minecraft version: **26.1.2**
- Java: **JDK 25** (standalone Eclipse Temurin, JAVA_HOME set)
- Group ID: `com.github.cerealklla.cartographyr` / Mod ID: `cartographyr`

Phase 1 minimal milestone (design-document.md Appendix B) — implemented 2026-09-23, see [context/decisions.md](context/decisions.md):
- `.geo`/`.storage`/`.api` package split; `EntityId`, `GeographicEntity`, point/bounds `Geometry`, world-level `CartographySavedData`, chunk-grid `SpatialIndex`, and the `Cartography` public facade (create/get/update/retire/getEntitiesAt).
- `EntityType` simplified since: dropped VILLAGE/TOWN/CITY tier and RUIN, collapsed to `REGION, MOUNTAIN, RIVER, SETTLEMENT, MINE, ROAD` — settlement tier isn't Cartography's to classify, and active-vs-abandoned is `LifecycleState`'s job, not a type.
- Structure Association (Section 5.7) — `associateStructure`/`getAssociatedStructures`/`getEntityForStructure` on `Cartography`, backed by vanilla `GlobalPos` and a `structureIndex` reverse lookup on `CartographySavedData`. Completes the PLANNED→REALIZED flow from Section 4.
- Characteristics (Section 5.4) — `addCharacteristic`/`removeCharacteristic`/`getCharacteristics` on `Cartography`, backed by a `Set<Characteristic>` field directly on `GeographicEntity` (no index needed). `Characteristic` is `LUMBER, MINING, FARMING` for now.
- Amenities (Section 5.5) — `addAmenity`/`removeAmenity`/`getAmenities` on `Cartography`, same shape as Characteristics. `Amenity` is `MARKET, MINE, INN, HARBOR` for now.
- Naming (Section 5.3) — `setName`/`addAlternateName`/`getNames` on `Cartography`. New `AlternateName` (name + free-text metadata) and composite `EntityNames` (current + alternate names) types in `.geo`.
- History (Section 5.6) — `addHistoricalFact`/`getHistoricalFacts` on `Cartography`. New `HistoricalFact` (description + `gameTime` + free-text metadata) type; the entity's fact list stays sorted by `gameTime` regardless of insertion order.
- **Section 5's core entity-data endpoints (5.1–5.7) are now complete.**
- Natural Geography (Section 5.8) — new `.natural` package (`NaturalRegionProfile`, `NaturalRegionDiscovery`) flood-fills outward from a point sampling live biome data, discovers/names regions, persists them as a new `Geometry.Region` (irregular chunk-cell shape). `getNaturalRegionAt`/`findNaturalRegions`/`getRegionBounds` added to `Cartography`. **This is the one part of the codebase that can't be unit tested** — needs a live `ServerLevel` to sample biomes; verify manually via `./gradlew runClient`.
- Biome coverage now spans 10 families: `DESERT, FOREST, PLAINS, SWAMP, TAIGA, JUNGLE, SAVANNA, BADLANDS, MOUNTAIN, RIVER`. Still not exhaustive — oceans, beaches, mushroom fields, cherry groves, ice spikes, cave/Nether/End biomes have no profile.
- Demo entry notification — `CartographyrMod`'s `PlayerTickEvent.Post` listener checks the player's position once/second, triggers natural-region discovery if nothing's known there, and sends a chat message ("You have entered <name>") on change. Session-only (in-memory, not persisted) — explicitly a demo feature, not the real Player Knowledge system (Section 5.9, still out of scope).
- Remaining design doc scope: 5.9 Player Knowledge, still out of scope per Appendix B until there's a real reason to need it.
- `./gradlew build` and `./gradlew test` both green (20 tests total, covering everything except the discovery algorithm itself). Boot-smoke-tested via `./gradlew runServer`.
- **Playtest history**: first playtest found a fragmentation bug (one forest splitting into multiple differently-named entities) — fixed by merging new patches into adjacent same-type entities instead of always creating new ones. A "still broken" second-playtest report turned out to be stale data from the *first* (pre-fix) session's save file, not a real regression — confirmed via temporary diagnostic logging (`CartographyrMod`/`NaturalRegionDiscovery` currently log position checks, merges, and suppressed/sent notifications at `INFO` on the server console — not player-visible, but verbose; worth stripping down once the system feels stable). A clean fresh-world session then showed exactly 1 real notification sent and 236 correctly-suppressed repeats over ~7 minutes. See [context/decisions.md](context/decisions.md) for the full trail.
- See [context/classes/](context/classes/) for per-class reference.

Next: manually verify the expanded biome coverage in-game (walk between a couple of the new biome families, confirm distinct names + no repeats), consider trimming the diagnostic logging down once confirmed stable, then likely either start on a second mod in the suite that actually consumes this API, or revisit one of the deferred items (EntityType extensibility, spatial index persistence, an `-api` Gradle module split) when something concrete needs it.
