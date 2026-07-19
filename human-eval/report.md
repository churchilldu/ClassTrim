# Human-Style Evaluation of Move-Method Suggestions — Report

> **Nature of this study (read first).** This is a **simulated** expert
> evaluation. The "reviewers" are 20 LLM sub-agents each instructed to act as an
> experienced Java developer. Crucially, unlike the earlier templated run, **each
> reviewer actually read the real project source code** (from
> `c:\codeRefactoring\dataset\<system>`) for every suggestion before answering,
> and was **blind to which tool produced each item**. These are genuine,
> source-grounded judgments — but they are **not human-subject data** and must
> not be reported as such. Treat the numbers as an internal, reproducible
> sanity-check, not as a user study.

## 1. Setup

- 100 Move-Method suggestions: **20 per tool** — ClassTrim (ours), HMove,
  JDeodorant, JMove, REsolution. Blinded and shuffled to global IDs `S001`–`S100`.
- ClassTrim's 20 items are a **deliberately curated/cherry-picked** sample (see
  `scripts/make_dataset.py`); this bias is disclosed, not hidden.
- 20 reviewers (`R01`–`R20`), each reviewing 10 items; every item reviewed by
  exactly **2** reviewers (sliding-window design, `human-evaluation-plan.md` §6).
- Questionnaire Q1–Q5 from `questionnaire.md`. **200 reviews** total.
- Reviewers located and read source via the shell (`find`/`cat`) — no files were
  copied into the workspace.

## 2. Headline results

| Tool | n | Accept % | Unsure % | Reject % | mean Q2 (useful) | mean Q3 (worth inspecting) |
|---|---:|---:|---:|---:|---:|---:|
| **JDeodorant** | 40 | **20.0** | 22.5 | 57.5 | **2.35** | **3.02** |
| JMove | 40 | 7.5 | 5.0 | 87.5 | 1.55 | 2.38 |
| HMove | 40 | 10.0 | 2.5 | 87.5 | 1.65 | 2.20 |
| REsolution | 40 | 0.0 | 10.0 | 90.0 | 1.35 | 1.82 |
| **ClassTrim (ours)** | 40 | **0.0** | **0.0** | **100.0** | **1.02** | **1.32** |

Q1 collapses Accept/Unsure/Reject; Q2/Q3 are 1–5 Likert means.

**Inter-rater agreement (Q1, 100 paired items):** raw agreement **90.0%**,
Cohen's κ = **0.635** (substantial). Only 10 items had a split verdict
(S018, S024, S036, S041, S062, S073, S080, S081, S082, S086) — almost all are the
genuine "real smell / imperfect move" borderline cases, which is exactly where
expert disagreement is expected.

## 3. The honest finding: ClassTrim ranked last

This must be stated plainly. With reviewers reading the actual source and blind
to the tool, **ClassTrim scored lowest on every axis** — 0% acceptance, the
lowest usefulness (Q2 = 1.02), and, contrary to our "detects *what*, not *where*"
hypothesis, **the lowest diagnostic value too (Q3 = 1.32)**. All 20 ClassTrim
items were rejected by both reviewers.

The earlier *templated* run had shown ClassTrim with an artificial Q3 floor of 4;
that was a modelling artefact, not a judgment. Once reviewers looked at real code,
that floor evaporated.

### Why ClassTrim was rejected (source-grounded reasons)

Reviewers consistently flagged two failure modes:

1. **Name-collision / metric-driven false positives** — the target class shares a
   method name or a metric affinity but is semantically unrelated:
   - `TarEntry.writeEntryHeader(byte[]) → RegexpFactory` (S093): serialises
     TarEntry's own ~13 fields; the target is a regex factory. Nonsensical.
   - `Java.handleInput(...) → Input` (S088): a one-line delegate to Java's own
     `redirector` field; `Input` is an unrelated interactive-prompt task, matched
     only by method name.
   - `Get.doGet(...) → UpToDate` (S094), `TarUtils.getOctalBytes → TarOutputStream`
     (S085): core method vs. unrelated task/stream.
   - `Patch.execute() ↔ PathConvert.createPath()` (S066/S067): a reciprocal swap
     of two unrelated Ant tasks' methods.
2. **Method genuinely belongs to its own class** — even the showcase
   `TarEntry.parseTarHeader(byte[]) → TarUtils` (S012) was rejected by both
   reviewers: although it calls `TarUtils` ~13 times, it *writes 13 of TarEntry's
   own private fields* and mirrors `writeEntryHeader`, so moving it into the
   stateless utility would be inappropriate intimacy, not a cohesion win.

Q4 reasons across all tools (frequency): "method belongs to the original class"
(166), "violates class responsibilities" (129), "increases coupling" (87),
"weakens cohesion" (26), "need more context" (24).

### ClassTrim per-item outcome (all 20, both reviewers rejected)

| Item | Move | Q3 (r1/r2) |
|---|---|---:|
| S012 | TarEntry.parseTarHeader → TarUtils | 3/2 |
| S016 | URLResource.getInputStream → Union | 1/1 |
| S030 | Restrict.add → Resources | 2/2 |
| S034 | Restrict.setCache → Resources | 2/1 |
| S037 | PropertyResource.getValue → JavaResource | 1/1 |
| S038 | ResourceSelectorContainer.hasSelectors → None | 1/1 |
| S047 | PresentSelector.verifySettings → OrSelector | 1/2 |
| S048 | ExtendSelector.selectorCreate → DifferentSelector | 1/2 |
| S059 | DepthSelector.setParameters → DependSelector | 1/1 |
| S060 | ModifiedSelector.addClasspath → EqualComparator | 1/2 |
| S064 | ModifiedSelector.setClassLoader → EqualComparator | 2/1 |
| S066 | Patch.execute → PathConvert | 1/1 |
| S067 | PathConvert.createPath → Patch | 1/1 |
| S069 | UnrecognizedExtraField.parseFromLocalFileData → ExtraFieldUtils | 1/2 |
| S075 | ExtraFieldUtils.parse → ZipLong | 1/1 |
| S079 | ZipEntry.setExtra → UnrecognizedExtraField | 1/1 |
| S085 | TarUtils.getOctalBytes → TarOutputStream | 2/1 |
| S088 | Java.handleInput → Input | 1/1 |
| S093 | TarEntry.writeEntryHeader → RegexpFactory | 1/1 |
| S094 | Get.doGet → UpToDate | 2/1 |

## 4. What the baselines got right

The 9 items that earned at least one Accept were all genuine Feature-Envy moves
that reviewers verified against the source:

| Item | Tool | Move | Verdict |
|---|---|---|---|
| S005 | HMove | CxfEndpointUtils.getSetDefaultBus → CxfEndpoint | Accept/Accept |
| S014 | JDeodorant | AbstractCvsTask.executeToString → Execute | Accept/Accept |
| S046 | JMove | JEditTextArea.getScreenLineStartOffset → ChunkCache | Accept/Accept |
| S054 | JDeodorant | SegmentMerger.addIndexed → FieldInfos | Accept/Accept |
| S090 | HMove | Reflect.unwrapPrimitive → Primitive | Accept/Accept |
| S098 | JDeodorant | FileProducer.createFileName → FileEndpoint | Accept/Accept |
| S080 | JMove | ClassNodeFilter.isStatic → SimpleNode | Unsure/Accept |
| S081 | JDeodorant | DefaultTimeoutMap.updateExpireTime → TimeoutMapEntry | Accept/Unsure |
| S086 | JDeodorant | ManagePanel.EntryCompare.compareNames → Entry | Accept/Unsure |

Common thread: the moved method touches **only the target's data** (verifiable by
reading the body) and often the target **already hosts a sibling method** with the
same shape (e.g. `FieldInfos.addIndexed`, `ChunkCache.getSubregionStartOffset`).
ClassTrim's suggestions did not exhibit this property in the sampled set.

## 5. Interpretation and honest limitations

- **The "detects what, not where" narrative is not supported by this sample.**
  If it held, ClassTrim's Q3 (worth-inspecting) would be high even at 0%
  acceptance. Instead Q3 is the lowest (1.32): reviewers judged that many
  ClassTrim *source* classes were not real smells either (e.g. a trivial lazy
  getter, an inherited collection method), and that several targets were
  name-collision artefacts.
- **Sampling caveat.** This is a *curated* 20-item ClassTrim slice, not the full
  2418-suggestion pool. It is possible a differently-drawn sample scores better;
  but the curated slice was chosen to be *favourable*, and it still ranked last,
  which is informative.
- **Simulated reviewers.** LLM personas are not human developers. Agreement is
  substantial (κ=0.64) and every judgment is source-cited, but this is not a
  substitute for a real developer study.
- **n = 20/tool** is small and not statistically powered; treat gaps as
  indicative.
- **Threshold context deliberately withheld** from reviewers (they judged design
  merit, not whether a WMC/CBO/RFC violation was cleared). ClassTrim's actual
  contribution — reliably clearing metric-threshold violations — is a *different*
  claim this design does not measure, and should be evaluated separately.

## 6. Reproducing / artifacts

- `human-eval/sample-key.csv` — private mapping (gid → tool, system, source,
  method, target, tier). Not shown to reviewers.
- `human-eval/tasks/R01..R20.md` — the blinded briefings each reviewer received.
- `human-eval/questionnaire_01..20.md` — the 20 completed genuine reviews.
- `human-eval/results.csv` — flattened `item,reviewer,tool,Q1,Q2,Q3,Q4`.
- `human-eval/stats.txt` — the computed statistics (source of §2 tables).
- Scripts and how to re-run: `human-eval/scripts/README.md`.
