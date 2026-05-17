# Requirements Document

## Introduction

The Smelly_Demo_Project is a small, self-contained Maven module that provides a deliberately code-smelly e-commerce / order-processing codebase used as fixture input for the classtrim NSGA-III refactoring engine. The module is added as a sibling Maven module under the existing NSGA3 root POM at `c:\codeRefactoring\NSGA3\smelly-demo\`. The codebase intentionally exhibits Feature Envy / move-method opportunities (the headline smell, since the NSGA-III engine targets bytecode coupling/cohesion analysis), plus secondary smells: God Class, Inappropriate Intimacy, and Shotgun Surgery. All compiled `.class` files MUST be eligible for classtrim's analyzer (public, top-level, non-abstract, non-enum, non-interface classes; smelly methods that are public, non-constructor, non-static, not pure getter/setter, not `@Override`), and the smells MUST manifest at the bytecode level through real field reads and method invocations on foreign classes (no reflection, no `toString` workarounds). The compiled output MUST land in `target/classes` so classtrim's `CompilerOutputResolver` fallback discovers it.

## Glossary

- **Smelly_Demo_Project**: The Maven module produced by this feature, located at `c:\codeRefactoring\NSGA3\smelly-demo\`, packaging `jar`, JDK 17.
- **NSGA3_Root_POM**: The existing parent Maven POM at `c:\codeRefactoring\NSGA3\pom.xml` that aggregates sibling modules.
- **Demo_Class**: A public, top-level, non-abstract, non-enum, non-interface Java class defined in package `org.classtrim.demo.ecommerce`.
- **Smelly_Method**: A public, non-static, non-constructor, non-`@Override` method on a Demo_Class that is not a pure getter or pure setter and that exhibits one or more targeted code smells through real bytecode-level field reads or method invocations on foreign Demo_Classes.
- **Feature_Envy_Method**: A Smelly_Method whose bytecode contains more field reads or method invocations on a single foreign Demo_Class than on the class declaring the method.
- **God_Class**: A Demo_Class (specifically `OrderProcessor`) that aggregates responsibilities spanning multiple other Demo_Classes (orders, customers, inventory, shipping, invoicing, tax, currency).
- **Inappropriate_Intimacy_Pair**: A pair of Demo_Classes (e.g., `Order`↔`Customer` or `Invoice`↔`Order`) whose methods read each other's fields or invoke each other's non-getter methods reciprocally.
- **Shotgun_Surgery_Concern**: A cross-cutting concern (tax computation, currency conversion) duplicated across multiple Demo_Classes such that a single conceptual change requires edits in multiple classes.
- **Compiler_Output_Directory**: The directory `target/classes` under the Smelly_Demo_Project module, which classtrim's `CompilerOutputResolver` uses as a fallback source of compiled bytecode.
- **Target_Package**: The Java package `org.classtrim.demo.ecommerce`.
- **Eligibility_Constraints**: The set of structural rules that make a Demo_Class and its Smelly_Methods discoverable by the classtrim analyzer, as enumerated in Requirement 3.

## Requirements

### Requirement 1: Maven Module Integration

**User Story:** As a classtrim developer, I want the smelly demo to be a sibling Maven module under the NSGA3 root POM, so that the demo builds with the rest of the project using a single Maven invocation.

#### Acceptance Criteria

1. THE Smelly_Demo_Project SHALL reside at the absolute path `c:\codeRefactoring\NSGA3\smelly-demo\`.
2. THE Smelly_Demo_Project SHALL provide a `pom.xml` with `packaging` set to `jar`.
3. THE Smelly_Demo_Project SHALL declare `maven.compiler.source` and `maven.compiler.target` as `17`.
4. THE Smelly_Demo_Project SHALL declare zero test dependencies in its `pom.xml`.
5. THE NSGA3_Root_POM SHALL list `smelly-demo` in its `<modules>` section.
6. WHEN `mvn -pl smelly-demo -am compile` is executed from the NSGA3 root, THE Smelly_Demo_Project SHALL compile successfully with no compilation errors.
7. WHEN compilation completes, THE Smelly_Demo_Project SHALL emit `.class` files into the Compiler_Output_Directory `smelly-demo/target/classes/org/classtrim/demo/ecommerce/`.

### Requirement 2: Domain Class Inventory

**User Story:** As a classtrim developer, I want a fixed inventory of e-commerce domain classes, so that the demo's structure is predictable and reproducible across analyzer runs.

#### Acceptance Criteria

1. THE Smelly_Demo_Project SHALL contain exactly the following Demo_Classes in the Target_Package: `Order`, `OrderItem`, `Customer`, `Product`, `Inventory`, `ShippingCalculator`, `Invoice`, `OrderProcessor`.
2. THE Smelly_Demo_Project SHALL contain a total source size between 600 and 1000 lines of code, inclusive, across all Demo_Classes combined.
3. THE Smelly_Demo_Project SHALL declare every Demo_Class in package `org.classtrim.demo.ecommerce`.
4. THE Smelly_Demo_Project SHALL define each Demo_Class in its own `.java` source file whose filename matches the class name.

### Requirement 3: Classtrim Analyzer Eligibility

**User Story:** As a classtrim developer, I want every demo class and smelly method to satisfy the analyzer's structural eligibility rules, so that the NSGA-III engine actually picks up the methods and produces refactoring candidates.

#### Acceptance Criteria

1. THE Smelly_Demo_Project SHALL declare every Demo_Class as `public`.
2. THE Smelly_Demo_Project SHALL declare every Demo_Class as a top-level type (no nested, inner, anonymous, or local class definitions).
3. THE Smelly_Demo_Project SHALL declare no Demo_Class as `abstract`.
4. THE Smelly_Demo_Project SHALL declare no type in the Target_Package as an `enum` or `interface`.
5. THE Smelly_Demo_Project SHALL declare every Smelly_Method as `public`.
6. THE Smelly_Demo_Project SHALL declare every Smelly_Method as a non-static instance method.
7. THE Smelly_Demo_Project SHALL declare no Smelly_Method as a constructor.
8. THE Smelly_Demo_Project SHALL declare no Smelly_Method with the `@Override` annotation.
9. IF a method body consists solely of returning a field value or solely of assigning a parameter to a field, THEN THE Smelly_Demo_Project SHALL NOT classify that method as a Smelly_Method.
10. THE Smelly_Demo_Project SHALL implement every smell exclusively through direct field reads or direct method invocations on foreign Demo_Classes at the bytecode level, with no use of `java.lang.reflect`, `MethodHandle`, `toString` parsing, or other indirection.

### Requirement 4: Feature Envy as the Headline Smell

**User Story:** As a classtrim developer, I want the demo to exhibit unmistakable Feature Envy / move-method opportunities, so that the NSGA-III bytecode coupling/cohesion objective produces meaningful candidates on this fixture.

#### Acceptance Criteria

1. THE Smelly_Demo_Project SHALL contain at least four Feature_Envy_Methods distributed across at least three distinct Demo_Classes.
2. WHERE a method is designated as a Feature_Envy_Method, THE Smelly_Demo_Project SHALL ensure the method's compiled bytecode performs strictly more field reads plus method invocations targeting one specific foreign Demo_Class than it performs targeting the class declaring the method.
3. THE Smelly_Demo_Project SHALL include at least one Feature_Envy_Method on `Order` whose envied target is `Customer`.
4. THE Smelly_Demo_Project SHALL include at least one Feature_Envy_Method on `Invoice` whose envied target is `Order`.
5. THE Smelly_Demo_Project SHALL include at least one Feature_Envy_Method on `ShippingCalculator` whose envied target is either `Order` or `OrderItem`.
6. THE Smelly_Demo_Project SHALL expose the fields and non-private methods on each envied Demo_Class with package-or-greater visibility sufficient for the envying method's accesses to compile and resolve at the bytecode level.

### Requirement 5: God Class Smell on OrderProcessor

**User Story:** As a classtrim developer, I want `OrderProcessor` to act as a God Class, so that the demo also stresses class-size and responsibility-distribution dimensions of the analyzer.

#### Acceptance Criteria

1. THE Smelly_Demo_Project SHALL declare `OrderProcessor` with at least eight public non-getter, non-setter, non-`@Override` instance methods.
2. THE Smelly_Demo_Project SHALL ensure `OrderProcessor` reads fields or invokes methods on at least five distinct other Demo_Classes from the inventory in Requirement 2.
3. THE Smelly_Demo_Project SHALL ensure `OrderProcessor` accounts for between 200 and 350 lines of source code, inclusive.
4. THE Smelly_Demo_Project SHALL ensure that at least three of the methods declared on `OrderProcessor` qualify as Feature_Envy_Methods under Requirement 4.

### Requirement 6: Inappropriate Intimacy Smell

**User Story:** As a classtrim developer, I want at least one Inappropriate_Intimacy_Pair, so that the analyzer encounters reciprocal cross-class coupling beyond unidirectional Feature Envy.

#### Acceptance Criteria

1. THE Smelly_Demo_Project SHALL include at least one Inappropriate_Intimacy_Pair drawn from the set {(`Order`, `Customer`), (`Invoice`, `Order`)}.
2. WHERE an Inappropriate_Intimacy_Pair `(A, B)` exists, THE Smelly_Demo_Project SHALL include at least one Smelly_Method on `A` that reads at least one field of `B` or invokes at least one non-getter, non-setter method of `B`.
3. WHERE an Inappropriate_Intimacy_Pair `(A, B)` exists, THE Smelly_Demo_Project SHALL include at least one Smelly_Method on `B` that reads at least one field of `A` or invokes at least one non-getter, non-setter method of `A`.

### Requirement 7: Shotgun Surgery Smell

**User Story:** As a classtrim developer, I want tax and currency logic scattered across multiple Demo_Classes, so that the demo exhibits a Shotgun_Surgery_Concern detectable as duplicated cross-cutting code.

#### Acceptance Criteria

1. THE Smelly_Demo_Project SHALL duplicate tax computation logic across at least three distinct Demo_Classes from the set {`Order`, `OrderItem`, `Invoice`, `OrderProcessor`}.
2. THE Smelly_Demo_Project SHALL duplicate currency-conversion logic across at least three distinct Demo_Classes from the set {`Order`, `OrderItem`, `Invoice`, `Product`, `OrderProcessor`}.
3. WHERE tax computation logic is duplicated, THE Smelly_Demo_Project SHALL inline the same numeric tax-rate constant (or an equivalent literal) directly in each duplicating method's body rather than delegating to a shared helper class.
4. WHERE currency-conversion logic is duplicated, THE Smelly_Demo_Project SHALL inline the same conversion factor (or an equivalent literal) directly in each duplicating method's body rather than delegating to a shared helper class.

### Requirement 8: Build Output and Discoverability

**User Story:** As a classtrim developer, I want the compiled classes to land where classtrim's CompilerOutputResolver fallback looks, so that the analyzer picks them up without additional configuration.

#### Acceptance Criteria

1. WHEN the Smelly_Demo_Project is compiled with Maven, THE Smelly_Demo_Project SHALL place every compiled `.class` file under `smelly-demo/target/classes/`.
2. WHEN the Smelly_Demo_Project is compiled with Maven, THE Smelly_Demo_Project SHALL produce one `.class` file per Demo_Class listed in Requirement 2 under `smelly-demo/target/classes/org/classtrim/demo/ecommerce/`.
3. IF a Demo_Class fails to compile, THEN THE Smelly_Demo_Project SHALL surface the compilation error through the standard Maven build failure (non-zero exit code) rather than silently skipping the class.
4. THE Smelly_Demo_Project SHALL declare zero runtime dependencies beyond the Java 17 standard library in its `pom.xml`.
