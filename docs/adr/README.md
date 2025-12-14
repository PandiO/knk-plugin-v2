# docs/adr/

Architecture Decision Records for v2 design and migration.

## Purpose
Document significant architectural decisions with rationale, alternatives, and consequences to prevent repeated debate and support future maintenance.

## Format
Each ADR follows MADR (Markdown Architecture Decision Records):
- **Title:** Short decision statement
- **Status:** Proposed | Accepted | Deprecated | Superseded
- **Context:** Problem/situation necessitating the decision
- **Decision:** What we chose and why
- **Consequences:** Trade-offs, benefits, risks
- **Alternatives:** What we considered and rejected
- **Related Decisions:** Links to upstream/downstream ADRs

## Naming Convention
`ADR-NN-short-title.md` (e.g., `ADR-001-async-ports-for-io.md`)

## Contents
- `ADR-001-async-ports-for-io.md` — CompletableFuture-based ports (rationale, trade-offs)
- `ADR-002-paper-adapter-thin-layer.md` — Paper as plugin entrypoint only
- `ADR-003-source-grounded-migration.md` — Why legacy inventory approach prevents hallucination
- (add more as decisions are made)

## Audience
Developers, architects, code reviewers, future maintainers.

## Reference
- MADR: https://adr.github.io/madr/
- ADR GitHub: https://adr.github.io/
