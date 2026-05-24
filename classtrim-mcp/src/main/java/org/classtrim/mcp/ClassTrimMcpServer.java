package org.classtrim.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.classtrim.common.Threshold;
import org.classtrim.core.analyzer.StandardProjectAnalyzer;
import org.classtrim.core.config.RefactoringConfig;
import org.classtrim.core.engine.AlgorithmType;
import org.classtrim.core.engine.RefactoringEngine;
import org.classtrim.core.engine.RefactoringResult;
import org.classtrim.core.engine.RefactoringSuggestion;
import org.classtrim.core.model.BinaryPathProjectSource;
import org.classtrim.core.repository.InMemoryProjectRepository;
import org.classtrim.core.service.ClassTrimService;
import org.classtrim.model.JavaClass;
import org.classtrim.model.JavaMethod;
import org.classtrim.model.JavaProject;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ClassTrim MCP Server — exposes move-method refactoring analysis over
 * JSON-RPC via stdio using the Model Context Protocol.
 *
 * <p>This is a minimal stdio-based MCP server that reads JSON-RPC requests
 * from stdin and writes responses to stdout. It implements the MCP protocol
 * directly (initialize, tools/list, tools/call) without depending on the
 * full MCP SDK to keep the dependency footprint minimal and avoid version
 * conflicts with classtrim-core's transitive dependencies.</p>
 *
 * <p>Tools exposed:</p>
 * <ul>
 *   <li>{@code analyze_project} — run NSGA-III/II optimization</li>
 *   <li>{@code get_metrics} — inspect per-class WMC/CBO/RFC</li>
 *   <li>{@code list_algorithms} — enumerate available algorithms</li>
 * </ul>
 */
public class ClassTrimMcpServer {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String SERVER_NAME = "classtrim-mcp";
    private static final String SERVER_VERSION = "1.0.0";

    public static void main(String[] args) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        PrintWriter writer = new PrintWriter(new BufferedOutputStream(System.out), true);

        // Redirect stderr so SLF4J/JMetal output doesn't corrupt the JSON-RPC stream.
        System.setErr(new PrintStream(new FileOutputStream(
                new File(System.getProperty("java.io.tmpdir"), "classtrim-mcp-stderr.log"))));

        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;

            try {
                Map<String, Object> request = MAPPER.readValue(line, Map.class);
                Map<String, Object> response = handleRequest(request);
                writer.println(MAPPER.writeValueAsString(response));
            } catch (Exception e) {
                Map<String, Object> error = makeError(null, -32700, "Parse error: " + e.getMessage());
                writer.println(MAPPER.writeValueAsString(error));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> handleRequest(Map<String, Object> request) {
        Object id = request.get("id");
        String method = (String) request.get("method");
        Map<String, Object> params = (Map<String, Object>) request.getOrDefault("params", Map.of());

        if (method == null) {
            return makeError(id, -32600, "Invalid request: missing method");
        }

        try {
            Object result = switch (method) {
                case "initialize" -> handleInitialize(params);
                case "notifications/initialized" -> null; // no response needed
                case "tools/list" -> handleToolsList();
                case "tools/call" -> handleToolsCall(params);
                default -> throw new IllegalArgumentException("Unknown method: " + method);
            };

            if (result == null && "notifications/initialized".equals(method)) {
                return null; // notifications don't get responses
            }

            return makeResult(id, result);
        } catch (Exception e) {
            return makeError(id, -32603, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private static Map<String, Object> handleInitialize(Map<String, Object> params) {
        return Map.of(
                "protocolVersion", "2024-11-05",
                "capabilities", Map.of("tools", Map.of()),
                "serverInfo", Map.of("name", SERVER_NAME, "version", SERVER_VERSION)
        );
    }

    private static Map<String, Object> handleToolsList() {
        List<Map<String, Object>> tools = List.of(
                makeToolDef("analyze_project",
                        "Run NSGA-III/II move-method refactoring analysis on compiled Java classes",
                        Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "roots", Map.of("type", "array", "items", Map.of("type", "string"),
                                                "description", "Absolute paths to directories containing .class files"),
                                        "projectName", Map.of("type", "string", "default", "project"),
                                        "wmc", Map.of("type", "integer", "default", 8),
                                        "cbo", Map.of("type", "integer", "default", 8),
                                        "rfc", Map.of("type", "integer", "default", 30),
                                        "populationSize", Map.of("type", "integer", "default", 500),
                                        "maxIterations", Map.of("type", "integer", "default", 2000),
                                        "algorithm", Map.of("type", "string", "default", "NSGA-III"),
                                        "useGuidingObjectives", Map.of("type", "boolean", "default", true)
                                ),
                                "required", List.of("roots")
                        )),
                makeToolDef("get_metrics",
                        "Parse compiled classes and return per-class WMC/CBO/RFC metrics without running optimization",
                        Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "roots", Map.of("type", "array", "items", Map.of("type", "string"),
                                                "description", "Paths to compiled .class directories"),
                                        "wmc", Map.of("type", "integer", "default", 8),
                                        "cbo", Map.of("type", "integer", "default", 8),
                                        "rfc", Map.of("type", "integer", "default", 30)
                                ),
                                "required", List.of("roots")
                        )),
                makeToolDef("list_algorithms",
                        "List available optimization algorithms",
                        Map.of("type", "object", "properties", Map.of()))
        );
        return Map.of("tools", tools);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> handleToolsCall(Map<String, Object> params) {
        String toolName = (String) params.get("name");
        Map<String, Object> args = (Map<String, Object>) params.getOrDefault("arguments", Map.of());

        Object content = switch (toolName) {
            case "analyze_project" -> analyzeProject(args);
            case "get_metrics" -> getMetrics(args);
            case "list_algorithms" -> listAlgorithms();
            default -> throw new IllegalArgumentException("Unknown tool: " + toolName);
        };

        return Map.of("content", List.of(Map.of("type", "text", "text", toJson(content))));
    }

    // =========================================================================
    // Tool implementations
    // =========================================================================

    @SuppressWarnings("unchecked")
    private static Map<String, Object> analyzeProject(Map<String, Object> args) {
        List<String> roots = (List<String>) args.get("roots");
        String projectName = (String) args.getOrDefault("projectName", "project");
        int wmc = toInt(args.getOrDefault("wmc", 8));
        int cbo = toInt(args.getOrDefault("cbo", 8));
        int rfc = toInt(args.getOrDefault("rfc", 30));
        int populationSize = toInt(args.getOrDefault("populationSize", 500));
        int maxIterations = toInt(args.getOrDefault("maxIterations", 2000));
        String algorithmName = (String) args.getOrDefault("algorithm", "NSGA-III");
        boolean useGuiding = (boolean) args.getOrDefault("useGuidingObjectives", true);

        Threshold threshold = new Threshold(wmc, cbo, rfc);
        RefactoringConfig config = new RefactoringConfig(threshold, populationSize, maxIterations, useGuiding);
        BinaryPathProjectSource source = new BinaryPathProjectSource(projectName, roots, threshold);

        AlgorithmType algorithmType = AlgorithmType.fromDisplayName(algorithmName);
        RefactoringEngine engine = algorithmType.createEngine();
        InMemoryProjectRepository repository = new InMemoryProjectRepository();
        StandardProjectAnalyzer analyzer = new StandardProjectAnalyzer(repository);
        ClassTrimService service = new ClassTrimService(analyzer, engine);

        RefactoringResult result = service.analyze(source, config);

        List<Map<String, String>> suggestions = result.getSuggestions().stream()
                .map(s -> Map.of(
                        "method", s.getMethod().toString(),
                        "sourceClass", s.getSourceClass().toString(),
                        "targetClass", s.getTargetClass().toString()
                ))
                .collect(Collectors.toList());

        JavaProject project = result.getProject();
        return Map.of(
                "suggestions", suggestions,
                "computingTimeMs", result.getComputingTimeMs(),
                "classCount", project.getClassCanRefactor().size(),
                "methodCount", project.getMethodsCanRefactor().size(),
                "algorithm", algorithmType.getDisplayName()
        );
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getMetrics(Map<String, Object> args) {
        List<String> roots = (List<String>) args.get("roots");
        int wmcThreshold = toInt(args.getOrDefault("wmc", 8));
        int cboThreshold = toInt(args.getOrDefault("cbo", 8));
        int rfcThreshold = toInt(args.getOrDefault("rfc", 30));

        Threshold threshold = new Threshold(wmcThreshold, cboThreshold, rfcThreshold);
        BinaryPathProjectSource source = new BinaryPathProjectSource("metrics", roots, threshold);

        InMemoryProjectRepository repository = new InMemoryProjectRepository();
        StandardProjectAnalyzer analyzer = new StandardProjectAnalyzer(repository);
        JavaProject project = analyzer.analyze(source);

        List<Map<String, Object>> classes = new ArrayList<>();
        int exceedingCount = 0;

        for (JavaClass clazz : project.getClassCanRefactor()) {
            int wmc = clazz.getDeclaredMethods().size(); // WMC approximation
            int cbo = clazz.getFieldsType().size();      // CBO approximation
            int rfc = clazz.getDeclaredMethods().stream()
                    .mapToInt(m -> 1 + m.getInvokeMethods().size())
                    .sum();

            boolean exceedsWmc = wmc > wmcThreshold;
            boolean exceedsCbo = cbo > cboThreshold;
            boolean exceedsRfc = rfc > rfcThreshold;
            boolean exceeds = exceedsWmc || exceedsCbo || exceedsRfc;
            if (exceeds) exceedingCount++;

            long refactorableCount = clazz.getDeclaredMethods().stream()
                    .filter(JavaMethod::canRefactor).count();

            classes.add(Map.of(
                    "name", clazz.toString(),
                    "wmc", wmc,
                    "cbo", cbo,
                    "rfc", rfc,
                    "exceedsWmc", exceedsWmc,
                    "exceedsCbo", exceedsCbo,
                    "exceedsRfc", exceedsRfc,
                    "methodCount", clazz.getDeclaredMethods().size(),
                    "refactorableMethodCount", refactorableCount
            ));
        }

        return Map.of(
                "classes", classes,
                "totalClasses", project.getClassCanRefactor().size(),
                "classesExceedingThreshold", exceedingCount
        );
    }

    private static Map<String, Object> listAlgorithms() {
        List<Map<String, String>> algorithms = Arrays.stream(AlgorithmType.values())
                .map(t -> Map.of("name", t.getDisplayName(), "description", t.name()))
                .collect(Collectors.toList());
        return Map.of(
                "algorithms", algorithms,
                "default", AlgorithmType.NSGA_III.getDisplayName()
        );
    }

    // =========================================================================
    // JSON-RPC helpers
    // =========================================================================

    private static Map<String, Object> makeResult(Object id, Object result) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.put("result", result);
        return response;
    }

    private static Map<String, Object> makeError(Object id, int code, String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.put("error", Map.of("code", code, "message", message));
        return response;
    }

    private static Map<String, Object> makeToolDef(String name, String description, Map<String, Object> inputSchema) {
        return Map.of("name", name, "description", description, "inputSchema", inputSchema);
    }

    private static String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    private static int toInt(Object value) {
        if (value instanceof Number) return ((Number) value).intValue();
        return Integer.parseInt(value.toString());
    }
}
