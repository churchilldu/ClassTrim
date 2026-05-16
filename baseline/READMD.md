# Dataset: Move-Method Refactoring Baseline Comparison

## Overview

This directory contains raw experimental data used for a comparative evaluation of four baseline move-method refactoring tools (HMove, JDeodorant, JMove, and REsolution). Each top-level subfolder corresponds to one baseline tool. The dataset records the tools' suggested move method refactorings for multiple subject software systems and the effect of applying those suggestions on a predefined metric threshold.

## Directory structure

- At the repository root: a spreadsheet named `baseline.xlsx` provides an aggregate summary across all baseline tools.
- For each baseline tool (tool folder):
	- A main TSV file, named identically to the tool, that summarizes results per subject system; specifically, for each system it reports the number of classes that remain above the configured threshold after applying the tool's suggested refactorings.
	- One subfolder per subject software system. Each subject folder typically contains:
		- A screenshot set documenting the usage of the tool (when a graphical user interface is available), which records the procedure used to obtain suggestions.
		- A TSV file that enumerates the move method refactoring suggestions produced for that system. These per-system tables list individual suggestions (e.g., source class, target class, method identifier) and any associated notes or metrics reported by the tool.
