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
- `./gradlew build` and `./gradlew test` both green (8 tests total). Manually smoke-tested via `./gradlew runServer` — boots clean, `CartographyrMod`'s `ServerStartingEvent` listener confirms the wiring works at real server boot.
- See [context/classes/](context/classes/) for per-class reference.

Next: amenities, history, naming (Sections 5.5/5.6/5.3), or whichever phase the next real feature requires — no obligation to follow the phase list in strict order.
