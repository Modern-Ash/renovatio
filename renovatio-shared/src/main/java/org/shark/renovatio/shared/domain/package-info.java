/**
 * Domain-centric data structures and value objects shared across providers and services.
 * <p>
 * These DTOs define the canonical, MCP-compliant models used by Renovatio to exchange
 * analysis results, refactoring plans, diffs, metrics, and stubs between the core engine,
 * language providers, and the MCP server.
 * <p>
 * Implementation notes:
 * - DTOs favor clarity and stability. Field names and shapes are treated as API and should
 *   remain backward compatible.
 * - Boilerplate (getters, setters, equals/hashCode, toString) is generated via Lombok to
 *   keep the code concise. Ensure annotation processing is enabled in your IDE/build.
 * - All comments, documentation, and identifiers are written in English.
 */
package org.shark.renovatio.shared.domain;
