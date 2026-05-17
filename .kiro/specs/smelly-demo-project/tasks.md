# Implementation Plan: Smelly_Demo_Project

## Overview

Build a new sibling Maven module `smelly-demo` under the NSGA3 root POM that ships eight hand-authored Java 17 classes in package `org.classtrim.demo.ecommerce`, deliberately seeded with Feature Envy (headline), God Class (`OrderProcessor`), Inappropriate Intimacy (`Order`↔`Customer`, `Invoice`↔`Order`), and Shotgun Surgery (inlined `0.0825` tax literal and `0.92` USD→EUR currency literal). Implementation proceeds bottom-up: leaf domain classes first (`Customer`, `Product`, `Inventory`), then aggregates (`OrderItem`, `Order`), then services (`ShippingCalculator`, `Invoice`), then the God Class (`OrderProcessor`), then a Maven compile to materialize bytecode under `smelly-demo/target/classes/org/classtrim/demo/ecommerce/`. An optional ASM-based verification harness backs each of the 12 correctness properties from the design.

## Tasks

- [x] 1. Set up Maven module skeleton
  - [x] 1.1 Create `smelly-demo/pom.xml`
    - Declare `<packaging>jar</packaging>`, `<artifactId>smelly-demo</artifactId>`, parent reference to the NSGA3 root POM
    - Set `<maven.compiler.source>17</maven.compiler.source>` and `<maven.compiler.target>17</maven.compiler.target>` overriding the parent's `11`
    - Declare zero `<dependency>` and zero `<dependencies>` test entries
    - Configure source root `src/main/java` (Maven default) so output lands at `target/classes`
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 8.4_

  - [x] 1.2 Register `smelly-demo` in the NSGA3 root `pom.xml`
    - Add `<module>smelly-demo</module>` to the root `<modules>` list
    - Leave existing modules and parent compiler settings untouched
    - _Requirements: 1.5_

- [x] 2. Implement leaf domain classes
  - [x] 2.1 Implement `Customer.java`
    - Path: `smelly-demo/src/main/java/org/classtrim/demo/ecommerce/Customer.java`
    - Public, top-level, non-abstract `class` in package `org.classtrim.demo.ecommerce`
    - Fields with package-or-public visibility: `name`, `email`, `street`, `city`, `postalCode`, `country`, `loyaltyPoints` (`int`), `vipTier` (`String`)
    - Add a non-getter, non-setter, non-`@Override` Smelly_Method that participates in the `(Order, Customer)` Inappropriate_Intimacy_Pair by reading `Order.items` (or invoking a non-getter on `Order`) — accept `Order` as a parameter so this resolves at compile time
    - Add a `evaluateLoyaltyTier()`-style method invoked by `OrderProcessor` and `Order.computeCustomerLifetimeValue`
    - No `@Override`, no `static`, no constructor logic beyond field initialization
    - _Requirements: 2.1, 2.3, 2.4, 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 3.9, 3.10, 4.6, 6.1, 6.2, 6.3_

  - [x] 2.2 Implement `Product.java`
    - Path: `smelly-demo/src/main/java/org/classtrim/demo/ecommerce/Product.java`
    - Public, top-level, non-abstract class in `org.classtrim.demo.ecommerce`
    - Fields with package-or-public visibility: `sku`, `name`, `priceUsd` (`double`), `weightKg` (`double`), `category`, `taxable` (`boolean`), `inStock` (`boolean`)
    - Smelly_Method `priceInEur()` inlines the literal `0.92` (currency-conversion shotgun-surgery site) directly in its body — no shared helper, no `static final` constant
    - _Requirements: 2.1, 2.3, 2.4, 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 3.9, 3.10, 7.2, 7.4_

  - [x] 2.3 Implement `Inventory.java`
    - Path: `smelly-demo/src/main/java/org/classtrim/demo/ecommerce/Inventory.java`
    - Public, top-level, non-abstract class in `org.classtrim.demo.ecommerce`
    - Fields: `stockBySku` (`Map<String, Integer>` initialized to `new HashMap<>()`), `lowStockThreshold` (`int`)
    - Public non-`@Override` instance methods `reserveStock(String sku, int qty)`, `releaseStock(String sku, int qty)`, `availableStock(String sku)` — invoked by `OrderProcessor` (cross-class fan-out target)
    - _Requirements: 2.1, 2.3, 2.4, 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 3.9, 3.10_

- [x] 3. Implement aggregate domain classes
  - [x] 3.1 Implement `OrderItem.java`
    - Path: `smelly-demo/src/main/java/org/classtrim/demo/ecommerce/OrderItem.java`
    - Public, top-level, non-abstract class in `org.classtrim.demo.ecommerce`
    - Fields: `product` (`Product` ref), `quantity` (`int`), `lineDiscount` (`double`)
    - Smelly_Method `computeLineTaxedTotal()` inlines tax-rate literal `0.0825` directly in body (shotgun-surgery site)
    - Smelly_Method `convertLineTotalToEur()` inlines currency literal `0.92` directly in body (shotgun-surgery site)
    - One additional supplementary FEM that reads `product.priceUsd` and `product.weightKg` more times than it accesses self-fields — Feature_Envy_Method on `Product`
    - _Requirements: 2.1, 2.3, 2.4, 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 3.9, 3.10, 4.2, 4.6, 7.1, 7.2, 7.3, 7.4_

  - [x] 3.2 Implement `Order.java`
    - Path: `smelly-demo/src/main/java/org/classtrim/demo/ecommerce/Order.java`
    - Public, top-level, non-abstract class in `org.classtrim.demo.ecommerce`
    - Fields: `id`, `customer` (`Customer` ref), `items` (`List<OrderItem>`, default `new ArrayList<>()`), `shippingAddressOverride`, `placedAt` (`Instant`), `currency`, `paid` (`boolean`)
    - Smelly_Method `formatShippingLabel()` reads `customer.name`, `customer.street`, `customer.city`, `customer.postalCode`, `customer.country` — strictly more foreign reads on `Customer` than self → Feature_Envy_Method on `Customer`
    - Smelly_Method `computeCustomerLifetimeValue()` reads `customer.loyaltyPoints`, `customer.vipTier`, plus invokes `customer.evaluateLoyaltyTier()` — Feature_Envy_Method on `Customer`
    - Smelly_Method `computeOrderTotalWithTax()` inlines tax literal `0.0825` directly in body
    - Smelly_Method `convertOrderTotalToEur()` inlines currency literal `0.92` directly in body
    - All Smelly_Methods are `public`, non-`static`, non-constructor, non-`@Override`, not pure getter/setter
    - _Requirements: 2.1, 2.3, 2.4, 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 3.9, 3.10, 4.1, 4.2, 4.3, 4.6, 6.1, 6.2, 6.3, 7.1, 7.2, 7.3, 7.4_

- [x] 4. Implement service-style classes
  - [x] 4.1 Implement `ShippingCalculator.java`
    - Path: `smelly-demo/src/main/java/org/classtrim/demo/ecommerce/ShippingCalculator.java`
    - Public, top-level, non-abstract class in `org.classtrim.demo.ecommerce`
    - Fields: `baseRateUsd` (`double`), `perKgRateUsd` (`double`), `expeditedSurchargeUsd` (`double`)
    - Smelly_Method `calculateShippingForOrder(Order o)` iterates `o.items`, reads `o.items[i].quantity`, `o.items[i].product.weightKg`, `o.shippingAddressOverride`, `o.customer.country` — strictly more foreign accesses on `Order`/`OrderItem` than on self → Feature_Envy_Method on `Order` (or `OrderItem`)
    - _Requirements: 2.1, 2.3, 2.4, 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 3.9, 3.10, 4.1, 4.2, 4.5, 4.6_

  - [x] 4.2 Implement `Invoice.java`
    - Path: `smelly-demo/src/main/java/org/classtrim/demo/ecommerce/Invoice.java`
    - Public, top-level, non-abstract class in `org.classtrim.demo.ecommerce`
    - Fields: `invoiceNumber`, `order` (`Order` ref), `issuedAt` (`Instant`), `lineItems` (`List<OrderItem>`), `notes`
    - Smelly_Method `renderInvoiceLines()` reads `order.id`, `order.customer.name`, `order.items[i].product.name`, `order.items[i].quantity`, `order.placedAt` — strictly more foreign accesses on `Order` than on self → Feature_Envy_Method on `Order`
    - Smelly_Method `computeInvoiceGrandTotalWithTax()` inlines tax literal `0.0825` directly in body
    - Smelly_Method `convertGrandTotalToEur()` inlines currency literal `0.92` directly in body
    - Smelly_Method `auditOrderItems()` reads at least one field of `Order` or invokes a non-getter, non-setter method of `Order` — closes the `(Invoice, Order)` Inappropriate_Intimacy_Pair (the matching back-edge from `Order` already exists via the FEMs in 3.2)
    - _Requirements: 2.1, 2.3, 2.4, 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 3.9, 3.10, 4.1, 4.2, 4.4, 4.6, 6.1, 6.2, 6.3, 7.1, 7.2, 7.3, 7.4_

- [x] 5. Implement OrderProcessor God Class
  - [x] 5.1 Implement `OrderProcessor.java`
    - Path: `smelly-demo/src/main/java/org/classtrim/demo/ecommerce/OrderProcessor.java`
    - Public, top-level, non-abstract class in `org.classtrim.demo.ecommerce`
    - Fields: `inventory` (`Inventory` ref), `shippingCalculator` (`ShippingCalculator` ref), `currentInvoice` (`Invoice` ref or `null`), `defaultCurrency`, `auditTrail` (`List<String>`)
    - At least 8 `public`, non-static, non-constructor, non-`@Override`, non-pure-getter/setter instance methods, including: `processNewOrder(Order)`, `chargeCustomer(Order)`, `reserveInventoryFor(Order)`, `releaseInventoryFor(Order)`, `dispatchShipmentFor(Order)`, `generateInvoiceFor(Order)`, `applyLoyaltyDiscountFor(Order)`, `recomputeOrderTotalWithTax(Order)`, `convertOrderToCurrency(Order)`, `flagSuspiciousOrder(Order)`
    - Across the methods, read fields of or invoke methods on at least 5 distinct other Demo_Classes (target the full set: `Order`, `OrderItem`, `Customer`, `Product`, `Inventory`, `ShippingCalculator`, `Invoice`)
    - At least 3 of those methods qualify as Feature_Envy_Methods (strictly more foreign-class accesses than self-class accesses on a single foreign Demo_Class), e.g. `chargeCustomer` envies `Customer`, `dispatchShipmentFor` envies `Order` or `ShippingCalculator`, `flagSuspiciousOrder` envies `Order`
    - `recomputeOrderTotalWithTax(Order)` inlines tax literal `0.0825` directly in body
    - `convertOrderToCurrency(Order)` inlines currency literal `0.92` directly in body
    - Source size between 200 and 350 lines, inclusive
    - Total project source lines across all eight `.java` files must land in `[600, 1000]` — size method bodies of `Customer`, `Product`, `OrderItem`, `Order`, `Inventory`, `ShippingCalculator`, `Invoice` accordingly
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 3.9, 3.10, 4.1, 4.2, 4.6, 5.1, 5.2, 5.3, 5.4, 7.1, 7.2, 7.3, 7.4_

- [x] 6. Compile module and verify build output
  - [x] 6.1 Compile and verify class file outputs
    - Run `mvn -pl smelly-demo -am compile` from the NSGA3 repository root and confirm exit code `0` with no compilation errors
    - Confirm exactly eight `.class` files exist directly under `smelly-demo/target/classes/org/classtrim/demo/ecommerce/`, one per Demo_Class in the inventory (`Order`, `OrderItem`, `Customer`, `Product`, `Inventory`, `ShippingCalculator`, `Invoice`, `OrderProcessor`)
    - Confirm no extra `.class` files exist in that directory (no inner/anonymous/local classes)
    - Confirm total source LOC across the eight `.java` files is in `[600, 1000]` and `OrderProcessor.java` LOC is in `[200, 350]`
    - _Requirements: 1.6, 1.7, 2.2, 5.3, 8.1, 8.2, 8.3_

- [x] 7. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 8. Property-based bytecode verification harness
  - [x] 8.1 Set up ASM-based bytecode verification harness
    - Create a JUnit test in an appropriate location (e.g. under `classtrim-core/src/test/java/...` or a new dedicated harness module) that loads every `.class` file under `smelly-demo/target/classes/org/classtrim/demo/ecommerce/` using ASM `ClassReader`
    - Provide shared utilities for: enumerating Demo_Classes, classifying methods (public/static/constructor/`@Override`/getter/setter), counting `GETFIELD`/`PUTFIELD`/`INVOKE*` per owner, and detecting `LDC` literal pushes
    - This harness is the prerequisite for all 8.x property tests below
    - _Requirements: prerequisite for Properties 1–12_

  - [x] 8.2 Write property test for Demo_Class structural eligibility
    - **Property 1: Demo_Class structural eligibility**
    - For every compiled type in `org.classtrim.demo.ecommerce`, assert it is `public`, top-level, non-`abstract`, not an `interface`, not an `enum`
    - **Validates: Requirements 3.1, 3.2, 3.3, 3.4**

  - [x] 8.3 Write property test for Demo_Class inventory completeness
    - **Property 2: Demo_Class inventory completeness**
    - Assert exactly one `.class` file exists for each name in `{Order, OrderItem, Customer, Product, Inventory, ShippingCalculator, Invoice, OrderProcessor}` under `smelly-demo/target/classes/org/classtrim/demo/ecommerce/`, and no others
    - **Validates: Requirements 2.1, 8.2**

  - [x] 8.4 Write property test for source layout matches package and filename
    - **Property 3: Source layout matches package and filename**
    - For every `.java` source file under `smelly-demo/src/main/java`, assert it declares package `org.classtrim.demo.ecommerce`, declares exactly one top-level public class, and the filename stem equals the class's simple name
    - **Validates: Requirements 2.3, 2.4**

  - [x] 8.5 Write property test for build output rooted under target/classes
    - **Property 4: Build output rooted under target/classes**
    - Assert every `.class` file emitted by compiling the module has a path rooted under `smelly-demo/target/classes/`
    - **Validates: Requirements 1.7, 8.1**

  - [x] 8.6 Write property test for Smelly_Method structural eligibility
    - **Property 5: Smelly_Method structural eligibility**
    - For every method registered as a Smelly_Method in the design's smelly-method registry, assert it is `public`, non-`static`, not a constructor (`<init>`), lacks `@Override`, and its body is not a pure field-return or pure parameter-to-field assignment
    - **Validates: Requirements 3.5, 3.6, 3.7, 3.8, 3.9**

  - [x] 8.7 Write property test for no reflection or toString indirection
    - **Property 6: Smells use no reflection or toString indirection**
    - For every method in `org.classtrim.demo.ecommerce`, assert no `INVOKE*` instruction has an owner in `java.lang.reflect.*` or `java.lang.invoke.MethodHandle*`, and no `INVOKEVIRTUAL` of `toString()` whose receiver type is another Demo_Class exists
    - **Validates: Requirements 3.10**

  - [x] 8.8 Write property test for Feature Envy definitional invariant
    - **Property 7: Feature Envy definitional invariant**
    - For every method designated as a Feature_Envy_Method, assert its bytecode performs strictly more field reads plus method invocations on one specific foreign Demo_Class than on the declaring class
    - **Validates: Requirements 4.2, 4.6**

  - [x] 8.9 Write property test for Feature Envy distribution and count
    - **Property 8: Feature Envy distribution and count**
    - Assert the set of Feature_Envy_Methods has cardinality at least 4, is distributed across at least 3 distinct declaring Demo_Classes, and at least 3 are declared on `OrderProcessor`
    - **Validates: Requirements 4.1, 5.4**

  - [x] 8.10 Write property test for God Class fan-out and method count
    - **Property 9: God Class fan-out and method count on OrderProcessor**
    - Assert `OrderProcessor` has at least 8 public, non-getter, non-setter, non-`@Override` instance methods, and reads fields of or invokes methods on at least 5 distinct other Demo_Classes
    - **Validates: Requirements 5.1, 5.2**

  - [x] 8.11 Write property test for Inappropriate Intimacy bidirectionality
    - **Property 10: Inappropriate Intimacy bidirectionality**
    - For each Inappropriate_Intimacy_Pair `(A, B)` declared in the design (drawn from `{(Order, Customer), (Invoice, Order)}`), assert at least one Smelly_Method on `A` reads a field of `B` or invokes a non-getter/setter method of `B`, and at least one Smelly_Method on `B` reads a field of `A` or invokes a non-getter/setter method of `A`
    - **Validates: Requirements 6.1, 6.2, 6.3**

  - [x] 8.12 Write property test for Shotgun Surgery duplication count
    - **Property 11: Shotgun Surgery duplication count**
    - For each cross-cutting concern in `{tax computation, currency conversion}`, assert the set of Demo_Classes from its candidate set whose source contains at least one method body computing the concern inline has cardinality at least 3
    - **Validates: Requirements 7.1, 7.2**

  - [x] 8.13 Write property test for Shotgun Surgery inlined-literal invariant
    - **Property 12: Shotgun Surgery inlined-literal invariant**
    - For every method registered in the tax-computation registry, assert its bytecode contains an `LDC` push of `0.0825` and does not delegate to a single shared helper class; symmetrically for currency-conversion methods, assert an `LDC` push of `0.92` and no shared helper invocation
    - **Validates: Requirements 7.3, 7.4**

- [x] 9. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for a faster MVP. The smelly-demo module itself ships no `src/test`; the property tests in section 8 are an external verification harness that backs the design's correctness contract against the produced bytecode.
- Each implementation task references specific requirements (granular sub-requirements, not just user stories) for traceability.
- The headline smell (Feature Envy) is realized through real `GETFIELD` and `INVOKE*` bytecode targeting foreign Demo_Classes — never through reflection, `MethodHandle`, or `toString` parsing.
- Shotgun Surgery is realized by inlining the *exact same* numeric literals (`0.0825` for tax, `0.92` for currency) directly in each duplicating method body — no `static final` constants, no shared helper classes.
- Total source LOC must land in `[600, 1000]` overall and `OrderProcessor.java` in `[200, 350]`; size method bodies during implementation to hit those bands.
- Checkpoints (tasks 7 and 9) ensure incremental validation between major implementation milestones.
- Property tests in section 8 validate the universal correctness properties from the design's "Correctness Properties" section.

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2"] },
    { "id": 1, "tasks": ["2.1", "2.2", "2.3"] },
    { "id": 2, "tasks": ["3.1", "3.2"] },
    { "id": 3, "tasks": ["4.1", "4.2"] },
    { "id": 4, "tasks": ["5.1"] },
    { "id": 5, "tasks": ["6.1"] },
    { "id": 6, "tasks": ["8.1"] },
    { "id": 7, "tasks": ["8.2", "8.3", "8.4", "8.5", "8.6", "8.7", "8.8", "8.9", "8.10", "8.11", "8.12", "8.13"] }
  ]
}
```
