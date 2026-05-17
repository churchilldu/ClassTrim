# smelly-demo

A small, dependency-free Maven module that ships a deliberately code-smelly e-commerce / order-processing codebase. It exists as fixture input for the classtrim NSGA-III refactoring engine, not as production code.

The eight hand-authored Java 17 classes live in package `org.classtrim.demo.ecommerce` and compile to `smelly-demo/target/classes/org/classtrim/demo/ecommerce/`, which is exactly where classtrim's `CompilerOutputResolver` looks by default.

## Smell inventory

The module is seeded with four targeted smells, all expressed at the bytecode level through real `GETFIELD` / `PUTFIELD` / `INVOKE*` instructions — no reflection, no `MethodHandle`, no `toString` parsing.

| Smell | Where |
|---|---|
| Feature Envy (headline) | 16 methods across `Order`, `Invoice`, `ShippingCalculator`, `OrderItem`, `OrderProcessor` (7 of them on `OrderProcessor`) |
| God Class | `OrderProcessor` — 10 public smelly-eligible methods, fan-out to 7 other Demo_Classes |
| Inappropriate Intimacy | `Order` ↔ `Customer` and `Invoice` ↔ `Order` (bidirectional in both pairs) |
| Shotgun Surgery | tax rate `0.0825` inlined in 4 classes; USD→EUR rate `0.92` inlined in 5 classes |

The 12 universal correctness properties that govern this fixture are enforced by an external ASM-based verification harness under `classtrim-core/src/test/java/org/classtrim/smellydemo/verify/`.

## Class inventory

```
org.classtrim.demo.ecommerce
├── Customer              leaf, intimacy back-edge to Order
├── Product               leaf, currency-conversion site
├── Inventory             leaf, fan-out target for OrderProcessor
├── OrderItem             aggregate, FEM on Product, tax + currency sites
├── Order                 aggregate, FEMs on Customer, tax + currency sites
├── ShippingCalculator    service, FEM on Order
├── Invoice               service, FEM on Order, tax + currency sites
└── OrderProcessor        God Class, FEMs on Customer/Order/ShippingCalculator
```

Total source: 952 LOC across the eight files; `OrderProcessor.java` accounts for 285.

## Build

From the NSGA3 repo root:

```
mvn -pl smelly-demo -am compile
```

This emits exactly eight `.class` files (one per Demo_Class) under `smelly-demo/target/classes/org/classtrim/demo/ecommerce/`. The module declares zero runtime and zero test dependencies — only the Java 17 standard library.

## Recommended thresholds

The classtrim NSGA-III engine reports a class as "over threshold" when its metric is **strictly greater than** the configured value (see `MetricUtils.countClass*OverThreshold`). The recommended thresholds for this module are:

| Metric | Threshold |
|---|---|
| WMC | **4** |
| CBO | **3** |
| RFC | **6** |

These values are tuned so the seeded smells surface cleanly while the small leaf classes stay under threshold. Approximate per-class metrics and how each class lands at `(4, 3, 6)`:

| Class | WMC | CBO | RFC | over WMC | over CBO | over RFC |
|---|---:|---:|---:|:---:|:---:|:---:|
| Product | 1 | 0 | 2 |   |   |   |
| Inventory | 3 | 0 | 4 |   |   |   |
| OrderItem | 3 | 1 | 5 |   |   |   |
| Customer | 4 | 2 | 6 |   |   |   |
| ShippingCalculator | 1 | 4 | 6 |   | ✓ |   |
| Invoice | 4 | 4 | 8 |   | ✓ | ✓ |
| Order | 6 | 4 | 10 | ✓ | ✓ | ✓ |
| OrderProcessor | 10 | 7 | 20+ | ✓ | ✓ | ✓ |

Net effect:
- `OrderProcessor` trips every objective — picked up as the God Class.
- `Order` and `Invoice` trip multiple objectives, exposing their Feature Envy and Inappropriate Intimacy edges.
- `ShippingCalculator` trips CBO/RFC despite a tiny WMC, which is the canonical Feature Envy signature.
- Leaf classes (`Product`, `Inventory`, `OrderItem`, `Customer`) stay under threshold so the fixture does not flood the optimizer with trivial candidates.

The numbers above are estimates from reading source. For exact engine-reported metrics, run `MetricUtils.getMetricsOfClass(...)` against the loaded `JavaProject`.

### Wiring the thresholds

Two ways to feed these thresholds into the engine:

1. **`DatasetEnum` entry** (matches the existing benchmark datasets):

   ```java
   SMELLY_DEMO ("smelly-demo", "smelly-demo/target/classes/", new Threshold(4, 3, 6));
   ```

2. **Programmatic** via `BinaryPathProjectSource` and `RefactoringConfig`:

   ```java
   Threshold threshold = new Threshold(4, 3, 6);
   var source = new BinaryPathProjectSource(
           "smelly-demo",
           List.of("smelly-demo/target/classes"),
           threshold);
   var config = new RefactoringConfig(threshold, populationSize, maxIterations);
   ```

## Verification

The bytecode-level invariants are validated by a JUnit harness in `classtrim-core`:

```
mvn -pl classtrim-core test -Dtest='SmellyDemoHarnessSmokeTest,Property*Test'
```

All 25 tests pass on a clean build. The harness covers Properties 1–12 from the design document at `.kiro/specs/smelly-demo-project/design.md`.
