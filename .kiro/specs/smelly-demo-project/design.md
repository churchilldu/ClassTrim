# Design Document

## Overview

The Smelly_Demo_Project is a single, dependency-free Maven module that provides a fixture e-commerce codebase deliberately seeded with code smells. It is consumed by the existing classtrim NSGA-III analyzer as compiled bytecode under `smelly-demo/target/classes/org/classtrim/demo/ecommerce/`.

The design is a static, hand-authored set of eight Java source files plus one `pom.xml`. There is no runtime logic to execute, no service to wire, and no library API to expose. The sole correctness contract is a structural invariant: the produced bytecode must satisfy every eligibility rule the classtrim analyzer applies, and the smells must be visible at the bytecode level (real `GETFIELD`, `PUTFIELD`, and `INVOKE*` instructions targeting foreign demo classes).

The detection logic that picks up these smells lives in `classtrim-core` (and its consumers); this module does not reimplement that detection. It only ships the input.

The headline smell is **Feature Envy**, with secondary **God Class** (`OrderProcessor`), **Inappropriate Intimacy** (e.g. `Order` ↔ `Customer`), and **Shotgun Surgery** (tax + currency literals duplicated inline across multiple classes).

## Architecture

### Module placement

```
NSGA3/
├── pom.xml                          (parent — add <module>smelly-demo</module>)
├── classtrim-core/
├── classtrim-cli/
├── classtrim-plugin/                (Gradle, unaffected)
└── smelly-demo/                     (NEW)
    ├── pom.xml
    └── src/
        └── main/
            └── java/
                └── org/classtrim/demo/ecommerce/
                    ├── Customer.java
                    ├── Product.java
                    ├── OrderItem.java
                    ├── Order.java
                    ├── Inventory.java
                    ├── ShippingCalculator.java
                    ├── Invoice.java
                    └── OrderProcessor.java
```

Note: the parent POM declares `maven.compiler.source/target=11`; this module overrides those to `17` in its own `pom.xml` (per Requirement 1.3).

### Build pipeline

`mvn -pl smelly-demo -am compile` from the NSGA3 root resolves the parent POM, compiles each `.java` to a `.class` under `smelly-demo/target/classes/org/classtrim/demo/ecommerce/`, and exits zero. No tests, no shading, no resources. The classtrim analyzer's `CompilerOutputResolver` then discovers the directory by its conventional Maven location.

### Eligibility contract

Every class in the package must satisfy:

| Rule | Source-level expression |
| --- | --- |
| public | `public class Foo` |
| top-level | one top-level type per file, no nested/inner/anonymous types |
| non-abstract | no `abstract` modifier on any class |
| non-enum / non-interface | only `class` keyword used |

Every method designated as a Smelly_Method must satisfy:

| Rule | Source-level expression |
| --- | --- |
| public | `public` modifier |
| non-static | no `static` modifier |
| non-constructor | not a `<init>` method |
| no `@Override` | annotation absent |
| not pure getter/setter | body contains more than a single `return this.f;` or `this.f = p;` |

Smells must be expressed exclusively through **direct field reads** and **direct method invocations** on foreign demo classes. The design forbids `java.lang.reflect`, `java.lang.invoke.MethodHandle`, and `Object.toString` parsing as smell carriers.

### Class graph (smell-bearing edges only)

```
                 Customer ◄──────┐
                    ▲            │  (Order envies Customer; Customer envies Order)
                    │            │
                    │            │
   OrderItem ◄───── Order ──────►Invoice
       ▲              ▲            │
       │              │            │
       └──── ShippingCalculator    │
                                   ▼
                                 Order   (Invoice envies Order)

   Product ◄── OrderItem (currency-conversion duplication site)
   Inventory ◄── OrderProcessor (god-class fan-out)

   OrderProcessor ──► {Order, OrderItem, Customer, Inventory,
                      ShippingCalculator, Invoice, Product}
                      (god-class: ≥5 distinct foreign targets)
```

## Components and Interfaces

This module exposes no public API to other modules; the "interface" of each class is its set of public fields and methods that the smelly methods read or invoke. Field visibility is `public` for fields read across class boundaries (or package-private when the consumer is in the same package, which all of these are). All accesses resolve at compile time without reflection.

### Customer
- Fields: `name`, `email`, `street`, `city`, `postalCode`, `country`, `loyaltyPoints`, `vipTier`.
- Public, non-getter/setter, non-`@Override` methods exist for: full-address rendering (envied by `Order`), loyalty-tier evaluation (read by `OrderProcessor`), and a smelly method that reaches into `Order.items` (intimacy back-edge).

### Product
- Fields: `sku`, `name`, `priceUsd`, `weightKg`, `category`, `taxable`, `inStock`.
- Inlines a currency conversion literal inside a `priceInEur()`-style method (shotgun surgery site).

### OrderItem
- Fields: `product` (ref), `quantity`, `lineDiscount`.
- Public methods compute line totals and inline both a tax rate literal and a currency conversion literal in their bodies (shotgun surgery sites).
- One method on `OrderItem` reads `product.priceUsd` and `product.weightKg` more times than it touches its own fields → Feature Envy on `Product` (optional supplementary FEM).

### Order
- Fields: `id`, `customer` (ref), `items` (`List<OrderItem>`), `shippingAddressOverride`, `placedAt`, `currency`, `paid`.
- Smelly methods:
  - `formatShippingLabel()` — reads `customer.name`, `customer.street`, `customer.city`, `customer.postalCode`, `customer.country` and concatenates them. Foreign reads on `Customer` ≫ self reads → **FEM (Order envies Customer)** [Req 4.3].
  - `computeCustomerLifetimeValue()` — reads `customer.loyaltyPoints`, `customer.vipTier`, plus invokes a non-getter on `Customer`. Foreign accesses on `Customer` ≫ self → FEM (Order envies Customer).
  - `computeOrderTotalWithTax()` — inlines the tax rate literal (shotgun surgery).
  - `convertOrderTotalToEur()` — inlines the currency conversion literal (shotgun surgery).

### Inventory
- Fields: `stockBySku` (`Map<String,Integer>`), `lowStockThreshold`.
- Public methods reserve/release stock by SKU. The God Class invokes these.

### ShippingCalculator
- Fields: `baseRateUsd`, `perKgRateUsd`, `expeditedSurchargeUsd`.
- Smelly method `calculateShippingForOrder(Order o)` — iterates `o.items`, reads `o.items[i].quantity`, `o.items[i].product.weightKg`, `o.shippingAddressOverride`, `o.customer.country`. Foreign accesses ≫ self → **FEM (ShippingCalculator envies Order or OrderItem)** [Req 4.5].

### Invoice
- Fields: `invoiceNumber`, `order` (ref), `issuedAt`, `lineItems` (mirrors `order.items`), `notes`.
- Smelly methods:
  - `renderInvoiceLines()` — reads `order.id`, `order.customer.name`, `order.items[i].product.name`, `order.items[i].quantity`, `order.placedAt`. Foreign accesses on `Order` ≫ self → **FEM (Invoice envies Order)** [Req 4.4].
  - `computeInvoiceGrandTotalWithTax()` — inlines the tax rate literal (shotgun surgery).
  - `convertGrandTotalToEur()` — inlines the currency conversion literal (shotgun surgery).
  - `auditOrderItems()` — mutates `order.<package-or-public field>` or invokes a non-getter on `Order` (intimacy back-edge for `(Invoice, Order)` pair).

### OrderProcessor (God Class)
- Fields: `inventory` (ref), `shippingCalculator` (ref), `currentInvoice` (ref or null), `defaultCurrency`, `auditTrail` (`List<String>`).
- ≥8 public, non-getter/setter, non-`@Override` instance methods, each reaching into multiple foreign demo classes:
  - `processNewOrder(Order)`
  - `chargeCustomer(Order)`
  - `reserveInventoryFor(Order)`
  - `releaseInventoryFor(Order)`
  - `dispatchShipmentFor(Order)`
  - `generateInvoiceFor(Order)`
  - `applyLoyaltyDiscountFor(Order)`
  - `recomputeOrderTotalWithTax(Order)` — inlines tax literal
  - `convertOrderToCurrency(Order)` — inlines currency literal
  - `flagSuspiciousOrder(Order)` — reaches into `order.customer` and `order.items[*].product`
- Touches ≥ 5 distinct foreign Demo_Classes: `Order`, `OrderItem`, `Customer`, `Product`, `Inventory`, `ShippingCalculator`, `Invoice` (seven, comfortably above the ≥ 5 floor).
- Source size sized to fall in the 200–350 LOC band (Req 5.3).
- ≥ 3 of its methods qualify as FEMs (e.g., `chargeCustomer`, `dispatchShipmentFor`, `generateInvoiceFor`, `flagSuspiciousOrder`) by reading more foreign-class fields and invoking more foreign-class methods than self.

## Data Models

There is no runtime data model in the engineering sense — the classes are fixture material. Field types are restricted to:

- Java primitives (`int`, `long`, `double`, `boolean`)
- `java.lang.String`, `java.time.Instant`, `java.math.BigDecimal`
- `java.util.List`, `java.util.Map`, `java.util.ArrayList`, `java.util.HashMap`
- References to other Demo_Classes in the same package

These are part of the Java 17 standard library, satisfying the zero-runtime-dependency rule (Req 8.4).

### Constants used in shotgun-surgery sites

| Concern | Literal | Inlined in (≥ 3 of) |
| --- | --- | --- |
| Tax rate | `0.0825` | `Order.computeOrderTotalWithTax`, `OrderItem.computeLineTaxedTotal`, `Invoice.computeInvoiceGrandTotalWithTax`, `OrderProcessor.recomputeOrderTotalWithTax` |
| Currency conversion (USD→EUR) | `0.92` | `Order.convertOrderTotalToEur`, `OrderItem.convertLineTotalToEur`, `Product.priceInEur`, `Invoice.convertGrandTotalToEur`, `OrderProcessor.convertOrderToCurrency` |

The literal value is the same constant in every duplicating method, written as a `double` literal in the method body (no `static final` extracted, no shared helper class). This is the explicit shotgun-surgery shape required by Req 7.3 / 7.4.

## Error Handling

This is a fixture module. There is no production-grade error handling design.

- **Compilation errors** propagate naturally as Maven build failures. Maven's `maven-compiler-plugin` returns a non-zero exit code on `javac` failure, which surfaces through the parent build (Req 8.3).
- **Runtime exceptions** are not engineered. The methods are never invoked at runtime in this module's lifecycle; classtrim analyzes them statically.
- **Missing dependencies** cannot occur — the `pom.xml` declares no dependencies.
- **Eligibility regressions** (e.g., someone marks a class `abstract` or annotates a smelly method with `@Override`) are caught by the verification properties in the next section, not by runtime guards.

## Testing Strategy

Although this module ships no `src/test`, the **correctness properties** below are what the broader classtrim project (or a one-off verification harness) checks against the produced bytecode. The properties are universal over the artifact and are the contract this design must uphold.

Two categories of automated checks back the design:

1. **Property-based bytecode verification** — uses ASM (already a transitive dependency of `classtrim-core`) to walk every compiled `.class` under `smelly-demo/target/classes/` and assert each universal property below.
2. **Targeted examples and smoke checks** — one-shot assertions for existence claims (e.g., "there exists a FEM on `Order` envying `Customer`") and configuration values (POM packaging, compiler source/target, module list, LOC bands).

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system — essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Demo_Class structural eligibility

For all compiled types in package `org.classtrim.demo.ecommerce`, the type is `public`, top-level (no enclosing class, not anonymous, not local), non-`abstract`, not an `interface`, and not an `enum`.

**Validates: Requirements 3.1, 3.2, 3.3, 3.4**

### Property 2: Demo_Class inventory completeness

For all class names in the fixed inventory `{Order, OrderItem, Customer, Product, Inventory, ShippingCalculator, Invoice, OrderProcessor}`, exactly one compiled `.class` file with that simple name exists under `smelly-demo/target/classes/org/classtrim/demo/ecommerce/`, and no other `.class` files exist in that directory.

**Validates: Requirements 2.1, 8.2**

### Property 3: Source layout matches package and filename

For all `.java` source files under `smelly-demo/src/main/java`, the file declares package `org.classtrim.demo.ecommerce`, declares exactly one top-level public class, and the filename stem equals that class's simple name.

**Validates: Requirements 2.3, 2.4**

### Property 4: Build output rooted under target/classes

For all `.class` files emitted by compiling the Smelly_Demo_Project, the file path is rooted under `smelly-demo/target/classes/`.

**Validates: Requirements 1.7, 8.1**

### Property 5: Smelly_Method structural eligibility

For all methods designated as Smelly_Methods in the design's smelly-method registry, the method is `public`, non-`static`, not a constructor, lacks the `@Override` annotation, and its body is not a pure field-return or pure parameter-to-field assignment.

**Validates: Requirements 3.5, 3.6, 3.7, 3.8, 3.9**

### Property 6: Smells use no reflection or toString indirection

For all methods in package `org.classtrim.demo.ecommerce`, the method's bytecode contains no `INVOKE*` instruction whose owner is in `java.lang.reflect.*` or `java.lang.invoke.MethodHandle*`, and no `INVOKEVIRTUAL` of `toString()` whose receiver type is another Demo_Class.

**Validates: Requirements 3.10**

### Property 7: Feature Envy definitional invariant

For all methods designated as Feature_Envy_Methods, the method's bytecode performs strictly more field reads plus method invocations targeting one specific foreign Demo_Class than it performs targeting the class declaring the method.

**Validates: Requirements 4.2, 4.6**

### Property 8: Feature Envy distribution and count

For the produced module, the set of Feature_Envy_Methods has cardinality at least 4 and is distributed across at least 3 distinct declaring Demo_Classes; furthermore, at least 3 of those Feature_Envy_Methods are declared on `OrderProcessor`.

**Validates: Requirements 4.1, 5.4**

### Property 9: God Class fan-out and method count on OrderProcessor

For the produced `OrderProcessor` class, the count of public, non-getter, non-setter, non-`@Override` instance methods is at least 8, and the set of distinct foreign Demo_Classes whose fields it reads or whose methods it invokes (across all its methods' bytecode) has cardinality at least 5.

**Validates: Requirements 5.1, 5.2**

### Property 10: Inappropriate Intimacy bidirectionality

For all Inappropriate_Intimacy_Pairs `(A, B)` declared in the design (drawn from `{(Order, Customer), (Invoice, Order)}`), there exists at least one Smelly_Method on `A` that reads a field of `B` or invokes a non-getter, non-setter method of `B`, and there exists at least one Smelly_Method on `B` that reads a field of `A` or invokes a non-getter, non-setter method of `A`.

**Validates: Requirements 6.1, 6.2, 6.3**

### Property 11: Shotgun Surgery duplication count

For each cross-cutting concern `c` in `{tax computation, currency conversion}`, the set of Demo_Classes from `c`'s candidate set whose source contains at least one method body computing `c` inline has cardinality at least 3.

**Validates: Requirements 7.1, 7.2**

### Property 12: Shotgun Surgery inlined-literal invariant

For all methods designated in the design's tax-computation registry, the method's bytecode contains an `LDC` (or equivalent constant push) of the tax-rate literal, and the method does not delegate to a single shared helper class for tax computation; symmetrically, for all methods in the currency-conversion registry, the bytecode contains the conversion-factor literal inline and no shared helper is invoked.

**Validates: Requirements 7.3, 7.4**

## Build and Configuration Checks (non-property smoke checks)

These are one-shot verifications that do not vary with input and therefore are not expressed as universal properties:

- `smelly-demo/pom.xml` exists, declares `<packaging>jar</packaging>`, sets `maven.compiler.source` and `maven.compiler.target` to `17`, and contains zero `<dependency>` entries (Reqs 1.1, 1.2, 1.3, 1.4, 8.4).
- The NSGA3 root `pom.xml` lists `<module>smelly-demo</module>` (Req 1.5).
- `mvn -pl smelly-demo -am compile` exits with code 0 (Req 1.6).
- Total source LOC across the eight `.java` files falls in `[600, 1000]`; `OrderProcessor.java` LOC falls in `[200, 350]` (Reqs 2.2, 5.3).
- Maven surfaces a non-zero exit on injected compile errors (Req 8.3 — relies on Maven's documented behavior).
