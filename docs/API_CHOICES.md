# Decision: typed DSL / Swift result builder with named presets

Accepted by Amal on 19 September 2026. Kotlin uses `trailEffect { }`; Swift uses `TrailEffect { }`. Teach one reusable named effect and one rendering call first. These are ordinary language constructs with no annotation processor, macro target, reflection, or plugin registration.

The implementation, playgrounds, and [compiled Android](examples/Android.md) / [compiled iOS](examples/iOS.md) examples now use this API. [API_GUIDE.md](API_GUIDE.md) defines its rules and ownership.

- One style and one animation per layer. Duplicate declarations fail with an explanation.
- `sequence { }` / `Sequence { }` runs finite steps in declaration order. Repeat applies to the entire sequence.
- `layer { }` / `Layer { }` draws independent layers back to front. Each layer owns its animation.
- A `TrailEffect` describes the result; each visible binding owns its playback. Map adapters own projection and attachment.
- Plugins implement the same public style and sampler contracts used by bundled effects. Named presets remain normal values.

The earlier [three-way comparison](archive/API_CHOICES.md) is retained as history. Fluent chains and generated annotations/macros are unimplemented proposals, not alternate supported APIs. Revisit generation only if real consumer needs justify its tooling cost.

There is no claim that the DSL is inherently faster, smaller, or more reliable for LLMs than a fluent API. Those claims require measurements. The design instead provides a small canonical vocabulary and examples extracted from source that the native compilers build.
