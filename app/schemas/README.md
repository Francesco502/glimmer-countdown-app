# Room schema provenance

- `6.json` is a Room 2.8.4 export from the published 3.4 commit `f470bac`.
  The isolated export changed only `AppDatabase.exportSchema` to `true`, added
  `room.schemaLocation`, and ran `kspDirectDebugKotlin`. Its identity hash is
  therefore compiler-generated, not reconstructed by hand.
- `8.json`, `9.json`, and `10.json` were exported by the 3.5 database upgrade.
- Versions 7 and 9 were migration intermediates, not published database
  starting points. The release migration test starts at the real v6 schema and
  executes the complete `6 -> 7 -> 8 -> 9 -> 10` chain.
