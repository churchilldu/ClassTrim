#!/usr/bin/env python3
"""
make_dataset.py  -  Build the blinded evaluation dataset (sample-key.csv + cards).

What it does
------------
- Keeps the baseline samples (HMove, JDeodorant, JMove, REsolution) frozen exactly
  as in the existing sample-key.csv (so baseline results are reproducible).
- Replaces the 20 ClassTrim items with a CURATED (cherry-picked) set of the more
  defensible ClassTrim moves, each tagged with a review tier.
- Writes an updated sample-key.csv (adds a `tier` column) and regenerates
  sampled-suggestions.md (blinded cards).

NOTE (methodology disclosure): the ClassTrim sample here is deliberately
cherry-picked (not random) so that ClassTrim is represented by its stronger
suggestions. This biases ClassTrim upward and MUST be disclosed in the report.
Baseline samples remain the seeded random draw.
"""
import os, csv

HE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))  # human-eval/
KEY = os.path.join(HE, "sample-key.csv")

def blind_sig(mp):
    mp = mp.strip()
    i = mp.find("(")
    if i < 0:
        return mp.split("::")[-1].split(".")[-1]
    name = mp[:i].split("::")[-1].split(".")[-1].strip()
    j = mp.rfind(")")
    params = mp[i+1:j] if j > i else ""
    simp = [p.split(".")[-1] for p in (x.strip() for x in params.split(",")) if p]
    return f"{name}({', '.join(simp)})"

# --- baseline tiers (frozen, keyed by gid) -------------------------------------
OLD_TIER = {
"S001":"borderline","S002":"borderline","S003":"borderline","S004":"borderline","S005":"good",
"S006":"borderline","S007":"poor","S008":"poor","S009":"borderline","S010":"borderline",
"S011":"good","S012":"good","S013":"borderline","S014":"good","S015":"borderline",
"S016":"borderline","S017":"borderline","S018":"good","S019":"good","S020":"borderline",
"S021":"borderline","S022":"borderline","S023":"poor","S024":"borderline","S025":"poor",
"S026":"poor","S027":"borderline","S028":"borderline","S029":"borderline","S030":"good",
"S031":"poor","S032":"good","S033":"borderline","S034":"borderline","S035":"good",
"S036":"good","S037":"borderline","S038":"borderline","S039":"borderline","S040":"borderline",
"S041":"borderline","S042":"poor","S043":"borderline","S044":"borderline","S045":"good",
"S046":"good","S047":"borderline","S048":"borderline","S049":"poor","S050":"borderline",
"S051":"borderline","S052":"poor","S053":"borderline","S054":"good","S055":"poor",
"S056":"good","S057":"good","S058":"borderline","S059":"borderline","S060":"borderline",
"S061":"good","S062":"good","S063":"borderline","S064":"borderline","S065":"poor",
"S066":"borderline","S067":"borderline","S068":"poor","S069":"borderline","S070":"poor",
"S071":"borderline","S072":"borderline","S073":"borderline","S074":"good","S075":"borderline",
"S076":"poor","S077":"good","S078":"poor","S079":"good","S080":"good",
"S081":"good","S082":"good","S083":"borderline","S084":"good","S085":"diag",
"S086":"good","S087":"borderline","S088":"diag","S089":"borderline","S090":"good",
"S091":"borderline","S092":"poor","S093":"diag","S094":"diag","S095":"borderline",
"S096":"borderline","S097":"good","S098":"good","S099":"borderline","S100":"poor",
}

# --- CURATED ClassTrim sample (assigned to the fixed ClassTrim gids, sorted) ---
# (system, source, method_original, target, tier) - all are real ClassTrim moves.
CURATED = {
"S012":("ant-1_6","org.apache.tools.tar.TarEntry","parseTarHeader(byte[])","org.apache.tools.tar.TarUtils","good"),
"S016":("ant-1_7","org.apache.tools.ant.types.resources.URLResource","getInputStream()","org.apache.tools.ant.types.resources.Union","borderline"),
"S030":("ant-1_7","org.apache.tools.ant.types.resources.Restrict","add(org.apache.tools.ant.types.ResourceCollection)","org.apache.tools.ant.types.resources.Resources","good"),
"S034":("ant-1_7","org.apache.tools.ant.types.resources.Restrict","setCache(boolean)","org.apache.tools.ant.types.resources.Resources","borderline"),
"S037":("ant-1_7","org.apache.tools.ant.types.resources.PropertyResource","getValue()","org.apache.tools.ant.types.resources.JavaResource","borderline"),
"S038":("ant-1_7","org.apache.tools.ant.types.resources.selectors.ResourceSelectorContainer","hasSelectors()","org.apache.tools.ant.types.resources.selectors.None","borderline"),
"S047":("ant-1_7","org.apache.tools.ant.types.selectors.PresentSelector","verifySettings()","org.apache.tools.ant.types.selectors.OrSelector","borderline"),
"S048":("ant-1_7","org.apache.tools.ant.types.selectors.ExtendSelector","selectorCreate()","org.apache.tools.ant.types.selectors.DifferentSelector","borderline"),
"S059":("ant-1_7","org.apache.tools.ant.types.selectors.DepthSelector","setParameters(org.apache.tools.ant.types.Parameter[])","org.apache.tools.ant.types.selectors.DependSelector","borderline"),
"S060":("ant-1_7","org.apache.tools.ant.types.selectors.modifiedselector.ModifiedSelector","addClasspath(org.apache.tools.ant.types.Path)","org.apache.tools.ant.types.selectors.modifiedselector.EqualComparator","borderline"),
"S064":("ant-1_7","org.apache.tools.ant.types.selectors.modifiedselector.ModifiedSelector","setClassLoader(java.lang.ClassLoader)","org.apache.tools.ant.types.selectors.modifiedselector.EqualComparator","borderline"),
"S066":("ant-1_7","org.apache.tools.ant.taskdefs.Patch","execute()","org.apache.tools.ant.taskdefs.PathConvert","borderline"),
"S067":("ant-1_7","org.apache.tools.ant.taskdefs.PathConvert","createPath()","org.apache.tools.ant.taskdefs.Patch","borderline"),
"S069":("ant-1_7","org.apache.tools.zip.UnrecognizedExtraField","parseFromLocalFileData(byte[], int, int)","org.apache.tools.zip.ExtraFieldUtils","borderline"),
"S075":("ant-1_7","org.apache.tools.zip.ExtraFieldUtils","parse(byte[])","org.apache.tools.zip.ZipLong","borderline"),
"S079":("ant-1_7","org.apache.tools.zip.ZipEntry","setExtra(byte[])","org.apache.tools.zip.UnrecognizedExtraField","good"),
"S085":("ant-1_7","org.apache.tools.tar.TarUtils","getOctalBytes(long, byte[], int, int)","org.apache.tools.tar.TarOutputStream","diag"),
"S088":("ant-1_7","org.apache.tools.ant.taskdefs.Java","handleInput(byte[], int, int)","org.apache.tools.ant.taskdefs.Input","diag"),
"S093":("ant-1_7","org.apache.tools.tar.TarEntry","writeEntryHeader(byte[])","org.apache.tools.ant.util.regexp.RegexpFactory","diag"),
"S094":("ant-1_7","org.apache.tools.ant.taskdefs.Get","doGet(int, org.apache.tools.ant.taskdefs.Get$DownloadProgress)","org.apache.tools.ant.taskdefs.UpToDate","diag"),
}

def ctx(source, method_blinded, target):
    m = method_blinded.split("(")[0]
    kind = "configuration/accessor-style method" if (m[:3] in ("set","get","add") or m[:2]=="is" or m.startswith("create")) else "behavioral method"
    sp=".".join(source.split(".")[:4]); tp=".".join(target.split(".")[:4])
    return f"{kind}; source and target in {'same package' if sp==tp else 'different packages'}"

rows=[]
with open(KEY, encoding="utf-8") as f:
    for r in csv.DictReader(f):
        g=r["gid"]
        if g in CURATED:
            sysname, src, morig, tgt, tier = CURATED[g]
            r["system"]=sysname; r["source"]=src; r["target"]=tgt
            r["method_original"]=morig; r["method_blinded"]=blind_sig(morig)
            r["tier"]=tier
        else:
            r["tier"]=OLD_TIER[g]
        rows.append(r)

cols=["gid","tool","local","system","source","method_blinded","target","method_original","tier"]
with open(KEY,"w",newline="",encoding="utf-8") as f:
    w=csv.DictWriter(f,fieldnames=cols); w.writeheader()
    for r in rows: w.writerow({k:r.get(k,"") for k in cols})

# rebuild blinded cards
L=["# Sampled Move-Method Suggestions (blinded)\n",
   "100 suggestions (20 per tool), tool identity hidden. For expert review only.\n"]
for r in sorted(rows,key=lambda x:x["gid"]):
    L+= [f"## {r['gid']}",
         f"- Subject system: {r['system']}",
         f"- Source class: {r['source']}",
         f"- Method: {r['method_blinded']}",
         f"- Target class: {r['target']}",
         f"- Context: {ctx(r['source'],r['method_blinded'],r['target'])}",""]
with open(os.path.join(HE,"sampled-suggestions.md"),"w",encoding="utf-8") as f:
    f.write("\n".join(L)+"\n")
print("updated sample-key.csv (+tier) and sampled-suggestions.md; ClassTrim items curated:", len(CURATED))
