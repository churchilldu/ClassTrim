#!/usr/bin/env python3
"""
analyze.py  -  Summarize results.csv into per-tool and cross-cutting statistics.

Input : human-eval/results.csv
Output: prints stats and writes human-eval/stats.txt
"""
import os, csv
from collections import defaultdict, Counter
HE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

rows=[]
with open(os.path.join(HE,"results.csv"),encoding="utf-8") as f:
    for r in csv.DictReader(f):
        r["Q2"]=int(r["Q2"]); r["Q3"]=int(r["Q3"]); rows.append(r)

tools=["ClassTrim","HMove","JDeodorant","JMove","REsolution"]
def stats(sub):
    n=len(sub); c=Counter(r["Q1"] for r in sub)
    a=c.get("Accept",0); u=c.get("Unsure",0); rj=c.get("Reject",0)
    return n,a,u,rj,a/n*100,sum(r["Q2"] for r in sub)/n,sum(r["Q3"] for r in sub)/n

out=["PER-TOOL: n, Accept, Unsure, Reject, Accept%, meanQ2, meanQ3"]
for t in tools:
    n,a,u,rj,ap,q2,q3=stats([r for r in rows if r["tool"]==t])
    out.append(f"{t:11s} n={n} A={a:2d} U={u:2d} R={rj:2d} Acc%={ap:5.1f} Q2={q2:.2f} Q3={q3:.2f}")
n,a,u,rj,ap,q2,q3=stats(rows)
out.append(f"{'ALL':11s} n={n} A={a} U={u} R={rj} Acc%={ap:.1f} Q2={q2:.2f} Q3={q3:.2f}")

byitem=defaultdict(list)
for r in rows: byitem[r["item"]].append(r["Q1"])
ag=sum(1 for v in byitem.values() if len(v)==2 and v[0]==v[1])
out.append(f"\nITEM-LEVEL Q1 AGREEMENT: {ag}/{len(byitem)} = {ag/len(byitem)*100:.1f}%")
out.append("pair patterns: "+"; ".join(f"{a}={b}" for a,b in sorted(Counter(tuple(sorted(v)) for v in byitem.values()).items())))

q4=Counter()
for r in rows:
    if r["Q4"].strip():
        for x in r["Q4"].split("|"): q4[x.strip()]+=1
out.append("\nQ4 REASON FREQ:")
for reason,cnt in q4.most_common(): out.append(f"  {cnt}\t{reason}")

txt="\n".join(out)
open(os.path.join(HE,"stats.txt"),"w",encoding="utf-8").write(txt+"\n")
print(txt)
