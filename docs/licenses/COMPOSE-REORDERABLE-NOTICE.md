# compose-reorderable (vendored)

Glimmer vendors the Kotlin sources of **org.burnoutcrew.composereorderable:reorderable:0.9.6**
because the published artifact calls `LazyItemScope.animateItemPlacement`, which Compose 1.10 removed.

- Upstream: `org.burnoutcrew.composereorderable:reorderable:0.9.6` (source JAR)
- Author: André Claßen (Copyright 2022)
- License: Apache License, Version 2.0 — https://www.apache.org/licenses/LICENSE-2.0
- Vendored location: `app/src/main/java/org/burnoutcrew/reorderable/`
- Local patch: `Modifier.animateItemPlacement()` → `Modifier.animateItem()` (two call sites in
  `ReorderableItem.kt`). No behavioral change beyond using the replacement API.
- Each source file retains its original Apache-2.0 copyright header.

This notice is provided to satisfy the attribution requirements of the Apache License, Version 2.0.
