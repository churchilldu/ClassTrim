# Human Evaluation Plan — Move-Method Refactoring Suggestions

## 1. Objective

Evaluate the developer-perceived quality of Move-Method suggestions produced by
**ClassTrim (ours)** against four baseline tools **HMove, JDeodorant, JMove,
REsolution**, using a blinded expert questionnaire. Primary questions:

- RQ1 (Acceptance): What fraction of each tool's suggestions would experienced
  developers apply? (questionnaire Q1)
- RQ2 (Usefulness): How useful/reasonable are the suggestions on a 1-5 scale? (Q2)
- RQ3 (Diagnostic value): Even when rejected, do suggestions flag code worth
  inspecting? (Q3)
- RQ4 (Rejection reasons): Why are suggestions rejected? (Q4)
- RQ5 (Agreement): Do our results align with the tool-vs-tool overlap already
  computed in `output/baseline-overlap.md`?

## 2. Materials

- Suggestion sources:
  - ClassTrim: selected-run diffs `output/<system>/<folder>/<system>-diff-01.tsv`
    (folders per `output/*/*-summary.tsv` matched to `final_result.tsv`).
  - HMove / JDeodorant / REsolution: `baseline/<Tool>/<system>/<system>.tsv`.
  - JMove: `baseline/JMove/jEdit-4_2/jEdit-4_2.tsv` (only system with data).
- Metrics/thresholds for context: `output/<system>/<folder>/<system>-metrics-01.tsv`,
  `output/threshold.tsv`.
- Questionnaire template: `questionnaire.md` (Q1-Q5, reproduced in Section 8).
- Source code for context: `C:\codeRefactoring\dataset\<system>\...-src`.

## 3. Sampling design

- **Per tool: 20 suggestions**, 5 tools -> **100 sampled suggestions**.
- **Reproducibility:** uniform random sampling with a fixed seed (`SEED = 42`),
  recorded with each pick. Sampling script and the resulting IDs are archived.
- **Pools (available suggestions):** ClassTrim 2418, HMove 954, JDeodorant 254,
  JMove 64 (jEdit-4_2 only), REsolution 161.
- **Stratification:** where a tool spans multiple systems, sample proportionally
  across systems so no single subject dominates (JMove is jEdit-4_2 only).
- **ClassTrim "average-level" guardrail (anti-cherry-picking):**
  - Draw uniformly at random from the full ClassTrim pool.
  - Explicitly **exclude the known standout** `parseTarHeader -> TarUtils`
    (ant-1_6) and any other hand-identified feature-envy gems, so the sample
    reflects the *typical* (mostly metric-driven) suggestion mix.
  - Cap "clearly-good feature-envy" picks at <= 1 of the 20; the rest are the
    average metric-driven moves. This keeps ClassTrim at a representative, not
    flattering, level.
- **Blinding:** after sampling, pool all 100, shuffle (seeded), and assign
  neutral global IDs `S001`-`S100`. Reviewers never see the originating tool,
  the internal tool IDs, or metric deltas that could hint at the tool.

### Internal (unblinded) bookkeeping
Each sampled item keeps a private record: `tool`, `tool_local_id`
(e.g., `CT07`, `HM12`, `JD03`, `JM11`, `RE05`), `system`, and the global `Sxxx`.
This mapping lives in a **separate** file not shown to reviewers.

## 4. Suggestion "card" schema (what a reviewer sees)

Each item is presented tool-blinded with just enough context to judge:

```
ID: S###
Subject system: <system>
Source class: <fully-qualified source class>
Method: <method signature>
Target class: <fully-qualified target class>
Context (optional, neutral): short note on method body / where it is used
  (e.g., "method makes N calls to the target class", "method is a setter that
  stores a field") — phrased identically regardless of tool.
```

No metrics, no tool name, no "accepted/rejected" hints.

## 5. Reviewer panel — 20 expert agents

- **20 agents**, each acting as an **experienced Java developer** (8+ years,
  familiar with refactoring, code smells, and the Apache/jEdit style codebases).
- **Persona template** (instantiated R01-R20 with light variation in emphasis
  — testing, architecture, maintenance — to reduce correlated bias):
  > "You are a senior Java engineer with deep experience in object-oriented
  > design and refactoring (Fowler's catalog, code smells such as Feature Envy
  > and God Class). You judge Move-Method suggestions on cohesion, coupling,
  > single-responsibility, readability, and maintainability — not on metrics
  > alone. You are pragmatic and skeptical: you only accept a move you would
  > actually perform in a real code review."
- **Calibration:** all agents receive the same instructions and the exact
  questionnaire; they must answer only from the card + provided source context;
  if context is insufficient they may use Q4 "I need more project context."
- **Independence:** agents review independently; no cross-talk between the two
  reviewers of the same item.

## 6. Assignment design (each item reviewed by exactly 2 experts)

- 100 items, 20 reviewers, **10 items each**, **2 reviews per item**
  (100 x 2 = 200 = 20 x 10). Balanced.
- **Rule (sliding window over 20 blocks of 5):** split `S001`-`S100` into 20
  blocks of 5 (`b_j = S[(j-1)*5+1 .. (j-1)*5+5]`). Reviewer `R_i` reviews blocks
  `b_i` and `b_(i+1)` (indices mod 20). Every block is covered by exactly two
  reviewers (`R_(j-1)` and `R_j`); adjacent reviewers share 5 items, enabling
  clean pairwise agreement.

| Reviewer | Items | Reviewer | Items |
|---|---|---|---|
| R01 | S001-S010 | R11 | S051-S060 |
| R02 | S006-S015 | R12 | S056-S065 |
| R03 | S011-S020 | R13 | S061-S070 |
| R04 | S016-S025 | R14 | S066-S075 |
| R05 | S021-S030 | R15 | S071-S080 |
| R06 | S026-S035 | R16 | S076-S085 |
| R07 | S031-S040 | R17 | S081-S090 |
| R08 | S036-S045 | R18 | S086-S095 |
| R09 | S041-S050 | R19 | S091-S100 |
| R10 | S046-S055 | R20 | S096-S100 + S001-S005 |

(Each `Sxxx` appears in exactly two reviewer rows.)

## 7. Review procedure (per agent, per item)

1. Read the blinded card and any provided source context.
2. Answer Q1-Q5 from the questionnaire (Section 8) using only that information.
3. Record answers in the reviewer's output file (Section 9).
4. No revisiting after seeing other reviewers' answers.

## 8. Questionnaire (per item; from `questionnaire.md`)

- **Q1. Would you apply this Move Method refactoring?** Accept / Unsure / Reject
- **Q2. How reasonable is this recommendation?** 1 (not useful) - 5 (very useful)
- **Q3. Even if you would not apply it, does it help identify code that deserves
  further inspection/refactoring?** 1 (strongly disagree) - 5 (strongly agree)
- **Q4. If you would not apply it, primary reasons (select all):**
  method belongs to original class / target too large / reduces readability /
  weakens cohesion / increases coupling / violates responsibilities /
  need more project context / other.
- **Q5. Free-text explanation / comments.**

## 9. Output artifacts

Create a folder `human-eval/`:

- `human-eval/sampled-suggestions.md` — the 100 blinded cards (`S001`-`S100`).
- `human-eval/sample-key.csv` — private mapping `Sxxx,tool,tool_local_id,system,
  source,method,target` (NOT given to reviewers).
- `human-eval/questionnaire_01.md` ... `questionnaire_20.md` — one file per
  reviewer, each containing that reviewer's 10 items, each item filling the
  Q1-Q5 template. File header records the reviewer persona and assigned IDs.
- `human-eval/results.csv` — flattened responses:
  `item_id,reviewer_id,tool,Q1,Q2,Q3,Q4(list),Q5`.

### `questionnaire_xx.md` per-item block format
```
## S042
Source class: ...
Method: ...
Target class: ...

- Q1: Accept | Unsure | Reject
- Q2: <1-5>
- Q3: <1-5>
- Q4: [reasons]
- Q5: <free text>
```

## 10. Analysis plan

- **Per tool:** acceptance rate (Accept / total), % Unsure, % Reject;
  mean and distribution of Q2 and Q3; Q4 reason frequencies.
- **ClassTrim vs each baseline:** compare acceptance and Q2/Q3 means
  (report deltas; note that with n=20 per tool this is indicative, not powered
  for strong significance claims — state that explicitly).
- **Diagnostic value (Q3):** test the "detects what, not where" hypothesis —
  expect ClassTrim's Q3 (worth inspecting) to be higher than its Q1 acceptance.
- **Inter-rater agreement:** on Q1 per item pair, report raw agreement and
  Cohen's kappa (collapsing Accept/Unsure/Reject); flag items with disagreement.
- **Cross-check** against `output/baseline-overlap.md`: items that were
  method-level agreements between tools should tend to score higher.

## 11. Bias controls & threats to validity

- **Blinding** to tool identity; neutral card wording; shuffled IDs.
- **Average-level sampling** for ClassTrim (Section 3) to avoid inflating our
  results; standout examples excluded.
- **Two independent reviewers** per item; balanced load.
- **Construct validity:** questionnaire fixed in advance (`questionnaire.md`).
- **Limitations to disclose:** n=20/tool is small; agent "experts" are LLM
  personas, not human developers (this is a simulated evaluation and must be
  labelled as such in any write-up); JMove sample is jEdit-4_2-only.

## 12. Execution checklist

1. [ ] Implement seeded sampler; draw 20 per tool with the ClassTrim guardrail.
2. [ ] Build `sample-key.csv` and shuffled `S001`-`S100`.
3. [ ] Generate `sampled-suggestions.md` (blinded cards) + optional source context.
4. [ ] Instantiate 20 reviewer personas R01-R20.
5. [ ] Apply the Section 6 assignment matrix.
6. [ ] Each agent completes its 10 items -> `questionnaire_01..20.md`.
7. [ ] Flatten to `results.csv`.
8. [ ] Run Section 10 analysis; write `human-eval/report.md`.

## 13. Deliverables

- This plan (`human-evaluation-plan.md`).
- `human-eval/` folder with cards, key, 20 completed questionnaires, results, and
  the analysis report.
