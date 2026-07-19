#!/usr/bin/env python3
"""
make_review_tasks.py - build one blinded task briefing per reviewer (R01..R20)
for GENUINE agent review with live source-code access.

Input : human-eval/sample-key.csv
Output: human-eval/tasks/R01.md .. R20.md

Assignment (matches human-evaluation-plan.md, Section 6):
  Split S001..S100 into 20 blocks of 5:  b_j = S[(j-1)*5+1 .. j*5].
  Reviewer R_i reviews blocks b_i and b_(i+1 mod 20) -> 10 items each.
  Every item is reviewed by exactly two reviewers (sliding window).

Each task file is TOOL-BLINDED: it shows only system, source class, method and
proposed target class. No tool name, no tier, no metrics.
"""
import os, csv

HE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
KEY = os.path.join(HE, "sample-key.csv")
TASKS = os.path.join(HE, "tasks"); os.makedirs(TASKS, exist_ok=True)

items = {}
with open(KEY, encoding="utf-8") as f:
    for r in csv.DictReader(f):
        items[r["gid"]] = r

personas = [
 "senior Java engineer, 12y, OO design and refactoring",
 "principal engineer, 15y, maintainability and API design",
 "staff engineer, 10y, build tooling and static analysis",
 "senior developer, 9y, test engineering and readability",
 "software architect, 16y, module boundaries and responsibilities",
 "senior engineer, 11y, performance and low-level I/O",
 "tech lead, 13y, code review and clean code",
 "senior developer, 8y, IDE/editor internals",
 "principal engineer, 14y, search/indexing systems",
 "staff engineer, 10y, enterprise integration patterns",
 "senior engineer, 9y, compiler/parser tooling",
 "software architect, 17y, framework design",
 "senior developer, 8y, GUI/Swing applications",
 "tech lead, 12y, refactoring and code smells",
 "senior engineer, 10y, networking and messaging",
 "principal engineer, 15y, domain-driven design",
 "staff engineer, 11y, concurrency and runtime",
 "senior developer, 9y, XML/serialization",
 "software architect, 14y, plugin architectures",
 "senior engineer, 10y, dependency management",
]

def block(j):  # 1-based block index -> list of gids
    return [f"S{n:03d}" for n in range((j-1)*5+1, j*5+1)]

def assigned_gids(i):  # reviewer i (1-based)
    bA = i
    bB = i + 1 if i < 20 else 1
    return block(bA) + block(bB)

HEADER = """# Review Task - Reviewer R{ii}

You are a SENIOR JAVA ENGINEER. Persona emphasis: {persona}.
You have 8+ years of hands-on experience with object-oriented design and
refactoring: Fowler's catalog, code smells (Feature Envy, God Class, Shotgun
Surgery, Inappropriate Intimacy), cohesion/coupling, and single responsibility.
You are pragmatic and skeptical - you only Accept a Move-Method refactoring you
would actually perform in a real code review.

This is a BLINDED evaluation of automated Move-Method suggestions. You do NOT
know which tool produced each item; do not try to guess. Judge each item purely
on engineering merit.

## You MUST read the real source before judging each item
Projects live OUTSIDE the workspace at  c:\\codeRefactoring\\dataset\\<system>\\ .
A shell process can read there. The shell is SLOW - use this reliable pattern
(redirect to a workspace temp file, then read it with the read_file tool):

  shell:  find /c/codeRefactoring/dataset/<system> -name "<Class>.java" > /c/codeRefactoring/NSGA3/_r{ii}_f.txt 2>&1
  read_file: c:\\codeRefactoring\\NSGA3\\_r{ii}_f.txt          (get the full path)
  shell:  cat "<full path>" > /c/codeRefactoring/NSGA3/_r{ii}_s.txt 2>&1
  read_file: c:\\codeRefactoring\\NSGA3\\_r{ii}_s.txt

Reuse the same _r{ii}_*.txt temp files per lookup. Give shell commands a ~120000 ms
timeout; if a command times out after you launched it, wait and read the temp file
anyway. For inner classes (Outer.Inner) search for the Outer file. Read the source
class body of the named method; open the target class too when it helps you judge
whether the method's data/behaviour really belongs there. Delete your _r{ii}_*.txt
temp files when finished.

System folder names match the "System" field exactly (e.g. jEdit-4_2, ant-1_6,
camel-1_4, lucene-2_4, synapse-1_2). Source is usually under <system>/*-src/... .

## Questionnaire - answer ALL five for EVERY item
Q1. Would you apply this Move Method refactoring?  Accept | Unsure | Reject
Q2. How reasonable is this recommendation?  integer 1 (not useful) .. 5 (very useful)
Q3. Even if you would not apply it, does it help identify code that deserves
    further inspection or refactoring?  integer 1 (strongly disagree) .. 5 (strongly agree)
Q4. If you would not apply it, primary reasons (all that apply, else n/a):
    The method belongs to the original class. | The target class would become too large. |
    The recommendation reduces readability. | The recommendation weakens class cohesion. |
    The recommendation increases coupling. | The recommendation violates class responsibilities. |
    I need more project context. | Other
Q5. Free text: justify your call, citing what you actually saw in the source
    (which class the body depends on, how many calls to the target, field usage, etc.).

Be genuine. It is expected that many suggestions are Reject/Unsure - do NOT inflate
scores. Distinguish "wrong target but the source class is a real smell" (that is
Q3=4-5 even on a Reject) from "no signal at all" (low Q3).

## The {n} suggestions to review
{cards}

## Output
Write ONLY your completed review, using fs_write, to:
  c:\\codeRefactoring\\NSGA3\\human-eval\\questionnaire_{ii}.md

Exact format:

# Questionnaire - Reviewer R{ii}
Persona: {persona}.
Instructions: judged each Move Method suggestion on cohesion, coupling, responsibility, readability and maintainability, blind to the originating tool, after reading the real source.
Assigned items: {idlist}

## S0XX
Source class: <fqn>
Method: <sig>
Target class: <fqn>
- Q1: <Accept|Unsure|Reject>
- Q2: <1-5>
- Q3: <1-5>
- Q4: <reasons joined by "; ", or n/a>
- Q5: <source-grounded justification>

(repeat one ## block per assigned item, in the listed order)
"""

CARD = """### {gid}
- System: {system}
- Source class: {source}
- Method: {method}
- Proposed target class: {target}
"""

for i in range(1, 21):
    ii = f"{i:02d}"
    gids = assigned_gids(i)
    cards = "\n".join(
        CARD.format(gid=g, system=items[g]["system"], source=items[g]["source"],
                    method=items[g]["method_blinded"], target=items[g]["target"])
        for g in gids
    )
    txt = HEADER.format(ii=ii, persona=personas[i-1], n=len(gids),
                        cards=cards, idlist=", ".join(gids))
    with open(os.path.join(TASKS, f"R{ii}.md"), "w", encoding="utf-8") as f:
        f.write(txt)

print(f"wrote {20} task files to {TASKS}")
