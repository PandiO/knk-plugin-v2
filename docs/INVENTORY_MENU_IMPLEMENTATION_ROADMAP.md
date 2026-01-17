# Inventory Menu System - Implementation Roadmap (v2)

**Version:** 2.0  
**Date:** January 17, 2026  
**Target:** knk-plugin-v2 (knk-core + knk-api-client + knk-paper)  
**Status:** Planning

---

## Executive Summary

This roadmap outlines the implementation of the Inventory Menu System for knk-plugin-v2, following the new architecture with separation of concerns across:
- **knk-core**: Domain models, ports/interfaces, menu structure definitions
- **knk-api-client**: API communication for template persistence
- **knk-paper**: Paper-specific rendering, interaction handling, Bukkit inventory management

**Key Enhancements:**
- Web API-backed menu template persistence (CRUD via Web App)
- Conditional rendering based on player attributes (level, rank, permissions)
- Async rendering pipeline for performance
- Comprehensive validation framework
- Observer pattern for event broadcasting
- Separated rendering engine for maintainability

---

## Architecture Alignment

### Module Responsibilities

```
┌──────────────────────────────────────────────────┐
│              knk-web-api-v2                      │
│  - MenuTemplate CRUD endpoints                   │
│  - MenuTemplateDto (JSON serialization)          │
│  - Validation rules                              │
│  - Version control for templates                 │
└─────────────────┬────────────────────────────────┘
                  │ HTTP/JSON
┌─────────────────────────────────────────────────┐
│           knk-api-client                        │
│  - MenuTemplateQueryApi                         │
│  - MenuTemplateCommandApi                       │
│  - DTO → Domain mapping                         │
└─────────────────┬────────────────────────────────┘
                  │
┌─────────────────────────────────────────────────┐
│              knk-core                            │
│  Domain Layer:                                   │
│  - MenuTemplate (domain model)                   │
│  - MenuSectionTemplate                           │
│  - MenuItemTemplate                              │
│  - RenderCondition (conditional display)         │
│  - VariableDefinition                            │
│                                                  │
│  Ports (Interfaces):                             │
│  - MenuTemplateQueryApi                          │
│  - MenuTemplateCommandApi                        │
│  - MenuRenderer (abstraction)                    │
│  - ConditionEvaluator                            │
│  - VariableResolver                              │
└─────────────────┬────────────────────────────────┘
                  │
┌─────────────────────────────────────────────────┐
│              knk-paper                           │
│  Implementation Layer:                           │
│  - BukkitMenuRenderer (MenuRenderer impl)        │
│  - PaperConditionEvaluator                       │
│  - PaperVariableResolver                         │
│  - MenuSession (runtime state)                   │
│  - InventoryClickListener                        │
│  - MenuCache (in-memory template cache)          │
│  - MenuUtil (slot calculations)                  │
└──────────────────────────────────────────────────┘
```

### Data Flow

```
[Web App] 
    ↓ (Create/Edit Menu Template)
[Web API] (POST /api/menus/templates)
    ↓ (Persist to DB)
[Database] (MenuTemplates table)
    ↓ (Cache refresh trigger)
[knk-api-client] (GET /api/menus/templates/{id})
    ↓ (Map to domain)
[knk-core] (MenuTemplate domain model)
    ↓ (Render for player)
[knk-paper] (BukkitMenuRenderer + conditions)
    ↓ (Display to player)
[Player Inventory]
```

---

## New Requirements

### REQ-1: Menu Template Persistence (Web API Integration)

**Requirement**: Menu templates must be stored in database and accessible via Web API for editing in Web App.

**Components:**

1. **Database Schema** (Web API)
   ```sql
   CREATE TABLE MenuTemplates (
     Id INT PRIMARY KEY IDENTITY,
     Name NVARCHAR(100) NOT NULL,
     DisplayName NVARCHAR(200),
     Description NVARCHAR(500),
     Width INT NOT NULL DEFAULT 9,
     Height INT NOT NULL DEFAULT 3,
     BackgroundMaterial NVARCHAR(50),
     Version INT NOT NULL DEFAULT 1,
     CreatedBy INT,
     CreatedDate DATETIME2 NOT NULL,
     UpdatedBy INT,
     UpdatedDate DATETIME2,
     IsActive BIT NOT NULL DEFAULT 1
   );

   CREATE TABLE MenuSectionTemplates (
     Id INT PRIMARY KEY IDENTITY,
     MenuTemplateId INT NOT NULL REFERENCES MenuTemplates(Id),
     Name NVARCHAR(100) NOT NULL,
     StartX INT NOT NULL,
     StartY INT NOT NULL,
     Width INT NOT NULL,
     Height INT NOT NULL,
     AlignVertical NVARCHAR(20),
     AlignHorizontal NVARCHAR(20),
     Position NVARCHAR(20),
     Priority NVARCHAR(20),
     ListMode NVARCHAR(20),
     Overflow NVARCHAR(20),
     DisplayOrder INT NOT NULL
   );

   CREATE TABLE MenuItemTemplates (
     Id INT PRIMARY KEY IDENTITY,
     MenuSectionTemplateId INT NOT NULL REFERENCES MenuSectionTemplates(Id),
     Name NVARCHAR(100) NOT NULL,
     DisplayNameTemplate NVARCHAR(500), -- VariableString pattern
     Material NVARCHAR(50) NOT NULL,
     Amount INT NOT NULL DEFAULT 1,
     Slot INT,
     DisplayOrder INT NOT NULL,
     ClickActionType NVARCHAR(50),
     ClickActionData NVARCHAR(MAX) -- JSON
   );

   CREATE TABLE MenuItemLoreTemplates (
     Id INT PRIMARY KEY IDENTITY,
     MenuItemTemplateId INT NOT NULL REFERENCES MenuItemTemplates(Id),
     LoreLineTemplate NVARCHAR(500), -- VariableString pattern
     DisplayOrder INT NOT NULL,
     RenderCondition NVARCHAR(MAX) -- JSON condition expression
   );

   CREATE TABLE MenuRenderConditions (
     Id INT PRIMARY KEY IDENTITY,
     MenuItemTemplateId INT REFERENCES MenuItemTemplates(Id),
     MenuSectionTemplateId INT REFERENCES MenuSectionTemplates(Id),
     ConditionType NVARCHAR(50) NOT NULL, -- 'PlayerLevel', 'PlayerRank', 'Permission', 'Expression'
     ConditionData NVARCHAR(MAX) NOT NULL, -- JSON
     IsRequired BIT NOT NULL DEFAULT 1
   );
   ```

2. **Web API Endpoints**
   ```csharp
   // Controllers/MenuTemplateController.cs
   [ApiController]
   [Route("api/menus/templates")]
   public class MenuTemplateController : ControllerBase
   {
     [HttpGet]
     public async Task<ActionResult<List<MenuTemplateDto>>> GetAll();
     
     [HttpGet("{id}")]
     public async Task<ActionResult<MenuTemplateDetailDto>> GetById(int id);
     
     [HttpPost]
     public async Task<ActionResult<MenuTemplateDetailDto>> Create(CreateMenuTemplateDto dto);
     
     [HttpPut("{id}")]
     public async Task<ActionResult<MenuTemplateDetailDto>> Update(int id, UpdateMenuTemplateDto dto);
     
     [HttpDelete("{id}")]
     public async Task<ActionResult> Delete(int id);
     
     [HttpPost("{id}/clone")]
     public async Task<ActionResult<MenuTemplateDetailDto>> Clone(int id, string newName);
     
     [HttpGet("{id}/preview")]
     public async Task<ActionResult<MenuPreviewDto>> GetPreview(int id, int? playerId);
   }
   ```

3. **Domain Model** (knk-core)
   ```java
   // MenuTemplate.java
   public record MenuTemplate(
     Integer id,
     String name,
     String displayName,
     Integer width,
     Integer height,
     String backgroundMaterial,
     List<MenuSectionTemplate> sections,
     Integer version,
     boolean isActive
   ) {}

   public record MenuSectionTemplate(
     Integer id,
     String name,
     Integer startX,
     Integer startY,
     Integer width,
     Integer height,
     AlignVertical alignVertical,
     AlignHorizontal alignHorizontal,
     Position position,
     Priority priority,
     ListMode listMode,
     Overflow overflow,
     List<MenuItemTemplate> items,
     List<RenderCondition> conditions
   ) {}

   public record MenuItemTemplate(
     Integer id,
     String name,
     String displayNameTemplate, // VariableString pattern
     String material,
     Integer amount,
     Integer slot,
     List<MenuItemLoreTemplate> lore,
     List<RenderCondition> conditions,
     ClickAction clickAction
   ) {}

   public record MenuItemLoreTemplate(
     Integer id,
     String loreLineTemplate, // VariableString pattern
     Integer displayOrder,
     RenderCondition condition // Optional: only show this line if condition met
   ) {}
   ```

4. **API Client** (knk-api-client)
   ```java
   // MenuTemplateQueryApi.java
   public interface MenuTemplateQueryApi {
     CompletableFuture<List<MenuTemplateSummary>> getAll();
     CompletableFuture<MenuTemplate> getById(Integer id);
     CompletableFuture<MenuTemplate> getByName(String name);
   }

   // MenuTemplateCommandApi.java
   public interface MenuTemplateCommandApi {
     CompletableFuture<MenuTemplate> create(MenuTemplate template);
     CompletableFuture<MenuTemplate> update(MenuTemplate template);
     CompletableFuture<Void> delete(Integer id);
   }
   ```

5. **Caching Strategy** (knk-paper)
   ```java
   // MenuTemplateCache.java
   public class MenuTemplateCache {
     private final Map<Integer, MenuTemplate> byId = new ConcurrentHashMap<>();
     private final Map<String, MenuTemplate> byName = new ConcurrentHashMap<>();
     private final MenuTemplateQueryApi queryApi;
     
     public Optional<MenuTemplate> getById(Integer id) {
       return Optional.ofNullable(byId.computeIfAbsent(id, 
         k -> queryApi.getById(id).join()));
     }
     
     public Optional<MenuTemplate> getByName(String name) {
       return Optional.ofNullable(byName.computeIfAbsent(name,
         k -> queryApi.getByName(name).join()));
     }
     
     public void invalidate(Integer id) {
       MenuTemplate template = byId.remove(id);
       if (template != null) {
         byName.remove(template.name());
       }
     }
     
     public void refresh() {
       byId.clear();
       byName.clear();
     }
   }
   ```

**Acceptance Criteria:**
- [ ] Web API exposes full CRUD endpoints for menu templates
- [ ] Web App can create/edit/delete menu templates via UI
- [ ] Plugin loads templates from API on startup
- [ ] Plugin caches templates in memory with refresh capability
- [ ] Template version control prevents concurrent edit conflicts
- [ ] Changes to templates via Web App reflect in-game within 60 seconds (cache refresh)

---

### REQ-2: Conditional Rendering (WITH PERSISTENCE)

**Requirement**: Menu items and lore lines can have conditions that determine visibility/positioning based on player attributes. **All conditions and expressions are stored in the database and editable via Web App.**

**Scope**: This is part of the INITIAL implementation (Phase 4), not a future requirement. Conditions are first-class citizens in the menu system.

**Condition Types:**

1. **PlayerLevel** - Player must be >= certain level
2. **PlayerRank** - Player must have specific rank/role
3. **Permission** - Player must have permission node
4. **Expression** - Complex boolean expression (e.g., "level >= 10 AND coins >= 1000")
5. **DateTime** - Time-based conditions (e.g., only show during event)
6. **Custom** - Plugin-defined condition via extension point

**Domain Model:**

```java
// RenderCondition.java (knk-core)
public record RenderCondition(
  Integer id,
  ConditionType type,
  String conditionData, // JSON or expression string
  boolean isRequired    // true = must pass; false = optional
) {}

public enum ConditionType {
  PLAYER_LEVEL,
  PLAYER_RANK,
  PERMISSION,
  EXPRESSION,
  DATE_TIME,
  CUSTOM
}

// Condition data examples (stored as JSON):
// PlayerLevel: {"minLevel": 10}
// PlayerRank: {"ranks": ["OWNER", "ADMIN"]}
// Permission: {"node": "k&k.menu.admin"}
// Expression: {"expression": "player.level >= 10 AND player.coins >= 1000"}
// DateTime: {"start": "2026-01-01T00:00:00Z", "end": "2026-12-31T23:59:59Z"}
```

**Implementation:**

```java
// ConditionEvaluator.java (knk-core - port/interface)
public interface ConditionEvaluator {
  boolean evaluate(RenderCondition condition, MenuSession session);
  boolean evaluateAll(List<RenderCondition> conditions, MenuSession session);
}

// PaperConditionEvaluator.java (knk-paper - implementation)
public class PaperConditionEvaluator implements ConditionEvaluator {
  private final UsersQueryApi usersApi;
  
  @Override
  public boolean evaluate(RenderCondition condition, MenuSession session) {
    Player player = session.getPlayer();
    UserSummary user = session.getUser();
    
    return switch (condition.type()) {
      case PLAYER_LEVEL -> {
        JsonNode data = parseJson(condition.conditionData());
        int minLevel = data.get("minLevel").asInt();
        yield user.level() >= minLevel;
      }
      case PLAYER_RANK -> {
        JsonNode data = parseJson(condition.conditionData());
        List<String> allowedRanks = parseStringArray(data.get("ranks"));
        yield allowedRanks.contains(user.rank());
      }
      case PERMISSION -> {
        JsonNode data = parseJson(condition.conditionData());
        String permission = data.get("node").asText();
        yield player.hasPermission(permission);
      }
      case EXPRESSION -> {
        JsonNode data = parseJson(condition.conditionData());
        String expression = data.get("expression").asText();
        yield evaluateExpression(expression, session);
      }
      case DATE_TIME -> {
        JsonNode data = parseJson(condition.conditionData());
        Instant start = Instant.parse(data.get("start").asText());
        Instant end = Instant.parse(data.get("end").asText());
        Instant now = Instant.now();
        yield now.isAfter(start) && now.isBefore(end);
      }
      case CUSTOM -> {
        // Extension point for custom conditions
        yield evaluateCustomCondition(condition, session);
      }
    };
  }
  
  @Override
  public boolean evaluateAll(List<RenderCondition> conditions, MenuSession session) {
    if (conditions == null || conditions.isEmpty()) {
      return true; // No conditions = always render
    }
    
    return conditions.stream()
      .filter(RenderCondition::isRequired)
      .allMatch(c -> evaluate(c, session));
  }
}
```

**Expression Language:**

Use a simple expression evaluator for complex conditions:

```java
// ExpressionEvaluator.java
public class ExpressionEvaluator {
  public boolean evaluate(String expression, Map<String, Object> context) {
    // Parse expression: "player.level >= 10 AND player.coins >= 1000"
    // Support operators: AND, OR, NOT, ==, !=, <, >, <=, >=
    // Support variables: player.level, player.coins, player.rank, etc.
    
    // Example: Use JEXL or implement custom parser
    JexlEngine jexl = new JexlBuilder().create();
    JexlExpression expr = jexl.createExpression(expression);
    JexlContext ctx = new MapContext(context);
    Object result = expr.evaluate(ctx);
    return Boolean.TRUE.equals(result);
  }
}
```

**Rendering Pipeline with Conditions:**

```java
// BukkitMenuRenderer.java
public class BukkitMenuRenderer implements MenuRenderer {
  private final ConditionEvaluator conditionEvaluator;
  
  @Override
  public Inventory render(MenuTemplate template, MenuSession session) {
    Inventory inv = createInventory(template);
    
    for (MenuSectionTemplate section : template.sections()) {
      // Check section-level conditions
      if (!conditionEvaluator.evaluateAll(section.conditions(), session)) {
        continue; // Skip entire section
      }
      
      for (MenuItemTemplate item : section.items()) {
        // Check item-level conditions
        if (!conditionEvaluator.evaluateAll(item.conditions(), session)) {
          continue; // Skip item
        }
        
        ItemStack itemStack = buildItemStack(item, session);
        int slot = calculateSlot(item, section);
        inv.setItem(slot, itemStack);
      }
    }
    
    return inv;
  }
  
  private ItemStack buildItemStack(MenuItemTemplate item, MenuSession session) {
    // ... build ItemStack
    
    // Build lore with conditional lines
    List<String> lore = new ArrayList<>();
    for (MenuItemLoreTemplate loreLine : item.lore()) {
      // Check lore line condition
      if (loreLine.condition() != null 
          && !conditionEvaluator.evaluate(loreLine.condition(), session)) {
        continue; // Skip this lore line
      }
      
      String resolved = variableResolver.resolve(loreLine.loreLineTemplate(), session);
      lore.add(resolved);
    }
    
    meta.setLore(lore);
    // ...
  }
}
```

**Web App Integration:**

```tsx
// MenuItemEditor.tsx (React component)
const MenuItemEditor = ({ item }: { item: MenuItemTemplate }) => {
  return (
    <div>
      <TextField label="Item Name" value={item.name} />
      <MaterialPicker value={item.material} />
      
      {/* Condition Builder */}
      <ConditionBuilder 
        conditions={item.conditions}
        onChange={setConditions}
      />
      
      {/* Lore Editor with per-line conditions */}
      <LoreEditor lines={item.lore}>
        {(line) => (
          <LoreLine 
            template={line.loreLineTemplate}
            condition={line.condition}
            onConditionChange={(cond) => updateLineCondition(line.id, cond)}
          />
        )}
      </LoreEditor>
    </div>
  );
};

// ConditionBuilder.tsx - Visual condition builder
const ConditionBuilder = ({ conditions, onChange }) => {
  const [mode, setMode] = useState<'simple' | 'advanced'>('simple');
  
  // Simple mode: Visual builder
  const addCondition = (type: ConditionType) => {
    const newCondition = {
      type,
      conditionData: JSON.stringify(getDefaultDataForType(type)),
      isRequired: true
    };
    onChange([...conditions, newCondition]);
  };
  
  return (
    <div className="condition-builder">
      <div className="mode-toggle">
        <Button onClick={() => setMode('simple')} active={mode === 'simple'}>
          Visual Builder
        </Button>
        <Button onClick={() => setMode('advanced')} active={mode === 'advanced'}>
          Expression Editor
        </Button>
      </div>
      
      {mode === 'simple' ? (
        // Visual builder for individual conditions
        <div>
          <h3>Render Conditions</h3>
          {conditions.map(cond => (
            <ConditionEditor 
              key={cond.id}
              condition={cond} 
              onUpdate={updateCondition}
              onDelete={deleteCondition}
            />
          ))}
          <ButtonGroup>
            <Button onClick={() => addCondition(ConditionType.PLAYER_LEVEL)}>
              + Level Condition
            </Button>
            <Button onClick={() => addCondition(ConditionType.PLAYER_RANK)}>
              + Rank Condition
            </Button>
            <Button onClick={() => addCondition(ConditionType.PERMISSION)}>
              + Permission
            </Button>
            <Button onClick={() => addCondition(ConditionType.EXPRESSION)}>
              + Custom Expression
            </Button>
          </ButtonGroup>
        </div>
      ) : (
        // Advanced mode: Expression editor
        <ExpressionEditor 
          conditions={conditions}
          onChange={onChange}
        />
      )}
    </div>
  );
};

// ConditionEditor.tsx - Edit individual condition
const ConditionEditor = ({ condition, onUpdate, onDelete }) => {
  const data = JSON.parse(condition.conditionData);
  
  return (
    <Card className="condition-card">
      <div className="condition-header">
        <Select 
          value={condition.type}
          onChange={(type) => onUpdate({...condition, type})}
        >
          <option value="PLAYER_LEVEL">Player Level</option>
          <option value="PLAYER_RANK">Player Rank</option>
          <option value="PERMISSION">Permission</option>
          <option value="EXPRESSION">Custom Expression</option>
          <option value="DATE_TIME">Date/Time</option>
        </Select>
        <IconButton onClick={onDelete}>🗑️</IconButton>
      </div>
      
      <div className="condition-body">
        {condition.type === 'PLAYER_LEVEL' && (
          <TextField
            label="Minimum Level"
            type="number"
            value={data.minLevel || 1}
            onChange={(val) => onUpdate({
              ...condition,
              conditionData: JSON.stringify({minLevel: val})
            })}
          />
        )}
        
        {condition.type === 'PLAYER_RANK' && (
          <MultiSelect
            label="Allowed Ranks"
            options={['OWNER', 'ADMIN', 'MODERATOR', 'PLAYER']}
            value={data.ranks || []}
            onChange={(ranks) => onUpdate({
              ...condition,
              conditionData: JSON.stringify({ranks})
            })}
          />
        )}
        
        {condition.type === 'PERMISSION' && (
          <TextField
            label="Permission Node"
            placeholder="k&k.menu.admin"
            value={data.node || ''}
            onChange={(node) => onUpdate({
              ...condition,
              conditionData: JSON.stringify({node})
            })}
          />
        )}
        
        {condition.type === 'EXPRESSION' && (
          <div>
            <CodeEditor
              label="Expression"
              language="expression"
              value={data.expression || ''}
              onChange={(expr) => onUpdate({
                ...condition,
                conditionData: JSON.stringify({expression: expr})
              })}
              placeholder="player.level >= 10 AND player.coins >= 1000"
            />
            <HelpText>
              Available variables: player.level, player.coins, player.rank, player.town
              <br/>
              Operators: AND, OR, NOT, ==, !=, &lt;, &gt;, &lt;=, &gt;=
            </HelpText>
          </div>
        )}
        
        {condition.type === 'DATE_TIME' && (
          <div>
            <DateTimePicker
              label="Start Date"
              value={data.start}
              onChange={(start) => onUpdate({
                ...condition,
                conditionData: JSON.stringify({...data, start})
              })}
            />
            <DateTimePicker
              label="End Date"
              value={data.end}
              onChange={(end) => onUpdate({
                ...condition,
                conditionData: JSON.stringify({...data, end})
              })}
            />
          </div>
        )}
      </div>
      
      <div className="condition-footer">
        <Checkbox
          label="Required (item hidden if condition fails)"
          checked={condition.isRequired}
          onChange={(isRequired) => onUpdate({...condition, isRequired})}
        />
      </div>
    </Card>
  );
};

// ExpressionEditor.tsx - Advanced mode for complex expressions
const ExpressionEditor = ({ conditions, onChange }) => {
  // Convert multiple conditions to single expression
  const toExpression = (conds: RenderCondition[]): string => {
    return conds.map(c => {
      const data = JSON.parse(c.conditionData);
      switch (c.type) {
        case 'PLAYER_LEVEL':
          return `player.level >= ${data.minLevel}`;
        case 'PLAYER_RANK':
          return `player.rank IN [${data.ranks.map(r => `"${r}"`).join(', ')}]`;
        case 'PERMISSION':
          return `hasPermission("${data.node}")`;
        case 'EXPRESSION':
          return `(${data.expression})`;
        default:
          return 'true';
      }
    }).join(' AND ');
  };
  
  const fromExpression = (expr: string): RenderCondition[] => {
    // Parse expression back to conditions (or keep as single EXPRESSION condition)
    return [{
      type: 'EXPRESSION',
      conditionData: JSON.stringify({expression: expr}),
      isRequired: true
    }];
  };
  
  const [expression, setExpression] = useState(toExpression(conditions));
  
  const handleSave = () => {
    onChange(fromExpression(expression));
  };
  
  return (
    <div className="expression-editor">
      <CodeEditor
        value={expression}
        onChange={setExpression}
        language="expression"
        height="200px"
        options={{
          lineNumbers: true,
          autocompletion: true,
          syntaxHighlighting: true
        }}
      />
      
      <div className="expression-help">
        <h4>Available Variables:</h4>
        <ul>
          <li><code>player.level</code> - Player's current level</li>
          <li><code>player.coins</code> - Player's coin balance</li>
          <li><code>player.rank</code> - Player's rank (OWNER, ADMIN, etc.)</li>
          <li><code>player.town</code> - Player's town name (null if no town)</li>
          <li><code>player.townRole</code> - Player's role in town</li>
        </ul>
        
        <h4>Functions:</h4>
        <ul>
          <li><code>hasPermission("node")</code> - Check permission</li>
          <li><code>inDateRange("start", "end")</code> - Check date range</li>
        </ul>
        
        <h4>Examples:</h4>
        <CodeBlock>
          player.level >= 10 AND player.coins >= 1000
          <br/>
          player.rank == "ADMIN" OR hasPermission("k&k.menu.admin")
          <br/>
          player.town != null AND player.townRole IN ["OWNER", "COOWNER"]
        </CodeBlock>
      </div>
      
      <Button onClick={handleSave}>Save Expression</Button>
    </div>
  );
};
```

**Example: Creating a Condition in Web App**

1. User clicks "Add Level Condition"
2. Visual builder shows:
   ```
   [Player Level] [>=] [10] [Required ✓] [Delete]
   ```
3. User saves template
4. Web App sends to API:
   ```json
   {
     "menuItemTemplateId": 42,
     "conditions": [
       {
         "type": "PLAYER_LEVEL",
         "conditionData": "{\"minLevel\": 10}",
         "isRequired": true
       }
     ]
   }
   ```
5. API saves to `MenuRenderConditions` table
6. Plugin fetches and caches template
7. Player opens menu → condition evaluated → item shown/hidden

**Acceptance Criteria:**
- [ ] Items can have multiple conditions (AND logic)
- [ ] Sections can have conditions that hide entire section (level, coins, rank, town, etc.)
- [ ] **Web App provides BOTH visual builder AND expression editor for conditions**
- [ ] **Conditions persisted as JSON in database**
- [ ] **All condition types (PlayerLevel, Permission, Expression, etc.) are editable in Web App**
- [ ] **Expression syntax validated before save**
- [ ] Conditions are evaluated efficiently (cached where possible)
- [ ] Invalid conditions log warnings but don't crash renderer
- [ ] **Preview endpoint shows menu with conditions evaluated for specific test player**
- [ ] Conditions are evaluated efficiently (cached where possible)
- [ ] Invalid conditions log warnings but don't crash renderer

---

## Addressing Improvement Comments

### Comment 1: Why Remove NMS Dependencies?

**Your Concern**: "I remember it being the only way of making certain effects or sounds happen."

**Clarification**: 

You're right that **in older versions (1.8-1.16)**, NMS was often necessary for:
- Custom particle effects
- Title/subtitle messages
- Tab list header/footer
- Certain sounds or animations

**However, in modern Paper 1.20+:**
- **Adventure API** provides all text formatting (titles, tab lists, chat components)
- **Bukkit API** supports all sounds, particles, and effects natively
- **Paper-specific enhancements** add features that previously required NMS

**What we're removing from legacy:**
```java
// Legacy code (NMS-dependent):
PacketPlayOutPlayerListHeaderFooter packet = new PacketPlayOutPlayerListHeaderFooter(...);
((CraftPlayer)player).getHandle().f.b(packet);
```

**Modern replacement:**
```java
// Modern Paper/Adventure API:
Component header = Component.text("§7Welcome to §9Knights and Kings");
Component footer = Component.text("§cOpen Beta");
player.sendPlayerListHeaderAndFooter(header, footer);
```

**Decision**: 
- **Keep NMS if absolutely necessary** for specific advanced features not available in Bukkit/Paper API
- **Document any NMS usage** with justification and Paper version compatibility notes
- **Prioritize Bukkit/Paper API** whenever possible for long-term maintainability
- **Use reflection sparingly** with fallback mechanisms

**Updated Requirement:**
✅ **NFR-NMS-1**: Minimize NMS dependencies; use only when Bukkit/Paper API cannot achieve the requirement. Document all NMS usage with version compatibility matrix.

---

### Comment 2: Observer Pattern as Requirement

**Your Request**: "Please add this as requirement."

**Updated Requirement:**

**FR-EVENT-1: Menu Event System** (Priority: High)

Menu system must broadcast state change events to registered listeners:

```java
// MenuStateListener.java (knk-core - port/interface)
public interface MenuStateListener {
  void onMenuOpened(MenuOpenedEvent event);
  void onMenuClosed(MenuClosedEvent event);
  void onItemClicked(ItemClickedEvent event);
  void onMenuUpdated(MenuUpdatedEvent event);
  void onPageChanged(PageChangedEvent event);
}

// Event classes (knk-core)
public record MenuOpenedEvent(
  MenuTemplate template,
  Player player,
  MenuSession session,
  Instant timestamp
) {}

public record MenuClosedEvent(
  MenuTemplate template,
  Player player,
  CloseReason reason,
  Duration openDuration,
  Instant timestamp
) {}

public record ItemClickedEvent(
  MenuItemTemplate item,
  Player player,
  ClickType clickType,
  boolean cancelled,
  Instant timestamp
) {}

public record MenuUpdatedEvent(
  MenuTemplate template,
  UpdateReason reason,
  List<Integer> affectedSlots,
  Instant timestamp
) {}

public enum CloseReason {
  PLAYER_CLOSED,
  MENU_NAVIGATED,
  PLUGIN_FORCED,
  SERVER_SHUTDOWN
}

public enum UpdateReason {
  VARIABLE_CHANGED,
  CONDITION_CHANGED,
  DATA_REFRESHED,
  MANUAL_UPDATE
}
```

**Implementation:**

```java
// MenuSession.java (knk-paper)
public class MenuSession {
  private final List<MenuStateListener> listeners = new CopyOnWriteArrayList<>();
  private final MenuTemplate template;
  private final Player player;
  private final Instant openedAt;
  
  public void addListener(MenuStateListener listener) {
    listeners.add(listener);
  }
  
  public void removeListener(MenuStateListener listener) {
    listeners.remove(listener);
  }
  
  void notifyOpened() {
    MenuOpenedEvent event = new MenuOpenedEvent(template, player, this, Instant.now());
    listeners.forEach(l -> {
      try {
        l.onMenuOpened(event);
      } catch (Exception ex) {
        LOGGER.error("Error in menu opened listener", ex);
      }
    });
  }
  
  void notifyClosed(CloseReason reason) {
    Duration duration = Duration.between(openedAt, Instant.now());
    MenuClosedEvent event = new MenuClosedEvent(template, player, reason, duration, Instant.now());
    listeners.forEach(l -> {
      try {
        l.onMenuClosed(event);
      } catch (Exception ex) {
        LOGGER.error("Error in menu closed listener", ex);
      }
    });
  }
  
  void notifyItemClicked(ItemClickedEvent event) {
    listeners.forEach(l -> {
      try {
        l.onItemClicked(event);
      } catch (Exception ex) {
        LOGGER.error("Error in item clicked listener", ex);
      }
    });
  }
}
```

**Use Cases:**

1. **Analytics/Logging**:
   ```java
   session.addListener(new MenuAnalyticsListener(analyticsService));
   ```

2. **Quest/Achievement Tracking**:
   ```java
   session.addListener(new QuestProgressListener(questService));
   ```

3. **Anti-Cheat Integration**:
   ```java
   session.addListener(new MenuInteractionMonitor(antiCheatService));
   ```

**Acceptance Criteria:**
- [ ] MenuSession supports listener registration/unregistration
- [ ] All menu state changes broadcast events
- [ ] Listeners execute safely (exceptions don't break menu)
- [ ] Events include all relevant context (player, template, timestamp)
- [ ] Async listeners supported (don't block menu operations)

---

### Comment 3: Async Rendering Pipeline

**Your Feedback**: "Sounds like a really good option to improve overall performance."

**Implementation Strategy:**

```java
// MenuRenderer.java (knk-core - interface)
public interface MenuRenderer {
  CompletableFuture<Inventory> renderAsync(MenuTemplate template, MenuSession session);
  Inventory renderSync(MenuTemplate template, MenuSession session);
}

// BukkitMenuRenderer.java (knk-paper - implementation)
public class BukkitMenuRenderer implements MenuRenderer {
  private final Executor renderExecutor = Executors.newFixedThreadPool(4);
  
  @Override
  public CompletableFuture<Inventory> renderAsync(MenuTemplate template, MenuSession session) {
    return CompletableFuture.supplyAsync(() -> {
      // Heavy calculations off main thread
      RenderPlan plan = calculateRenderPlan(template, session);
      List<PreparedItem> items = prepareItems(plan, session);
      
      // Return to main thread for inventory operations
      return Bukkit.getScheduler().callSyncMethod(plugin, () -> {
        Inventory inv = createInventory(template);
        applyItemsToInventory(inv, items);
        return inv;
      }).get();
    }, renderExecutor);
  }
  
  @Override
  public Inventory renderSync(MenuTemplate template, MenuSession session) {
    // Fallback for simple menus or when async not needed
    RenderPlan plan = calculateRenderPlan(template, session);
    List<PreparedItem> items = prepareItems(plan, session);
    Inventory inv = createInventory(template);
    applyItemsToInventory(inv, items);
    return inv;
  }
  
  private record PreparedItem(int slot, ItemStack itemStack) {}
}

// Usage
public void openMenu(Player player, String menuName) {
  MenuTemplate template = menuCache.getByName(menuName).orElseThrow();
  MenuSession session = new MenuSession(player, template);
  
  // Show loading screen
  Inventory loading = createLoadingInventory();
  player.openInventory(loading);
  
  // Render asynchronously
  renderer.renderAsync(template, session)
    .thenAccept(inventory -> {
      // Update player's view on main thread
      Bukkit.getScheduler().runTask(plugin, () -> {
        player.openInventory(inventory);
        session.notifyOpened();
      });
    })
    .exceptionally(ex -> {
      LOGGER.error("Error rendering menu", ex);
      player.closeInventory();
      player.sendMessage("Error loading menu");
      return null;
    });
}
```

**Performance Targets:**
- Simple menus (<50 items): <50ms total
- Complex menus (50-200 items): <200ms total
- Very large menus (200+ items): <500ms with async rendering

**Acceptance Criteria:**
- [ ] Rendering calculations happen off main thread
- [ ] Inventory updates happen on main thread (thread-safe)
- [ ] Loading screen shown for slow-rendering menus
- [ ] Errors in async rendering don't crash plugin
- [ ] Performance metrics logged for monitoring

---

### Comment 4: Comprehensive Validation Framework

**Your Request**: "Please add comprehensive validation framework."

**Implementation:**

```java
// MenuValidator.java (knk-core)
public interface MenuValidator {
  ValidationResult validate(MenuTemplate template);
  ValidationResult validateSection(MenuSectionTemplate section);
  ValidationResult validateItem(MenuItemTemplate item);
}

public record ValidationResult(
  boolean isValid,
  List<ValidationError> errors,
  List<ValidationWarning> warnings
) {
  public static ValidationResult success() {
    return new ValidationResult(true, List.of(), List.of());
  }
  
  public static ValidationResult failure(List<ValidationError> errors) {
    return new ValidationResult(false, errors, List.of());
  }
  
  public ValidationResult merge(ValidationResult other) {
    List<ValidationError> allErrors = new ArrayList<>(errors);
    allErrors.addAll(other.errors());
    List<ValidationWarning> allWarnings = new ArrayList<>(warnings);
    allWarnings.addAll(other.warnings());
    return new ValidationResult(isValid && other.isValid(), allErrors, allWarnings);
  }
}

public record ValidationError(
  String code,
  String message,
  ValidationSeverity severity,
  String path // e.g., "sections[0].items[2].material"
) {}

public record ValidationWarning(
  String code,
  String message,
  String path,
  String suggestion
) {}

public enum ValidationSeverity {
  ERROR,    // Blocks rendering
  WARNING,  // Logs but allows rendering
  INFO      // Informational only
}

// MenuValidatorImpl.java (knk-paper)
public class MenuValidatorImpl implements MenuValidator {
  
  @Override
  public ValidationResult validate(MenuTemplate template) {
    List<ValidationError> errors = new ArrayList<>();
    List<ValidationWarning> warnings = new ArrayList<>();
    
    // Validate dimensions
    if (template.height() < 1 || template.height() > 6) {
      errors.add(new ValidationError(
        "INVALID_HEIGHT",
        "Menu height must be 1-6, got: " + template.height(),
        ValidationSeverity.ERROR,
        "height"
      ));
    }
    
    if (template.width() != 9) {
      errors.add(new ValidationError(
        "INVALID_WIDTH",
        "Menu width must be 9, got: " + template.width(),
        ValidationSeverity.ERROR,
        "width"
      ));
    }
    
    // Validate sections
    Set<Integer> occupiedSlots = new HashSet<>();
    for (int i = 0; i < template.sections().size(); i++) {
      MenuSectionTemplate section = template.sections().get(i);
      ValidationResult sectionResult = validateSection(section);
      
      if (!sectionResult.isValid()) {
        errors.addAll(sectionResult.errors());
      }
      warnings.addAll(sectionResult.warnings());
      
      // Check slot collisions
      Set<Integer> sectionSlots = calculateSlots(section);
      for (int slot : sectionSlots) {
        if (slot >= template.height() * 9) {
          errors.add(new ValidationError(
            "SLOT_OUT_OF_BOUNDS",
            "Section slot " + slot + " exceeds menu bounds",
            ValidationSeverity.ERROR,
            "sections[" + i + "]"
          ));
        }
        
        if (occupiedSlots.contains(slot)) {
          warnings.add(new ValidationWarning(
            "SLOT_COLLISION",
            "Slot " + slot + " is occupied by multiple sections",
            "sections[" + i + "]",
            "Use priority to control layering"
          ));
        }
        occupiedSlots.add(slot);
      }
    }
    
    // Validate background material
    if (template.backgroundMaterial() != null) {
      if (!isValidMaterial(template.backgroundMaterial())) {
        errors.add(new ValidationError(
          "INVALID_MATERIAL",
          "Unknown background material: " + template.backgroundMaterial(),
          ValidationSeverity.ERROR,
          "backgroundMaterial"
        ));
      }
    }
    
    return new ValidationResult(errors.isEmpty(), errors, warnings);
  }
  
  @Override
  public ValidationResult validateSection(MenuSectionTemplate section) {
    List<ValidationError> errors = new ArrayList<>();
    List<ValidationWarning> warnings = new ArrayList<>();
    
    // Validate dimensions
    if (section.width() < 1 || section.width() > 9) {
      errors.add(new ValidationError(
        "INVALID_SECTION_WIDTH",
        "Section width must be 1-9, got: " + section.width(),
        ValidationSeverity.ERROR,
        "width"
      ));
    }
    
    if (section.height() < 1) {
      errors.add(new ValidationError(
        "INVALID_SECTION_HEIGHT",
        "Section height must be >= 1, got: " + section.height(),
        ValidationSeverity.ERROR,
        "height"
      ));
    }
    
    // Validate position
    if (section.startX() < 0 || section.startX() >= 9) {
      errors.add(new ValidationError(
        "INVALID_START_X",
        "Section startX must be 0-8, got: " + section.startX(),
        ValidationSeverity.ERROR,
        "startX"
      ));
    }
    
    if (section.startY() < 0) {
      errors.add(new ValidationError(
        "INVALID_START_Y",
        "Section startY must be >= 0, got: " + section.startY(),
        ValidationSeverity.ERROR,
        "startY"
      ));
    }
    
    // Validate items
    for (int i = 0; i < section.items().size(); i++) {
      MenuItemTemplate item = section.items().get(i);
      ValidationResult itemResult = validateItem(item);
      
      if (!itemResult.isValid()) {
        errors.addAll(itemResult.errors().stream()
          .map(e -> new ValidationError(
            e.code(),
            e.message(),
            e.severity(),
            "items[" + i + "]." + e.path()
          ))
          .toList());
      }
    }
    
    return new ValidationResult(errors.isEmpty(), errors, warnings);
  }
  
  @Override
  public ValidationResult validateItem(MenuItemTemplate item) {
    List<ValidationError> errors = new ArrayList<>();
    List<ValidationWarning> warnings = new ArrayList<>();
    
    // Validate material
    if (!isValidMaterial(item.material())) {
      errors.add(new ValidationError(
        "INVALID_MATERIAL",
        "Unknown material: " + item.material(),
        ValidationSeverity.ERROR,
        "material"
      ));
    }
    
    // Validate amount
    if (item.amount() < 1 || item.amount() > 64) {
      errors.add(new ValidationError(
        "INVALID_AMOUNT",
        "Item amount must be 1-64, got: " + item.amount(),
        ValidationSeverity.ERROR,
        "amount"
      ));
    }
    
    // Validate variable strings
    if (item.displayNameTemplate() != null) {
      ValidationResult varResult = validateVariableString(item.displayNameTemplate());
      if (!varResult.isValid()) {
        errors.addAll(varResult.errors());
      }
    }
    
    for (MenuItemLoreTemplate lore : item.lore()) {
      ValidationResult varResult = validateVariableString(lore.loreLineTemplate());
      if (!varResult.isValid()) {
        errors.addAll(varResult.errors());
      }
    }
    
    // Validate conditions
    for (RenderCondition condition : item.conditions()) {
      ValidationResult condResult = validateCondition(condition);
      if (!condResult.isValid()) {
        errors.addAll(condResult.errors());
      }
    }
    
    return new ValidationResult(errors.isEmpty(), errors, warnings);
  }
  
  private ValidationResult validateVariableString(String template) {
    // Check for valid placeholder syntax: $variable.path$
    Pattern pattern = Pattern.compile("\\$([\w.]+)\\$");
    Matcher matcher = pattern.matcher(template);
    
    List<ValidationError> errors = new ArrayList<>();
    while (matcher.find()) {
      String path = matcher.group(1);
      if (path.isEmpty()) {
        errors.add(new ValidationError(
          "EMPTY_VARIABLE",
          "Variable placeholder cannot be empty",
          ValidationSeverity.ERROR,
          "displayNameTemplate"
        ));
      }
    }
    
    return new ValidationResult(errors.isEmpty(), errors, List.of());
  }
  
  private ValidationResult validateCondition(RenderCondition condition) {
    // Validate condition data is valid JSON and has required fields
    List<ValidationError> errors = new ArrayList<>();
    
    try {
      JsonNode data = parseJson(condition.conditionData());
      
      switch (condition.type()) {
        case PLAYER_LEVEL -> {
          if (!data.has("minLevel")) {
            errors.add(new ValidationError(
              "MISSING_MIN_LEVEL",
              "PlayerLevel condition requires 'minLevel' field",
              ValidationSeverity.ERROR,
              "conditions"
            ));
          }
        }
        case PERMISSION -> {
          if (!data.has("node")) {
            errors.add(new ValidationError(
              "MISSING_PERMISSION_NODE",
              "Permission condition requires 'node' field",
              ValidationSeverity.ERROR,
              "conditions"
            ));
          }
        }
        // ... other condition types
      }
    } catch (JsonProcessingException ex) {
      errors.add(new ValidationError(
        "INVALID_JSON",
        "Condition data is not valid JSON: " + ex.getMessage(),
        ValidationSeverity.ERROR,
        "conditions"
      ));
    }
    
    return new ValidationResult(errors.isEmpty(), errors, List.of());
  }
  
  private boolean isValidMaterial(String material) {
    try {
      Material.valueOf(material.toUpperCase());
      return true;
    } catch (IllegalArgumentException ex) {
      return false;
    }
  }
}
```

**Validation Execution:**

```java
// Before rendering
public void openMenu(Player player, String menuName) {
  MenuTemplate template = menuCache.getByName(menuName).orElseThrow();
  
  // Validate template
  ValidationResult result = validator.validate(template);
  
  if (!result.isValid()) {
    LOGGER.error("Menu validation failed for {}: {}", menuName, result.errors());
    player.sendMessage("§cError: Menu is misconfigured");
    return;
  }
  
  if (!result.warnings().isEmpty()) {
    LOGGER.warn("Menu validation warnings for {}: {}", menuName, result.warnings());
  }
  
  // Proceed with rendering...
}
```

**Web API Validation:**

```csharp
// Before saving template to database
public async Task<ActionResult<MenuTemplateDetailDto>> Create(CreateMenuTemplateDto dto)
{
  // Server-side validation using same rules
  var validationResult = await _menuValidator.ValidateAsync(dto);
  
  if (!validationResult.IsValid)
  {
    return BadRequest(new {
      errors = validationResult.Errors.Select(e => new {
        code = e.Code,
        message = e.Message,
        path = e.Path
      })
    });
  }
  
  // Save to database...
}
```

**Acceptance Criteria:**
- [ ] Validation runs on template load from API
- [ ] Validation runs before rendering
- [ ] Web API validates templates before saving
- [ ] All validation errors include actionable messages
- [ ] Warnings logged but don't block functionality
- [ ] Validation results cached to avoid repeated checks
- [ ] Custom validators can be registered via plugin system

---

### Comment 5: Separate Rendering Engine - Pros & Cons

**Pros:**

1. **Testability**
   - Render logic can be tested without Bukkit/Paper server
   - Mock rendering engine for unit tests
   - Easier to write integration tests

2. **Flexibility**
   - Swap rendering strategies (e.g., different layout algorithms)
   - Support multiple Minecraft versions with version-specific renderers
   - A/B testing different rendering approaches

3. **Maintainability**
   - Clear separation between menu structure (domain) and display (implementation)
   - Changes to rendering don't affect menu definitions
   - Easier onboarding for new developers (focused modules)

4. **Performance**
   - Can optimize rendering separately from business logic
   - Profile rendering in isolation
   - Cache rendering calculations without affecting menu state

5. **Extensibility**
   - Third-party plugins can provide custom renderers
   - Support for different display targets (chat-based menus, bossbar menus, etc.)

**Cons:**

1. **Increased Complexity**
   - More interfaces and abstractions
   - Steeper learning curve for developers
   - More files to navigate

2. **Potential Over-Engineering**
   - If we only ever have one renderer, abstraction might be unnecessary
   - Added indirection can make debugging harder

3. **Performance Overhead**
   - Extra method calls through interfaces (minimal in practice)
   - Potential duplication of data structures

4. **Initial Development Time**
   - More upfront design work
   - Need to define clear contracts between layers

**Recommendation:**

✅ **Implement Separated Rendering Engine**

The pros significantly outweigh the cons for this project because:
- We already have multiple modules (knk-core, knk-paper) enforcing separation
- Testing is critical for menu system stability
- Future versions may need different rendering strategies
- Performance optimization is a stated goal

**Implementation Approach:**

```
knk-core (domain + ports):
- MenuTemplate, MenuSection, MenuItem (data structures)
- MenuRenderer interface (port)
- ConditionEvaluator interface (port)
- VariableResolver interface (port)

knk-paper (implementation):
- BukkitMenuRenderer implements MenuRenderer
- PaperConditionEvaluator implements ConditionEvaluator
- PaperVariableResolver implements VariableResolver
- MenuUtil (helper functions)
```

This keeps the architecture clean while allowing Paper-specific optimizations.

---

### Comment 6: Keyboard Navigation for Inventory Menus

**Question**: "Is keyboard navigation possible in a minecraft server for inventory menus?"

**Short Answer**: **Yes, but with limitations.**

**Technical Details:**

Minecraft Java Edition supports keyboard navigation in inventories:
- **Tab key**: Cycle through inventory slots (vanilla behavior)
- **Shift+Click**: Quick transfer items
- **Number keys**: Swap hotbar items
- **Arrow keys**: No native support for arrow key navigation in inventories

**What's Possible:**

1. **Tab Cycling** (Native)
   - Already works out-of-the-box
   - Players can Tab through slots
   - Server can detect which slot is selected via `InventoryClickEvent`

2. **Hotkey Bindings** (Custom)
   - Players can bind custom keys via client-side mods (OptiFine, Fabric, etc.)
   - Server cannot force client-side key bindings
   - Not portable across vanilla clients

3. **Chat-Based Navigation** (Workaround)
   - Close inventory, type command to navigate
   - Not truly "keyboard navigation" in inventory
   - Poor UX

4. **Arrow Key Navigation** (Requires Client Mod)
   - Would require custom client-side mod
   - Not achievable server-side only
   - Breaks vanilla client compatibility

**Recommendation:**

❌ **Do NOT prioritize keyboard navigation for v1.0**

Reasons:
- Requires client-side mods (not server-side only)
- Limited demand from players (most use mouse)
- Complex implementation for marginal benefit
- Vanilla Tab cycling already exists

**Alternative**: Focus on **mouse accessibility** instead:
- Clear visual indicators for clickable items
- Hover tooltips explaining item function
- Consistent button placement (e.g., "Back" always in bottom-left)
- Keyboard shortcuts for menu commands (e.g., `/menu back`, `/menu next`)

**Future Consideration:**

If there's strong demand, consider:
- Optional Forge/Fabric mod companion for enhanced navigation
- Command-based navigation as fallback
- Voice command integration via third-party mod

**Updated Requirement:**

**FR-ACCESS-1: Menu Accessibility** (Priority: Low)

- [ ] Consistent button placement across all menus
- [ ] Clear hover tooltips for all interactive items
- [ ] High-contrast colors for important items
- [ ] Command-based menu navigation (`/menu next`, `/menu back`, `/menu close`)
- [ ] Screen reader-friendly text (avoid Unicode art in critical info)

**Future (v2.0):**
- [ ] Optional client mod for arrow key navigation
- [ ] Voice command integration via plugin hooks

---

## Implementation Phases

### Phase 1: Foundation (Weeks 1-3)

**knk-core:**
- [ ] Define domain models (MenuTemplate, MenuSectionTemplate, MenuItemTemplate)
- [ ] Define ports/interfaces (MenuRenderer, ConditionEvaluator, VariableResolver)
- [ ] Implement RenderCondition and ConditionType
- [ ] Create base event classes (MenuOpenedEvent, etc.)

**knk-web-api-v2:**
- [ ] Create database schema (MenuTemplates, MenuSectionTemplates, etc.)
- [ ] Implement MenuTemplate entity models
- [ ] Create MenuTemplateRepository
- [ ] Create MenuTemplateService with validation

**knk-api-client:**
- [ ] Implement MenuTemplateQueryApi
- [ ] Implement MenuTemplateCommandApi
- [ ] Create DTO mapping (API DTO → Domain model)

**Deliverables:**
- Menu templates can be created/stored in database
- API client can fetch templates
- Domain models compiled and documented

---

### Phase 2: Core Rendering (Weeks 4-6)

**knk-paper:**
- [ ] Implement MenuUtil (slot calculations, coordinate conversions)
- [ ] Implement MenuSession (runtime state management)
- [ ] Implement BukkitMenuRenderer (basic rendering without conditions)
- [ ] Implement MenuTemplateCache
- [ ] Create basic inventory click listener

**Testing:**
- [ ] Unit tests for slot calculations
- [ ] Integration test: render simple menu from template
- [ ] Validate slot collision detection

**Deliverables:**
- Simple menus can be rendered from templates
- Players can open menus via command
- Click events are captured (no actions yet)

---

### Phase 3: Variable System (Weeks 7-8)

**knk-core:**
- [ ] Define VariableString data structure
- [ ] Define VariableResolver interface

**knk-paper:**
- [ ] Implement PaperVariableResolver
- [ ] Integrate with player/user context
- [ ] Support method chaining (e.g., `$player.getName$`)
- [ ] Implement result caching

**Testing:**
- [ ] Unit tests for variable parsing
- [ ] Integration test: menu with dynamic player names
- [ ] Performance test: 1000 variable resolutions

**Deliverables:**
- Menu items can display player-specific data
- Lore lines resolve variables correctly
- Variable resolution is performant

---

### Phase 4: Conditional Rendering (Weeks 9-11)

**knk-core:**
- [ ] Define ConditionEvaluator interface
- [ ] Implement condition type enums and data structures

**knk-paper:**
- [ ] Implement PaperConditionEvaluator
- [ ] Support all condition types (PlayerLevel, Permission, etc.)
- [ ] Implement expression evaluator for complex conditions
- [ ] Integrate conditions into renderi (with JSON serialization)
- [ ] Validate condition data on save (check JSON structure, required fields)
- [ ] Add expression syntax validation endpoint (`POST /api/menus/conditions/validate`)
- [ ] Add expression test endpoint (`POST /api/menus/conditions/test` - evaluate against sample player data)
**knk-web-api-v2:**
- [ ] Add MenuRenderConditions table
- [ ] Update DTOs to include conditions
- [ ] Validate condition data on save

**Testing:**
- [ ] Unit tests for each condition type
- [ ] Integration test: menu with level-based items
- [ ] Integration test: menu with permission-based sections

**Deliverables:**
- Items can be conditionally rendered
- Sections can be hidden based on conditions
- Lore lines can have individual conditions

---

### Phase 5: Event System & Actions (Weeks 12-13)

**knk-core:**
- [ ] Define MenuStateListener interface
- [ ] Create all event record classes

**knk-paper:**
- [ ] Implement event broadcasting in MenuSession
- [ ] Create ClickAction system
- [ ] Implement common actions (OpenMenu, RunCommand, CloseMenu)
- [ ] Add listener management to MenuSession

**Testing:**
- [ ] Unit tests for event broadcasting
- [ ] Integration test: listener receives events
- [ ] Integration test: click actions execute correctly

**Deliverables:**
- External systems can listen to menu events
- Click actions are configurable via templates
- Menu navigation works (forward/back)

---

### Phase 6: Validation Framework (Weeks 14-15)

**knk-core:**
- [ ] Define MenuValidator interface
- [ ] Create ValidationResult data structures

**knk-paper:**
- [ ] Implement MenuValidatorImpl
- [ ] Add pre-render validation checks
- [ ] Implement validation result logging

**knk-web-api-v2:**
- [ ] Add server-side validation before save
- [ ] Return validation errors in API responses

**Testing:**
- [ ] Unit tests for all validation rules
- [ ] Integration test: invalid template rejected
- [ ] Integration test: warnings logged but menu renders

**Deliverables:**
- Templates validated before rendering
- Web API validates before saving
- Clear error messages for misconfigured templates

---

### Phase 7: Async Rendering & Performance (Weeks 16-17)

**knk-paper:**
- [ ] Implement async rendering pipeline
- [ ] Add loading screen for slow renders
- [ ] Optimize slot calculations (memoization)
- [ ] Implement ItemStack pooling for common items
- [ ] Add performance metrics logging

**Testing:**
- [ ] Performance benchmark: 1000-item menu render time
- [ ] Load test: 50 players opening menus simultaneously
- [ ] Memory leak test: repeated open/close cycles

**Deliverables:**
- Large menus render without blocking server
- Performance meets targets (<500ms for 200+ items)
- No memory leaks in rendering pipeline

---

### Phase 8: Web App Integration (Weeks 18-20)
**Create ConditionBuilder component (visual mode)**
- [ ] **Create ExpressionEditor component (advanced mode)**
- [ ] **Create ConditionEditor component (individual condition editing)**
- [ ] Create VariableStringEditor component
- [ ] Add preview functionality (with player selector for condition testing)
- [ ] Add expression syntax highlighting and autocomplete
- [ ] Add real-time expression validationomponent
- [ ] Create MenuTemplateEditor component
- [ ] Create MenuSectionEditor component
- [ ] Create MenuItemEditor component
- [ ] Create ConditionBuilder component
- [ ] Create VariableStringEditor component
- [ ] Add preview functionality

**knk-web-api-v2:**
- [ ] Add preview endpoint (render menu for specific player)
- [ ] Add clone endpoint
- [ ] Add version history endpoints

**Testing:**
- [ ] E2E test: create menu in web app, open in-game
- [ ] E2E test: edit menu in web app, changes reflect in-game
- [ ] E2E test: condition builder generates valid JSON

**Deliverables:**
- Web app can create/edit menu templates
- Preview shows what players will see
- Changes sync to plugin within 60 seconds

---

### Phase 9: Polish & Documentation (Weeks 21-22)

**Documentation:**
- [ ] API documentation (Swagger)
- [ ] Developer guide (how to create custom renderers)
- [ ] Template author guide (variable syntax, conditions)
- [ ] Migration guide (legacy → v2)

**Preset Templates:**
- [ ] Confirmation dialog template
- [ ] Paginated list template
- [ ] Form input template
- [ ] Settings menu template

**Enhancements:**
- [ ] Add more click action types
- [ ] Add more condition types
- [ ] Improve error messages
- [ ] Add debug mode (visualize sections/slots)

**Testing:**
- [ ] Full regression test suite
- [ ] User acceptance testing with sample players
- [ ] Performance regression testing

**Deliverables:**
- Complete documentation
- Common templates ready to use
- Production-ready system

---

### Phase 10: Deployment & Migration (Week 23+)

**Migration:**
- [ ] Create adapter: legacy Menu → MenuTemplate
- [ ] Migrate existing menus to database
- [ ] Run legacy + v2 systems in parallel
- [ ] Gradual cutover (one menu at a time)

**Monitoring:**
- [ ] Set up metrics collection (render times, error rates)
- [ ] Set up alerting (validation failures, slow renders)
- [ ] Create dashboard (menu usage statistics)

**Rollout:**
- [ ] Deploy to test server
- [ ] Beta test with select players
- [ ] Monitor for issues
- [ ] Deploy to production
- [ ] Remove legacy system

**Deliverables:**
- All menus migrated to v2
- Legacy system removed
- Monitoring in place
- Production stable

---

## Success Metrics

### Performance
- Menu render time: <200ms for 95% of menus
- API response time: <100ms for template fetch
- Cache hit rate: >90%
- Memory usage: <500MB for all cached templates

### Reliability
- Validation error rate: <1% of template saves
- Rendering error rate: <0.1% of opens
- Event delivery success rate: >99.9%

### Usability
- Template creation time: <5 minutes for simple menu
- Developer onboarding time: <2 hours to create first custom menu
- Player satisfaction: >80% positive feedback

### Adoption
- 100% of menus migrated to v2 within 3 months
- 5+ custom templates created by community
- 0 rollbacks to legacy system

---

## Risk Mitigation

| Risk | Mitigation |
|------|-----------|
| API latency impacts menu open speed | Aggressive caching, async rendering, fallback to cached version |
| Template validation too strict, blocks legitimate menus | Warnings vs errors, validation can be disabled per-template |
| Condition evaluation performance degrades | Cache evaluation results, limit expression complexity |
| Web app and plugin template versions diverge | Version field in template, migration system |
| Complex menus cause lag | Async rendering, performance budgets, profiling |
| Legacy migration fails for some menus | Keep legacy system as fallback, gradual migration |

---

## Next Steps

1. **Review this roadmap** with team
2. **Prioritize phases** based on business needs
3. **Assign developers** to modules (knk-core, knk-web-api-v2, knk-paper, knk-web-app)
4. **Set up project tracking** (Jira, GitHub Projects, etc.)
5. **Create Phase 1 sprint plan** with detailed tasks
6. **Begin implementation** 🚀

---

## Questions for Stakeholders

- [ ] Confirm priority: Should web app integration come earlier?
- [ ] Confirm scope: Are there additional condition types needed?
- [ ] Confirm timeline: Is 23 weeks acceptable?
- [ ] Confirm resources: How many developers available?
- [ ] Confirm testing: Will we have dedicated QA?
