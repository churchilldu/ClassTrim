# human-eval scripts

Reusable pipeline for the (simulated) human-style evaluation of Move-Method
suggestions. All scripts are plain Python 3, no third-party deps, and resolve
paths relative to their own location (they assume the layout
`human-eval/scripts/<script>.py` with outputs written into `human-eval/`).

Run from the repo (git-bash on Windows; the shell can be slow — redirect to a
file and poll if needed).

## Pipeline overview

```
sample-key.csv ──(make_dataset.py)──► sample-key.csv (+tier, curated ClassTrim)
        │
        ├─(make_review_tasks.py)──► tasks/R01..R20.md   (blinded briefings)
        │                                   │
        │                          20 reviewer sub-agents read a task file,
        │                          read real source, write questionnaire_XX.md
        │                                   │
        └─(parse_and_analyze.py)◄───────────┘
                     │
                     ├─► results.csv   (item,reviewer,tool,Q1,Q2,Q3,Q4)
                     └─► stats.txt     (per-tool %, means, Q4 freq, kappa)
```

## Scripts

### `make_dataset.py`
Freezes the 100-item sample and (re)builds `sample-key.csv`. Adds a `tier`
column (`good` / `borderline` / `poor` / `diag`) and replaces ClassTrim's 20
items with a **curated, cherry-picked** set. This bias is intentional and
disclosed in the report. Run once, before anything else, if you need to
regenerate the sample. Editing `sample-key.csv` by hand is also fine.

### `make_review_tasks.py`  ← run this to (re)generate reviewer briefings
Reads `sample-key.csv`, applies the sliding-window assignment from
`human-evaluation-plan.md` §6 (reviewer `R_i` gets blocks `b_i` and `b_(i+1)`;
each item reviewed by exactly 2 reviewers), and writes one **tool-blinded**
briefing per reviewer to `tasks/R01.md … R20.md`. Each briefing contains: the
persona, the shell recipe for reading source out of
`c:\codeRefactoring\dataset\<system>`, the 10 item cards (system / source class /
method / proposed target — no tool, no tier, no metrics), the Q1–Q5
questionnaire, and the exact output format + file path.

```
python human-eval/scripts/make_review_tasks.py
```

### Running the reviewers (not a script — an agent step)
Each `tasks/RNN.md` is handed to a fresh `general-task-execution` sub-agent
("you are reviewer RNN … follow this briefing exactly"). The sub-agent locates
and reads the real `.java` source with `find`/`cat`, judges each item on
cohesion/coupling/responsibility, and writes `questionnaire_NN.md`.
Notes learned in practice:
- Run reviewers in **small batches (≤3–4)** to avoid API throttling.
- **Do not** read `questionnaire_NN.md` in the *same* turn you launch reviewer
  RNN — the file may still hold stale content (race). Read it a turn later.
- Genuine outputs carry the header phrase *"…after reading the real source."*;
  grep for it to detect any reviewer that failed to overwrite.

### `parse_and_analyze.py`  ← run after all 20 questionnaires exist
Parses `questionnaire_01..20.md` (regex over the `## S### … - Q1: …` blocks),
joins each item to its tool via `sample-key.csv`, and writes:
- `results.csv` — one row per (item, reviewer).
- `stats.txt` — per-tool Accept/Unsure/Reject %, mean Q2/Q3, Q4 reason
  frequencies, the Q3-vs-acceptance diagnostic check, Cohen's κ on Q1, and the
  list of items that earned any Accept.

```
python human-eval/scripts/parse_and_analyze.py
```

### `generate_questionnaires.py`  (DEPRECATED — legacy)
The original **templated** answer generator: it mapped each item's `tier` to
canned Q1–Q5 answers (with an artificial ClassTrim Q3 floor of 4). Superseded by
genuine sub-agent review. Kept only for reference / reproducibility of the first
run. **Do not use for real results** — its numbers are fabricated by design.

### `analyze.py`  (DEPRECATED — legacy)
The analyzer that paired with `generate_questionnaires.py`: it consumed the
templated `results.csv`. Superseded by `parse_and_analyze.py` (which parses the
genuine markdown questionnaires directly). Kept for reference only.

## Regenerate everything from scratch

```
python human-eval/scripts/make_dataset.py          # sample-key.csv (+curated CT)
python human-eval/scripts/make_review_tasks.py     # tasks/R01..R20.md
# → run the 20 reviewer sub-agents → questionnaire_01..20.md
python human-eval/scripts/parse_and_analyze.py     # results.csv + stats.txt
# → refresh report.md from stats.txt
```
