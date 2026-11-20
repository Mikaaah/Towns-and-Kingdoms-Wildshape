# Troubleshooting

Check the loader edition, preset and world type; test in a backed-up or disposable world. For river adapter issues, check the matching Streams Reflowing version and remove duplicate standalone compatibility JARs. Include seed, coordinates, preset, dependency versions and logs in reports.

| Symptom | Check |
| --- | --- |
| Wildshape world creation fails while exporting Integrated Villages avoidance tags | Install 1.0.2; it includes the 1.0.1 tagged custom-structure export fix. |
| Chunk generation reports `Comparison method violates its general contract!` | Install 1.0.2 for the concurrent cache-eviction fix from TNK3 issue #158. |
| Streams Reflowing 2.17.3 or 2.17.4 is rejected | Install 1.0.2, which includes the inspected loader ranges and version-specific bindings. |
| Existing floating clay/water remains after updating | Generation fixes affect new chunks; existing saved terrain is not rewritten. |
| Old worlds refer to `ultraterraforged` registries | A fresh world or separately verified registry migration is required. Settings import alone does not migrate saves. |

[1.0.2 verification and limits](https://github.com/Mikaaah/Towns-and-Kingdoms-Wildshape/blob/7c136fc44a215e82c56c5f7fcc0bcc2a85d578b6/docs/WILDSHAPE_WORLDGEN_FIX_2026-11-15.md)

[Documentation home](Home)
