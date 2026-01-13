# Custom Enchantments Integration – Documentation Summary

**Generated**: January 11, 2026  
**For**: Knights & Kings v2 Plugin  
**Source Analysis**: CustomEnchantments-1.0.3.jar plugin  

---

## What Was Created

Three comprehensive specification documents have been generated in `/knk-plugin-v2/spec/`:

### 1. **SPEC_CUSTOM_ENCHANTMENTS.md** (Primary Specification)
**Length**: ~600 lines  
**Purpose**: Comprehensive feature specification and requirements

**Includes**:
- Feature overview (12 enchantments with detailed specs)
- Architecture & integration points (ports, domain models, async patterns)
- Detailed enchantment specifications (triggering, probability, duration, effects)
- Storage format (lore-based with color codes)
- Permission system (customenchantments.*)
- Commands (`/ce add`, `/ce remove`, `/ce info`, `/ce cooldown clear`, `/ce reload`)
- Configuration structure (messages, settings)
- Implementation priorities (5 phases)
- Known limitations & future improvements
- Testing strategy
- Open questions & TODOs

### 2. **IMPLEMENTATION_ROADMAP_CUSTOM_ENCHANTMENTS.md** (Execution Plan)
**Length**: ~500 lines  
**Purpose**: Step-by-step implementation roadmap with effort estimates

**Includes**:
- Complete file structure for all 3 modules (knk-core, knk-api-client, knk-paper)
- 7 implementation phases with detailed tasks:
  - **Phase 1**: Core infrastructure (domain models, ports) – 13 hours
  - **Phase 2**: Passive attack enchantments (Poison, Wither, Freeze, etc.) – 14.5 hours
  - **Phase 3**: Active support enchantments (right-click abilities) – 10 hours
  - **Phase 4**: Commands & administration – 5 hours
  - **Phase 5**: Configuration & bootstrap – 2 hours
  - **Phase 6**: Testing (unit, integration, manual) – 17 hours
  - **Phase 7**: Documentation & release – 4 hours
- **Total Estimate**: 65.5 hours
- For each task: deliverables, acceptance criteria, estimated effort
- Dependency graph showing phase sequencing
- Risk assessment with mitigation strategies
- Notes for Copilot implementation

### 3. **QUICK_REFERENCE_CUSTOM_ENCHANTMENTS.md** (Quick Lookup)
**Length**: ~400 lines  
**Purpose**: Quick reference for developers during implementation

**Includes**:
- Enchantment stats table (all 12 with levels, cooldowns, triggers)
- Core ports (method signatures)
- Domain models (record definitions)
- Key files to create (complete list)
- Threading rules (async patterns, Bukkit scheduler)
- Lore format with parsing helpers
- Config structure template
- Special case handling (Freeze, Chaos, Strength, Health Boost)
- Commands & permissions list
- Test checklist
- Implementation tips
- Common pitfalls to avoid

---

## Key Findings from Analysis

### Enchantments Identified

**Attack Enchantments** (trigger on melee hit):
1. Poison (3 levels) – Poison II potion effect
2. Wither (3 levels) – Poison II + witch particles
3. Freeze (3 levels) – Block movement + ice particles
4. Blindness (3 levels) – Blindness potion + sound
5. Confusion (3 levels) – Nausea potion effect
6. Strength (2 levels) – Grants attacker strength boost (120s cooldown)

**Support Enchantments** (triggered by right-click):
7. Chaos (1 level) – AoE damage + knockback (90s cooldown)
8. Flash Chaos (1 level) – Enhanced Chaos with debuffs (90s cooldown)
9. Health Boost (1 level) – Restore health over time (120s cooldown)
10. Armor Repair (1 level) – Fully repair all armor (120s cooldown)
11. Resistance (2 levels) – Damage resistance buff (120s cooldown)
12. Invisibility (1 level) – Invisibility potion (90s cooldown)

### Architecture Patterns

**Storage**: Item lore with color codes (§7Poison I, §7Chaos I, etc.)
**Threading**: Events → async parsing → main thread for effects
**Permissions**: Per-enchantment (customenchantments.*)
**Cooldowns**: Per-player per-enchantment tracking
**Effects**: Potion effects, particles, sounds, velocity changes

### Design Decisions Made

1. **Local Implementation First**: No API integration initially (stored only in item lore)
2. **Async-Friendly**: All services return CompletableFuture for future API integration
3. **Event-Driven**: Listeners trigger effects on damage/interact events
4. **Configurable**: All messages, settings in config.yml
5. **Permission-Based**: Admin controls via customenchantments.* permissions
6. **Cooldown System**: In-memory tracking prevents spam
7. **Thread-Safe**: Main thread scheduler for all Bukkit operations

---

## Integration with knk-plugin-v2 Architecture

These specifications follow the established patterns from knk-plugin-v2:

✅ **Layered Architecture**:
- knk-core: Domain models + ports (no Bukkit)
- knk-api-client: Port implementations (local for now, API-ready)
- knk-paper: Event listeners, commands, UI

✅ **Async Pattern**:
- All services return CompletableFuture
- Bukkit effects scheduled on main thread
- Ready for future API integration

✅ **Configuration**:
- config.yml for messages and settings
- ConfigManager for loading and placeholder replacement

✅ **Testing**:
- Unit tests in knk-core
- Integration tests in knk-paper
- Manual testing checklist included

---

## How to Use These Documents

### For Copilot Implementation

1. **Start Here**: Read QUICK_REFERENCE_CUSTOM_ENCHANTMENTS.md for overview
2. **Deep Dive**: Reference SPEC_CUSTOM_ENCHANTMENTS.md for details
3. **Follow Roadmap**: Use IMPLEMENTATION_ROADMAP_CUSTOM_ENCHANTMENTS.md for execution
4. **Stay Compliant**: Check Architecture & Threading sections before coding

### For Stakeholders/PMs

1. **Project Scope**: SPEC_CUSTOM_ENCHANTMENTS.md sections 1-2
2. **Timeline**: IMPLEMENTATION_ROADMAP_CUSTOM_ENCHANTMENTS.md summary table (65.5 hours)
3. **Risk**: IMPLEMENTATION_ROADMAP_CUSTOM_ENCHANTMENTS.md risk assessment
4. **Questions**: SPEC_CUSTOM_ENCHANTMENTS.md section 12 (open questions)

### For Testers

1. **Feature List**: QUICK_REFERENCE_CUSTOM_ENCHANTMENTS.md enchantment table
2. **Test Cases**: IMPLEMENTATION_ROADMAP_CUSTOM_ENCHANTMENTS.md phase 6 (testing)
3. **Manual Checklist**: QUICK_REFERENCE_CUSTOM_ENCHANTMENTS.md test checklist
4. **Permissions**: QUICK_REFERENCE_CUSTOM_ENCHANTMENTS.md permissions list

---

## File Locations

```
/Users/pandi/Documents/Werk/knk-workspace/Repository/knk-plugin-v2/spec/
├── SPEC_CUSTOM_ENCHANTMENTS.md                    (Primary specification)
├── IMPLEMENTATION_ROADMAP_CUSTOM_ENCHANTMENTS.md  (Execution plan)
└── QUICK_REFERENCE_CUSTOM_ENCHANTMENTS.md         (Developer reference)
```

All documents follow markdown format and are integrated into the knk-plugin-v2 spec directory alongside existing documentation (SPEC_TOWNS.md, SPEC_USER.md, etc.).

---

## Next Steps

1. **Review**: Stakeholders review and approve SPEC_CUSTOM_ENCHANTMENTS.md
2. **Answer Questions**: Address open questions in section 12 of SPEC
3. **Assign Tasks**: Use IMPLEMENTATION_ROADMAP_CUSTOM_ENCHANTMENTS.md to create sprints
4. **Implement**: Follow phases 1-7 in sequence
5. **Test**: Use test checklists during development
6. **Release**: Create wiki/help docs after completion

---

## Questions or Refinements?

The specification documents include extensive TODO sections and open questions. Before implementation begins:

- [ ] Confirm API persistence strategy (should enchantments sync to Web API?)
- [ ] Approve enchantment balance (damage values, cooldowns, levels)
- [ ] Decide on UI/inventory menu needs
- [ ] Confirm permission hierarchy (admin perms for /ce commands)
- [ ] Plan migration from old CustomEnchantments plugin data (if any)

---

## Summary

✅ **Complete analysis** of CustomEnchantments-1.0.3.jar plugin  
✅ **Comprehensive specification** with all 12 enchantments detailed  
✅ **Architecture-compliant** design following knk-plugin-v2 patterns  
✅ **Detailed roadmap** with 65.5-hour effort estimate  
✅ **Quick reference** guide for developers  
✅ **Ready for implementation** with clear phases and deliverables  

The documentation is production-ready and can be handed to Copilot for immediate implementation.

