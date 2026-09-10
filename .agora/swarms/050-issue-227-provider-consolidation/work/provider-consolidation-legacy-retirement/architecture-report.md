# Architecture report

The shared emission package now owns deterministic emitter selection. Registered emitters take precedence; Java's existing renderer is exposed as the registry-managed fallback, while unsupported targets fail closed with available-target diagnostics. COBOL no longer declares provider-java or Core dependencies.

Java-specific artifact layout is target architecture policy and now resides in `renovatio-architecture`. COBOL semantic transpilation depends on a local recipe execution port and OpenRewrite APIs rather than the Java provider implementation. Active ArchUnit rules prohibit COBOL dependencies on provider-java, Core, and web packages.

Generation remains decomposed into parsing, semantic projection, architecture planning, target emission, orchestration, and artifact writing collaborators. `JavaGenerationService` is the Java rendering compatibility facade; target selection and layout authority are outside it.

The deprecated Core registry adapter is the sole intentional compatibility exception and is not referenced by providers.
