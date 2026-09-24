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
- **Demo entry notification removed entirely, 2026-09-24** (see [context/decisions.md](context/decisions.md)) — it lived here briefly (as a chat message, then a HUD overlay) but was correctly identified as player knowledge/belief, not world truth, so it now lives in Lyfe (`.location` package there) instead. Cartographyr has **no player-facing notification logic of any kind anymore** — it's back to a pure data/query API. Gained `Cartography.discoverNaturalRegion(ServerLevel, BlockPos)` on the public facade to support Lyfe's equivalent tracker (wraps `NaturalRegionDiscovery.discover`, previously internal-only).
- **Section 5.9 (Player Knowledge) permanently removed from scope** (2026-09-24, see [context/decisions.md](context/decisions.md)) — not just deferred. That concept now belongs to the suite's second mod, **Lyfe** (github.com/CerealKlla/lyfe, private context repo lyfe-context), specifically its Historian/Cartographyr skills, which consume Cartographyr's read-only queries rather than Cartographyr owning any player-knowledge API itself.
- `./gradlew build` and `./gradlew test` both green (20 tests total, covering everything except the discovery algorithm itself). Boot-smoke-tested via `./gradlew runServer`.
- **Natural Geography is now considered stable**, confirmed across six playtest iterations while the (since-removed) demo notification exercised it — fragmentation bug found & fixed, verified via temporary diagnostic logging, biome coverage expanded from 2 to 10 families. Diagnostic logging has been stripped back down to just the meaningful state-change logs (a region created/merged) — no more per-tick noise. Full trail in [context/decisions.md](context/decisions.md).
- See [context/classes/](context/classes/) for per-class reference.

Next: **Lyfe's Cartographyr skill needs settlement-detection functionality here** (design need surfaced 2026-09-24, planning Lyfe's design doc — see lyfe-context repo, design-document.md Section 14 Phase 3.5) — **reframed 2026-09-24** (see [context/decisions.md](context/decisions.md)'s "open N-layer entity model" entry): this is no longer "Cartographyr must invent autonomous settlement-detection heuristics." Under the emerging push model, detection can live anywhere (a hook into vanilla village generation, a future town-management mod, even Lyfe itself); Cartographyr's actual job shrinks to having an open type system + write API to accept the push. Still blocks Lyfe's sign/map knowledge-sharing mechanic; still not urgent since Lyfe is doing Lumberjack/Miner first. The real prerequisite work here is making `EntityType`/`Classification` open/`Identifier`-keyed instead of closed Java enums — not yet started, see decisions.md for the full reasoning and the still-open write-permission governance question. Otherwise: revisit one of the other deferred items (spatial index persistence, an `-api` Gradle module split, more biome profiles) when something concrete needs it.

Precedent set 2026-09-24 for that Cartographyr-skill work: Lyfe now has a real `compileOnly` Gradle dependency on this repo's built jar (see lyfe-context decisions.md, same date) to call `Cartography` directly rather than through reflection — the same mechanism the Cartographyr skill/Historian will need. Whenever `Cartography`'s public surface changes, double-check Lyfe's `.location.LocationTracker` (and later the Cartographyr skill) still compile against it.
