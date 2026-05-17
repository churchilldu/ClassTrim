# ClassTrim MCP Server — Design Document

## Overview

A Model Context Protocol (MCP) server that exposes ClassTrim's move-method refactoring engine to AI coding assistants (Kiro, Claude Desktop, Cursor, etc.) over JSON-RPC via stdio. The server wraps `classtrim-core`'s existing `ClassTrimService` API and adds structured tool interfaces for analysis, metrics inspection, and suggestion explanation.

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                        AI Coding Assistant                           │
│                   (Kiro / Claude Desktop / Cursor)                   │
└────────────────────────────────┬────────────────────────────────────┘
                                 │ JSON-RPC over stdio
                                 │ (MCP protocol)
                                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      classtrim-mcp (Java process)                    │
│                                                                     │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │                    MCP Server (stdio transport)                │  │
│  │                                                               │  │
│  │  Tools:                                                       │  │
│  │    ├── analyze_project    → run NSGA-III/II optimization      │  │
│  │    ├── get_metrics        → inspect WMC/CBO/RFC per class     │  │
│  │    ├── list_algorithms    → enumerate available algorithms    │  │
│  │    └── explain_suggestion → coupling analysis for a move      │  │
│  │                                                               │  │
│  │  Resources:                                                   │  │
│  │    └── classtrim://last-result → most recent analysis result  │  │
│  └───────────────────────────────────────────────────────────────┘  │
│                                 │                                    │
│                                 ▼                                    │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │                      classtrim-core                            │  │
│  │                                                               │  │
│  │  ClassTrimService.analyze(ProjectSource, RefactoringConfig)   │  │
│  │       │                                                       │  │
│  │       ├── StandardProjectAnalyzer (ASM bytecode parsing)      │  │
│  │       ├── InMemoryProjectRepository                           │  │
│  │       └── RefactoringEngine (NSGA-III / NSGA-II)              │  │
│  │              └── RefactoringProblem (jMetal)                   │  │
│  │                                                               │  │
│  │  Returns: RefactoringResult                                   │  │
│  │    ├── List<RefactoringSuggestion>                            │  │
│  │    │     ├── method (name, descriptor, class)                 │  │
│  │    │     ├── sourceClass (FQN)                                │  │
│  │    │     └── targetClass (FQN)                                │  │
│  │    └── computingTimeMs                                        │  │
│  └───────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    Project's compiled .class files                   │
│              (e.g. target/classes, build/classes/java/main)          │
└─────────────────────────────────────────────────────────────────────┘
```

## Module Layout

```
classtrim-mcp/
├── pom.xml                          # Maven module, depends on classtrim-core + MCP SDK
├── src/main/java/org/classtrim/mcp/
│   ├── ClassTrimMcpServer.java      # Entry point: configures tools, starts stdio server
│   ├── tools/
│   │   ├── AnalyzeProjectTool.java  # analyze_project implementation
│   │   ├── GetMetricsTool.java      # get_metrics implementation
│   │   ├── ListAlgorithmsTool.java  # list_algorithms implementation
│   │   └── ExplainSuggestionTool.java # explain_suggestion implementation
│   └── model/
│       ├── AnalysisRequest.java     # Input DTO for analyze_project
│       ├── AnalysisResponse.java    # Output DTO
│       ├── MetricsResponse.java     # Output DTO for get_metrics
│       └── SuggestionExplanation.java # Output DTO for explain_suggestion
└── README.md
```

## Tools Specification

### 1. `analyze_project`

Runs the full NSGA-III/II optimization and returns move-method suggestions.

**Input Schema:**
```json
{
  "type": "object",
  "properties": {
    "roots": {
      "type": "array",
      "items": { "type": "string" },
      "description": "Absolute paths to directories containing compiled .class files"
    },
    "projectName": {
      "type": "string",
      "description": "Human-readable project name for labeling results",
      "default": "project"
    },
    "wmc": { "type": "integer", "default": 8, "description": "WMC threshold" },
    "cbo": { "type": "integer", "default": 8, "description": "CBO threshold" },
    "rfc": { "type": "integer", "default": 30, "description": "RFC threshold" },
    "populationSize": { "type": "integer", "default": 500, "minimum": 1 },
    "maxIterations": { "type": "integer", "default": 2000, "minimum": 1 },
    "algorithm": {
      "type": "string",
      "enum": ["NSGA-III", "NSGA-II"],
      "default": "NSGA-III"
    },
    "useGuidingObjectives": { "type": "boolean", "default": true }
  },
  "required": ["roots"]
}
```

**Output:**
```json
{
  "suggestions": [
    {
      "method": "processNewOrder(Order)",
      "sourceClass": "org.example.OrderProcessor",
      "targetClass": "org.example.Order"
    }
  ],
  "computingTimeMs": 4523,
  "classCount": 8,
  "methodCount": 32,
  "algorithm": "NSGA-III"
}
```

### 2. `get_metrics`

Parses the project and returns per-class metric values without running the optimization. Useful for understanding the project's current state before deciding whether to run the full analysis.

**Input Schema:**
```json
{
  "type": "object",
  "properties": {
    "roots": {
      "type": "array",
      "items": { "type": "string" },
      "description": "Paths to compiled .class directories"
    },
    "wmc": { "type": "integer", "default": 8 },
    "cbo": { "type": "integer", "default": 8 },
    "rfc": { "type": "integer", "default": 30 }
  },
  "required": ["roots"]
}
```

**Output:**
```json
{
  "classes": [
    {
      "name": "org.example.OrderProcessor",
      "wmc": 15,
      "cbo": 7,
      "rfc": 42,
      "exceedsWmc": true,
      "exceedsCbo": false,
      "exceedsRfc": true,
      "methodCount": 12,
      "refactorableMethodCount": 10
    }
  ],
  "totalClasses": 8,
  "classesExceedingThreshold": 5
}
```

### 3. `list_algorithms`

Returns the available optimization algorithms.

**Input:** none

**Output:**
```json
{
  "algorithms": [
    { "name": "NSGA-III", "description": "Non-dominated Sorting Genetic Algorithm III" },
    { "name": "NSGA-II", "description": "Non-dominated Sorting Genetic Algorithm II" }
  ],
  "default": "NSGA-III"
}
```

### 4. `explain_suggestion`

Given a specific suggestion, explains why the algorithm recommended moving that method — coupling counts, field accesses, method invocations between the method and both the source and target classes.

**Input Schema:**
```json
{
  "type": "object",
  "properties": {
    "roots": { "type": "array", "items": { "type": "string" } },
    "method": { "type": "string", "description": "Method signature, e.g. 'processNewOrder(Order)'" },
    "sourceClass": { "type": "string", "description": "FQN of the source class" },
    "targetClass": { "type": "string", "description": "FQN of the target class" }
  },
  "required": ["roots", "method", "sourceClass", "targetClass"]
}
```

**Output:**
```json
{
  "method": "processNewOrder(Order)",
  "sourceClass": "org.example.OrderProcessor",
  "targetClass": "org.example.Order",
  "couplingToSource": 2,
  "couplingToTarget": 5,
  "reason": "Method accesses 5 fields/methods on Order but only 2 on OrderProcessor — feature envy pattern detected",
  "fieldAccessesOnTarget": ["id", "items", "customer", "placedAt", "shippingAddressOverride"],
  "fieldAccessesOnSource": ["inventory", "auditTrail"]
}
```

## Technology Stack

| Component | Choice | Rationale |
|---|---|---|
| Language | Java 17 | Same as classtrim-core; direct API access, no serialization boundary |
| MCP SDK | `io.modelcontextprotocol.sdk:mcp:1.0.0` | Official Java SDK with built-in stdio transport |
| Transport | stdio | Standard for local MCP servers; AI assistant spawns the process |
| Build | Maven (fat JAR via maven-shade-plugin) | Consistent with classtrim-core; single distributable artifact |
| Packaging | `classtrim-mcp-1.0.0-all.jar` | Self-contained; `java -jar` to run |

## Configuration (mcp.json)

For Kiro:
```json
{
  "mcpServers": {
    "classtrim": {
      "command": "java",
      "args": ["-jar", "C:/path/to/classtrim-mcp-1.0.0-all.jar"],
      "autoApprove": ["analyze_project", "get_metrics", "list_algorithms", "explain_suggestion"]
    }
  }
}
```

## Interaction Examples

### Example 1: AI assistant analyzes a project

```
User: "Analyze smelly-demo for move-method opportunities"

Assistant calls: analyze_project({
  roots: ["C:/codeRefactoring/NSGA3/smelly-demo/target/classes"],
  wmc: 4, cbo: 3, rfc: 6,
  populationSize: 100, maxIterations: 500
})

Server returns: {
  suggestions: [
    { method: "chargeCustomer(Order)", sourceClass: "...OrderProcessor", targetClass: "...Order" },
    { method: "dispatchShipmentFor(Order)", sourceClass: "...OrderProcessor", targetClass: "...Order" },
    ...
  ],
  computingTimeMs: 7135,
  classCount: 8,
  methodCount: 32
}

Assistant: "Found 5 move-method suggestions in 7.1 seconds. The top candidate is
chargeCustomer(Order) — it accesses 5 fields on Order but only 2 on OrderProcessor.
Want me to explain any of these or apply them?"
```

### Example 2: AI assistant checks metrics first

```
User: "Which classes in my project are too complex?"

Assistant calls: get_metrics({
  roots: ["C:/project/target/classes"],
  wmc: 8, cbo: 8, rfc: 30
})

Server returns: { classes: [...], classesExceedingThreshold: 3 }

Assistant: "3 classes exceed your thresholds:
- OrderProcessor: WMC=15 (threshold 8), RFC=42 (threshold 30)
- Invoice: WMC=9 (threshold 8)
- Order: RFC=35 (threshold 30)

Want me to run the refactoring optimizer to suggest method moves?"
```

### Example 3: AI assistant explains a suggestion

```
User: "Why should chargeCustomer move to Order?"

Assistant calls: explain_suggestion({
  roots: [...],
  method: "chargeCustomer(Order)",
  sourceClass: "org.classtrim.demo.ecommerce.OrderProcessor",
  targetClass: "org.classtrim.demo.ecommerce.Order"
})

Server returns: {
  couplingToSource: 2,
  couplingToTarget: 5,
  reason: "Feature envy — method accesses 5 members on Order but only 2 on OrderProcessor",
  fieldAccessesOnTarget: ["customer", "items", "id", "paid", "placedAt"]
}

Assistant: "chargeCustomer accesses 5 fields on Order (customer, items, id, paid, placedAt)
but only 2 on its own class (inventory, auditTrail). This is a classic Feature Envy
pattern — the method is more interested in Order's data than its own class's data.
Moving it to Order would reduce coupling and improve cohesion."
```

## Implementation Plan

| Phase | Tasks | Effort |
|---|---|---|
| 1. Scaffold | Create Maven module, add MCP SDK + classtrim-core deps, write entry point | 1 hour |
| 2. analyze_project | Wire ClassTrimService through the tool handler, JSON serialization | 1 hour |
| 3. get_metrics | Parse project without optimization, extract per-class metrics | 1 hour |
| 4. list_algorithms | Trivial — read AlgorithmType.values() | 15 min |
| 5. explain_suggestion | Extract coupling data from JavaMethod/JavaClass model | 2 hours |
| 6. Package & test | Fat JAR, mcp.json example, manual test with Kiro | 1 hour |

**Total: ~6 hours**

## Future Extensions

- **`apply_suggestion` tool** — generate a diff or patch file for a specific move-method suggestion
- **`compare_algorithms` tool** — run both NSGA-II and NSGA-III on the same project and compare Pareto fronts
- **`watch_project` resource** — re-analyze on file change (useful for CI integration)
- **Progress notifications** — stream iteration progress via MCP notifications during long-running analysis
- **Multi-project support** — analyze multiple modules in a single server session
