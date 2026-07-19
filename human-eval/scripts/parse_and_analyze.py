#!/usr/bin/env python3
"""
parse_and_analyze.py - parse the 20 GENUINE agent-reviewed questionnaires and
compute per-tool statistics + inter-rater agreement.

Input : human-eval/questionnaire_01..20.md   (genuine reviews, Q1-Q5 per item)
        human-eval/sample-key.csv            (gid -> tool, tier)
Output: human-eval/results.csv               (item,reviewer,tool,Q1,Q2,Q3,Q4)
        human-eval/stats.txt                  (per-tool + agreement summary)

The questionnaire item block format produced by the reviewer agents is:
  ## S001
  Source class: ...
  Method: ...
  Target class: ...
  - Q1: Reject
  - Q2: 1
  - Q3: 2
  - Q4: <reasons; ...>  (or n/a)
  - Q5: <free text>
"""
import os, csv, re, glob
from collections import defaultdict

HE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
KEY = os.path.join(HE, "sample-key.csv")

tool_of = {}
tier_of = {}
with open(KEY, encoding="utf-8") as f:
    for r in csv.DictReader(f):
        tool_of[r["gid"]] = r["tool"]
        tier_of[r["gid"]] = r.get("tier", "")

def parse_int(s):
    m = re.search(r"-?\d+", s)
    return int(m.group()) if m else None

rows = []  # (item, reviewer, tool, Q1, Q2, Q3, Q4)
for path in sorted(glob.glob(os.path.join(HE, "questionnaire_*.md"))):
    rid = "R" + re.search(r"questionnaire_(\d+)", path).group(1)
    text = open(path, encoding="utf-8").read()
    # split into blocks starting at "## S###"
    blocks = re.split(r"\n##\s+(S\d{3})\b", text)
    # blocks[0] = header; then pairs (gid, body)
    for i in range(1, len(blocks), 2):
        gid = blocks[i]; body = blocks[i+1]
        def field(tag):
            m = re.search(r"-\s*"+tag+r"\s*:\s*(.*)", body)
            return m.group(1).strip() if m else ""
        q1 = field("Q1")
        q1n = "Accept" if q1.lower().startswith("accept") else \
              "Unsure" if q1.lower().startswith("unsure") else \
              "Reject" if q1.lower().startswith("reject") else q1
        q2 = parse_int(field("Q2")); q3 = parse_int(field("Q3"))
        q4 = field("Q4")
        rows.append((gid, rid, tool_of.get(gid, "?"), q1n, q2, q3, q4))

# write results.csv
with open(os.path.join(HE, "results.csv"), "w", newline="", encoding="utf-8") as f:
    w = csv.writer(f)
    w.writerow(["item","reviewer","tool","Q1","Q2","Q3","Q4"])
    for r in sorted(rows):
        w.writerow(r)

# ---------- per-tool stats ----------
tools = ["ClassTrim","HMove","JDeodorant","JMove","REsolution"]
by_tool = defaultdict(list)
for r in rows:
    by_tool[r[2]].append(r)

def mean(xs):
    xs=[x for x in xs if x is not None]
    return sum(xs)/len(xs) if xs else float("nan")

lines = []
lines.append("GENUINE AI-AGENT REVIEW - RESULTS")
lines.append("(20 experienced-Java-developer sub-agents, blinded to tool, each")
lines.append(" read the real project source before answering. SIMULATED expert")
lines.append(" panel - NOT human subjects.)")
lines.append("")
lines.append(f"total reviews parsed: {len(rows)}  (expected 200 = 100 items x 2)")
lines.append("")
hdr = f"{'tool':<11} {'n':>3} {'Acc%':>6} {'Uns%':>6} {'Rej%':>6} {'meanQ2':>7} {'meanQ3':>7}"
lines.append(hdr); lines.append("-"*len(hdr))
for t in tools:
    rs = by_tool.get(t, [])
    n = len(rs)
    if n==0: continue
    acc = sum(1 for r in rs if r[3]=="Accept")
    uns = sum(1 for r in rs if r[3]=="Unsure")
    rej = sum(1 for r in rs if r[3]=="Reject")
    lines.append(f"{t:<11} {n:>3} {100*acc/n:>6.1f} {100*uns/n:>6.1f} {100*rej/n:>6.1f} "
                 f"{mean([r[4] for r in rs]):>7.2f} {mean([r[5] for r in rs]):>7.2f}")

# ---------- Q4 reason frequencies (rejections/unsure) ----------
lines.append("")
lines.append("Q4 rejection-reason frequency (all tools, non-n/a):")
reasons = defaultdict(int)
for r in rows:
    q4 = r[6]
    if not q4 or q4.lower() in ("n/a","na",""): continue
    for part in re.split(r";", q4):
        p = part.strip()
        if p and p.lower() != "n/a":
            reasons[p] += 1
for k,v in sorted(reasons.items(), key=lambda kv:-kv[1]):
    lines.append(f"  {v:>3}  {k}")

# ---------- diagnostic-value gap (Q3 vs acceptance) per tool ----------
lines.append("")
lines.append("Diagnostic value: mean Q3 vs acceptance rate (the 'detects what,")
lines.append("not where' check - Q3 high while acceptance low = flags real smells):")
for t in tools:
    rs = by_tool.get(t, [])
    if not rs: continue
    n=len(rs); acc=sum(1 for r in rs if r[3]=="Accept")
    lines.append(f"  {t:<11} accept={100*acc/n:>5.1f}%  meanQ3={mean([r[5] for r in rs]):.2f}")

# ---------- inter-rater agreement on Q1 (per item, 2 reviewers) ----------
by_item = defaultdict(list)
for r in rows:
    by_item[r[0]].append(r[3])
paired = {g:v for g,v in by_item.items() if len(v)==2}
raw_agree = sum(1 for v in paired.values() if v[0]==v[1])
# Cohen's kappa (3 categories) over the paired items
cats = ["Accept","Unsure","Reject"]
a = [v[0] for v in paired.values()]; b=[v[1] for v in paired.values()]
N=len(paired)
po = raw_agree/N if N else float("nan")
def pdist(x):
    return {c: x.count(c)/len(x) for c in cats}
pa=pdist(a); pb=pdist(b)
pe = sum(pa[c]*pb[c] for c in cats)
kappa = (po-pe)/(1-pe) if (1-pe) else float("nan")
lines.append("")
lines.append(f"Inter-rater agreement on Q1 over {N} paired items:")
lines.append(f"  raw agreement = {100*po:.1f}%   Cohen's kappa = {kappa:.3f}")
disagree=[g for g,v in paired.items() if v[0]!=v[1]]
lines.append(f"  disagreement items ({len(disagree)}): {', '.join(sorted(disagree))}")

# ---------- items that reached any Accept ----------
acc_items = sorted({r[0] for r in rows if r[3]=="Accept"})
lines.append("")
lines.append(f"Items with >=1 Accept ({len(acc_items)}):")
for g in acc_items:
    v = by_item[g]
    lines.append(f"  {g}  [{tool_of[g]:<10}] {'/'.join(v)}")

open(os.path.join(HE,"stats.txt"),"w",encoding="utf-8").write("\n".join(lines)+"\n")
print("\n".join(lines))
