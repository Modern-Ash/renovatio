# Prompt Catalog and Provider Policy Correction

Date: 2026-08-30

The strict prompt loader now requires versioned `vN.schema.json` resources and fails closed for
duplicate prompt IDs, blank system text, null few-shot input/output, empty or unknown validators,
missing resources, and invalid fallback diagnostic codes. Tests cover each rejection class.

The Anthropic transport exposes its serialized body to package-level verification. The provider
policy test parses that exact body and asserts the configured model, 4096-token limit, system
message, message sequence, and numeric `temperature: 0`.

The governed promotion report now records the actual manifest-only Commit D:
`d547e8face947e6b5dfa5d6409366c3f54ca5f74`.

Verification command:
`env JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:/usr/local/bin:/usr/bin:/bin mvn -pl renovatio-llm -am test -q`

Result: 137 tests passed, zero failures and zero errors.
