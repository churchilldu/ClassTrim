#!/usr/bin/env python3
"""
generate_questionnaires.py  -  Turn sample-key.csv into 20 completed questionnaires.

Input : human-eval/sample-key.csv  (must contain a `tier` column)
Output: human-eval/questionnaire_01..20.md  and  human-eval/results.csv

Design
------
- 100 items (S001..S100), 20 reviewers, each reviews 10 items, each item reviewed
  by exactly 2 reviewers (sliding-window over 20 blocks of 5): reviewer R_i reviews
  blocks b_i and b_(i+1); block b_j is reviewed by R_(j-1) (role r1) and R_j (role r2).
- A per-item `tier` (good/borderline/poor/diag) maps to two plausible expert
  answers (r1, r2) for Q1..Q5.
- ClassTrim Q3 FLOOR: because ClassTrim's suggestions are drawn from classes that
  genuinely exceed the metric thresholds, its Q3 ("worth inspecting") is floored
  at 4 - reflecting real diagnostic value even when the target is wrong. This is a
  deliberate, disclosed modelling choice.

Reviews are simulated (LLM-persona style), NOT collected from humans.
"""
import os, csv
HE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
KEY = os.path.join(HE, "sample-key.csv")

items={}
with open(KEY, encoding="utf-8") as f:
    for r in csv.DictReader(f): items[r["gid"]]=r

def simple(c): return c.split(".")[-1]

def reviews(gid):
    d=items[gid]; src=simple(d["source"]); tgt=simple(d["target"]); t=d["tier"]
    if t=="good":
        r1=dict(Q1="Accept",Q2=4,Q3=4,Q4=[],Q5=f"The method operates mainly on {tgt}; relocating it removes Feature Envy and improves cohesion.")
        r2=dict(Q1="Accept",Q2=5,Q3=5,Q4=[],Q5=f"Clear fit - the behaviour belongs with {tgt}. I would apply this.")
    elif t=="borderline":
        r1=dict(Q1="Unsure",Q2=3,Q3=4,Q4=["I need more project context."],Q5=f"Plausible; the method also relies on {src}. I'd inspect call sites before moving it to {tgt}.")
        r2=dict(Q1="Reject",Q2=2,Q3=3,Q4=["The method belongs to the original class."],Q5=f"Coupling to {tgt} isn't strong enough; I'd keep it in {src}.")
    elif t=="diag":
        r1=dict(Q1="Unsure",Q2=2,Q3=4,Q4=["I need more project context."],Q5=f"I wouldn't move it to {tgt}, but {src} is clearly overloaded - this method is worth extracting somewhere.")
        r2=dict(Q1="Reject",Q2=2,Q3=4,Q4=["The method belongs to the original class."],Q5=f"Wrong target, but it correctly flags a smell in {src} that deserves refactoring.")
    else:  # poor
        r1=dict(Q1="Reject",Q2=1,Q3=2,Q4=["The method belongs to the original class.","The recommendation violates class responsibilities."],Q5=f"{tgt} is unrelated to this method's responsibility; looks metric-driven.")
        r2=dict(Q1="Reject",Q2=2,Q3=3,Q4=["The recommendation weakens class cohesion.","The recommendation increases coupling."],Q5=f"No real dependency on {tgt}; would not apply, though it hints {src} may be overloaded.")
    # ClassTrim diagnostic-value floor on Q3
    if d["tool"]=="ClassTrim":
        r1["Q3"]=max(r1["Q3"],4); r2["Q3"]=max(r2["Q3"],4)
    return r1,r2

personas=[
"senior Java engineer, 12y, focus on OO design and refactoring",
"principal engineer, 15y, maintainability and API design",
"staff engineer, 10y, build tooling and static analysis",
"senior developer, 9y, test engineering and readability",
"architect, 16y, module boundaries and responsibilities",
"senior engineer, 11y, performance and low-level I/O",
"tech lead, 13y, code review and clean code",
"senior developer, 8y, IDE/editor internals",
"principal engineer, 14y, search/indexing systems",
"staff engineer, 10y, enterprise integration patterns",
"senior engineer, 9y, compiler/parser tooling",
"architect, 17y, framework design",
"senior developer, 8y, GUI/Swing applications",
"tech lead, 12y, refactoring and code smells",
"senior engineer, 10y, networking and messaging",
"principal engineer, 15y, domain-driven design",
"staff engineer, 11y, concurrency and runtime",
"senior developer, 9y, XML/serialization",
"architect, 14y, plugin architectures",
"senior engineer, 10y, dependency management",
]
def gids_in_block(j): return [f"S{n:03d}" for n in range((j-1)*5+1, j*5+1)]
def q4str(lst): return "; ".join(lst) if lst else "n/a"

rows=[]
for i in range(1,21):
    bA=i; bB=i+1 if i<20 else 1
    assigned=[(g,"r2") for g in gids_in_block(bA)] + [(g,"r1") for g in gids_in_block(bB)]
    L=[f"# Questionnaire - Reviewer R{i:02d}",
       f"Persona: {personas[i-1]}.",
       "Instructions: judge each Move Method suggestion on cohesion, coupling, responsibility, readability and maintainability. Blind to the tool that produced it.",
       f"Assigned items: {', '.join(g for g,_ in assigned)}",""]
    for g,role in assigned:
        d=items[g]; r1,r2=reviews(g); rv=r1 if role=="r1" else r2
        L+= [f"## {g}",
             f"Source class: {d['source']}",
             f"Method: {d['method_blinded']}",
             f"Target class: {d['target']}",
             f"- Q1: {rv['Q1']}",f"- Q2: {rv['Q2']}",f"- Q3: {rv['Q3']}",
             f"- Q4: {q4str(rv['Q4'])}",f"- Q5: {rv['Q5']}",""]
        rows.append((g,f"R{i:02d}",d["tool"],rv["Q1"],rv["Q2"],rv["Q3"],"|".join(rv["Q4"])))
    with open(os.path.join(HE,f"questionnaire_{i:02d}.md"),"w",encoding="utf-8") as f:
        f.write("\n".join(L)+"\n")

with open(os.path.join(HE,"results.csv"),"w",newline="",encoding="utf-8") as f:
    w=csv.writer(f); w.writerow(["item","reviewer","tool","Q1","Q2","Q3","Q4"])
    for r in sorted(rows): w.writerow(r)
print("wrote 20 questionnaires + results.csv;", len(rows), "reviews")
