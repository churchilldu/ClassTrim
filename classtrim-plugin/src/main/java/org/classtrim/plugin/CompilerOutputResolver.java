package org.classtrim.plugin;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.roots.CompilerProjectExtension;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Predicate;

/**
 * Resolves the {@code Compiler_Output_Roots} for an Analysis_Run from raw IntelliJ
 * inputs.
 *
 * <p>This class is split into two layers:</p>
 * <ul>
 *   <li>A <strong>pure</strong> layer ({@link #resolveFromInputs}) that takes plain
 *       strings plus an injected filesystem-existence predicate. It is straightforward
 *       to property-test without spinning up an IntelliJ test fixture.</li>
 *   <li>An <strong>IntelliJ-facing</strong> layer ({@code resolve(Project)}, added in
 *       task 2.3) that extracts the project-level compiler-output URL and per-module
 *       production output paths and forwards them into the pure layer with
 *       {@code Files::exists} as the predicate.</li>
 * </ul>
 *
 * <p>The pure layer never throws. Every external call (URL → path conversion, the
 * existence predicate, {@link Path#of}) is wrapped so that even pathological inputs —
 * malformed paths, predicates that throw, embedded NUL bytes — produce an empty or
 * partial result instead of propagating an exception.</p>
 *
 * <p>Validates: Requirements 2.1, 2.2, 2.3, 2.4.</p>
 */
public final class CompilerOutputResolver {

    private CompilerOutputResolver() {
        // static helper
    }

    /**
     * IntelliJ-facing entry point. Extracts the project-level compiler output URL and
     * each module's production compiler output filesystem path from the open
     * {@link Project} and forwards them into {@link #resolveFromInputs} with
     * {@link Files#exists(Path, java.nio.file.LinkOption...)} as the existence
     * predicate.
     *
     * <p>Both extension calls are null-tolerant: a missing
     * {@link CompilerProjectExtension}, a missing {@link CompilerModuleExtension},
     * a {@code null} URL, and a {@code null} {@link VirtualFile} per module all
     * collapse to a {@code null} entry that the pure layer treats as
     * "no value provided" (Requirements 2.2).</p>
     *
     * <p>The existence predicate wraps {@code Files.exists(Paths.get(...))} in a
     * {@code try/catch} so that an {@link java.nio.file.InvalidPathException} from a
     * pathological path string (for example, one with embedded NUL bytes) is
     * reported as "does not exist" instead of propagating to the caller. The pure
     * layer already promises never to throw; this wrapper preserves that
     * guarantee.</p>
     *
     * @param project the open IDE_Project
     * @return the deduplicated, existence-filtered list of compiler-output roots
     * @see #resolveFromInputs(String, List, String, Predicate)
     */
    public static List<String> resolve(Project project) {
        String projectOutputUrl;
        try {
            CompilerProjectExtension projectExtension =
                    CompilerProjectExtension.getInstance(project);
            projectOutputUrl = projectExtension == null
                    ? null
                    : projectExtension.getCompilerOutputUrl();
        } catch (Throwable t) {
            projectOutputUrl = null;
        }

        Module[] modules = ModuleManager.getInstance(project).getModules();
        List<String> moduleOutputPaths = new ArrayList<>(modules.length);
        for (Module module : modules) {
            moduleOutputPaths.add(extractModuleOutputPath(module));
        }

        String basePath;
        try {
            basePath = project.getBasePath();
        } catch (Throwable t) {
            basePath = null;
        }

        Predicate<String> existsOnDisk = path -> {
            try {
                return Files.exists(Paths.get(path));
            } catch (Throwable t) {
                return false;
            }
        };

        return resolveFromInputs(projectOutputUrl, moduleOutputPaths, basePath, existsOnDisk);
    }

    /**
     * Module-scoped IntelliJ-facing entry point. Resolves the production
     * compiler-output path for a single {@link Module} and returns the
     * deduplicated, existence-filtered list — which in practice is at most
     * one entry, since a module has a single production output directory.
     *
     * <p>Used by the right-click "Run ClassTrim Analysis" action on a module
     * in the Project view (or on any file/folder inside a module) so the
     * developer can target one module instead of the entire project. The
     * project-level compiler-output URL and the {@code <basePath>/target/classes}
     * fallback are intentionally <em>not</em> consulted here: a per-module
     * invocation must include only that module's output, otherwise the
     * "scope to module" promise leaks to peer modules.</p>
     *
     * @param module the {@link Module} to scope the analysis to
     * @return a list of compiler-output roots for this module, possibly empty
     */
    public static List<String> resolve(Module module) {
        String modulePath = extractModuleOutputPath(module);
        Predicate<String> existsOnDisk = path -> {
            try {
                return Files.exists(Paths.get(path));
            } catch (Throwable t) {
                return false;
            }
        };
        // Reuse the pure layer with a single-element module list and no project
        // URL / base path so the fallback branch is never eligible.
        return resolveFromInputs(null, List.of(modulePath == null ? "" : modulePath),
                null, existsOnDisk);
    }

    /**
     * Reads a module's production compiler-output path through
     * {@link CompilerModuleExtension}, swallowing any exception so a single
     * misbehaving module never breaks resolution.
     *
     * @param module the {@link Module} to inspect
     * @return the filesystem path string, or {@code null} when no output is
     *         configured or extraction fails
     */
    private static String extractModuleOutputPath(Module module) {
        try {
            CompilerModuleExtension moduleExtension =
                    CompilerModuleExtension.getInstance(module);
            VirtualFile outputDir = moduleExtension == null
                    ? null
                    : moduleExtension.getCompilerOutputPath();
            return outputDir == null ? null : outputDir.getPath();
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Pure transformation from raw IntelliJ inputs to the deduplicated, existence-filtered
     * list of compiler-output roots.
     *
     * <p>Algorithm:</p>
     * <ol>
     *   <li>Track whether the project URL was provided (non-null and non-blank) and
     *       whether any module path was provided (any entry that is non-null and
     *       non-blank).</li>
     *   <li>If the project URL was provided, resolve it via
     *       {@link VfsUtilCore#urlToPath} and add the result when it is itself
     *       non-blank.</li>
     *   <li>For each module path in input order, skip null or blank entries (no error
     *       to the developer — Requirements 2.2) and add the rest.</li>
     *   <li>Drop any entry where {@code existsOnDisk.test(path)} returns {@code false}
     *       or throws (Requirements 2.4).</li>
     *   <li>If neither the project URL nor any module path was provided as a
     *       non-null/non-blank input <em>and</em> {@code projectBasePathOrNull} is
     *       non-null, attempt the fallback {@code <basePath>/target/classes} and add
     *       it iff the predicate accepts it (Requirements 2.3).</li>
     * </ol>
     *
     * <p>The returned list preserves first-seen discovery order across project URL →
     * modules → optional fallback, contains no duplicates, and contains no null or
     * blank entries.</p>
     *
     * @param projectOutputUrlOrNull        the project-level compiler output URL as
     *                                      reported by {@code CompilerProjectExtension}
     *                                      (may be {@code null} or blank)
     * @param moduleOutputPathsRawNullable  per-module production output filesystem
     *                                      paths in iteration order (the list itself
     *                                      and individual entries may be {@code null}
     *                                      or blank)
     * @param projectBasePathOrNull         project base path used to compute the
     *                                      {@code target/classes} fallback (may be
     *                                      {@code null})
     * @param existsOnDisk                  predicate that returns {@code true} when
     *                                      its argument resolves to an existing
     *                                      filesystem entry; may be {@code null} (in
     *                                      which case nothing is considered to exist)
     * @return an immutable list of compiler-output root paths, possibly empty, never
     *         {@code null}
     */
    public static List<String> resolveFromInputs(
            String projectOutputUrlOrNull,
            List<String> moduleOutputPathsRawNullable,
            String projectBasePathOrNull,
            Predicate<String> existsOnDisk) {

        LinkedHashSet<String> result = new LinkedHashSet<>();

        boolean projectUrlProvided =
                projectOutputUrlOrNull != null && !projectOutputUrlOrNull.isBlank();
        boolean anyModulePathProvided = false;

        // Step 2: project-level compiler output URL → filesystem path.
        if (projectUrlProvided) {
            try {
                String path = VfsUtilCore.urlToPath(projectOutputUrlOrNull);
                if (path != null && !path.isBlank()) {
                    result.add(path);
                }
            } catch (Throwable ignored) {
                // VfsUtilCore.urlToPath should not throw, but the contract here is
                // "never throw on any input", so we defensively swallow.
            }
        }

        // Step 3: module-level production output paths in iteration order.
        if (moduleOutputPathsRawNullable != null) {
            for (String modulePath : moduleOutputPathsRawNullable) {
                if (modulePath == null || modulePath.isBlank()) {
                    continue;
                }
                anyModulePathProvided = true;
                result.add(modulePath);
            }
        }

        // Step 4: drop entries that do not exist on disk.
        result.removeIf(path -> !safeExists(existsOnDisk, path));

        // Step 5: fallback to <basePath>/target/classes only when neither the project
        // URL nor any module path was provided as a non-null/non-blank input.
        boolean fallbackEligible = !projectUrlProvided && !anyModulePathProvided;
        if (fallbackEligible && projectBasePathOrNull != null) {
            String fallback = safeBuildFallback(projectBasePathOrNull);
            if (fallback != null && safeExists(existsOnDisk, fallback)) {
                result.add(fallback);
            }
        }

        return List.copyOf(result);
    }

    /**
     * Invokes the existence predicate without ever propagating an exception. A
     * {@code null} predicate, a predicate that throws, and a predicate that returns
     * {@code false} all collapse to {@code false}.
     */
    private static boolean safeExists(Predicate<String> predicate, String path) {
        if (predicate == null) {
            return false;
        }
        try {
            return predicate.test(path);
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Builds the {@code <basePath>/target/classes} fallback string, swallowing any
     * {@link java.nio.file.InvalidPathException} (or other failure) caused by an
     * exotic base-path value.
     *
     * @return the fallback path string, or {@code null} when the base path is not a
     *         valid filesystem path
     */
    private static String safeBuildFallback(String basePath) {
        try {
            return Path.of(basePath, "target", "classes").toString();
        } catch (Throwable t) {
            return null;
        }
    }
}
