# ClassTrim MCP Server

A [Model Context Protocol](https://modelcontextprotocol.io/) server that exposes ClassTrim's
move-method refactoring engine to AI coding assistants over JSON-RPC via stdio.

## Prerequisites

- JDK 17+
- Maven 3.8+

## Build

```bash
# From the repo root
mvn -pl classtrim-mcp -am package -DskipTests
```

Output: `classtrim-mcp/target/classtrim-mcp-1.0-SNAPSHOT.jar` (fat JAR, ~45 MB)

## Tools

| Tool | Description |
|---|---|
| `analyze_project` | Run NSGA-III/II optimization on compiled .class files, returns move-method suggestions |
| `get_metrics` | Parse classes and return per-class WMC/CBO/RFC metrics without running optimization |
| `list_algorithms` | List available algorithms (NSGA-III, NSGA-II) |

## Configuration

### Kiro

Create or edit `.kiro/settings/mcp.json` in your workspace (or `~/.kiro/settings/mcp.json` for global):

```json
{
  "mcpServers": {
    "classtrim": {
      "command": "C:\\Program Files\\Eclipse Adoptium\\jdk-17.0.12.7-hotspot\\bin\\java.exe",
      "args": ["-jar", "C:\\path\\to\\classtrim-mcp-1.0-SNAPSHOT.jar"],
      "autoApprove": ["analyze_project", "get_metrics", "list_algorithms"]
    }
  }
}
```

### VS Code (with Claude / Copilot MCP extension)

Create or edit `.vscode/mcp.json` in your workspace:

```json
{
  "mcpServers": {
    "classtrim": {
      "command": "java",
      "args": ["-jar", "/path/to/classtrim-mcp-1.0-SNAPSHOT.jar"],
      "env": {
        "JAVA_HOME": "/path/to/jdk-17"
      }
    }
  }
}
```

> Note: Make sure `java` on your PATH is JDK 17+. If not, use the full path to the JDK 17 `java` executable in the `command` field.

### IntelliJ IDEA (with AI Assistant MCP support)

IntelliJ IDEA 2025.1+ supports MCP servers. Configure in **Settings → Tools → AI Assistant → MCP Servers**:

- **Name:** ClassTrim
- **Command:** `java`
- **Arguments:** `-jar /path/to/classtrim-mcp-1.0-SNAPSHOT.jar`
- **Working directory:** (your project root)

Or add to `.idea/mcp.json`:

```json
{
  "mcpServers": {
    "classtrim": {
      "command": "java",
      "args": ["-jar", "/path/to/classtrim-mcp-1.0-SNAPSHOT.jar"]
    }
  }
}
```

### Claude Desktop

Edit `claude_desktop_config.json` (usually at `%APPDATA%\Claude\claude_desktop_config.json` on Windows or `~/Library/Application Support/Claude/claude_desktop_config.json` on macOS):

```json
{
  "mcpServers": {
    "classtrim": {
      "command": "C:\\Program Files\\Eclipse Adoptium\\jdk-17.0.12.7-hotspot\\bin\\java.exe",
      "args": ["-jar", "C:\\path\\to\\classtrim-mcp-1.0-SNAPSHOT.jar"]
    }
  }
}
```

## Usage Examples

Once configured, ask your AI assistant:

- "Analyze smelly-demo for move-method opportunities"
- "Which classes in my project exceed WMC threshold 8?"
- "What algorithms are available for refactoring?"
- "Run NSGA-II with population 200 and 1000 iterations on target/classes"

The assistant will call the appropriate tool and present structured results.

## Example: analyze_project

**Input:**
```json
{
  "roots": ["C:/myproject/target/classes"],
  "wmc": 8,
  "cbo": 8,
  "rfc": 30,
  "populationSize": 500,
  "maxIterations": 2000,
  "algorithm": "NSGA-III",
  "useGuidingObjectives": true
}
```

**Output:**
```json
{
  "suggestions": [
    {
      "method": "chargeCustomer(Order)",
      "sourceClass": "org.example.OrderProcessor",
      "targetClass": "org.example.Order"
    }
  ],
  "computingTimeMs": 4523,
  "classCount": 8,
  "methodCount": 32,
  "algorithm": "NSGA-III"
}
```

## Debugging

The server redirects stderr to a temp file to avoid corrupting the JSON-RPC stream.
Check `%TEMP%/classtrim-mcp-stderr.log` (or `/tmp/classtrim-mcp-stderr.log`) for
SLF4J/JMetal output if something goes wrong.

## Manual Testing

```bash
echo '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}' | java -jar classtrim-mcp/target/classtrim-mcp-1.0-SNAPSHOT.jar
```

Expected response:
```json
{"jsonrpc":"2.0","id":1,"result":{"capabilities":{"tools":{}},"protocolVersion":"2024-11-05","serverInfo":{"name":"classtrim-mcp","version":"1.0.0"}}}
```
