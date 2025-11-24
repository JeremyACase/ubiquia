import * as vscode from "vscode";
import * as path from "path";
import * as http from "http";
import * as https from "https";

/**
 * Ubiquia VS Code Extension
 * --------------------------
 * This extension includes:
 *  - "Ubiquia: Install Ubiquia" (stub command)
 *  - "Ubiquia: Show DAG Visualizer" (Mermaid-based DAG viewer)
 *
 * Mermaid is bundled directly with the extension (node_modules/mermaid),
 * so no external download is required by the user.
 *
 * The DAG visualizer attempts to fetch a Mermaid DAG definition from
 * a running Ubiquia instance, then renders it.
 */

const DEFAULT_UBIQUIA_DAG_URL =
  "http://localhost:8080/ubiquia/flow-service/dag/mermaid";

export function activate(context: vscode.ExtensionContext) {
  console.log("Ubiquia extension is now active.");

  // -------------------------------------------------------------------------
  // Command: Ubiquia Install (Stub)
  // -------------------------------------------------------------------------
  const installCommand = vscode.commands.registerCommand(
    "ubiquia.install",
    async () => {
      const choice = await vscode.window.showInformationMessage(
        "Ubiquia installer stub.\n\nIn the future, this command will install or connect to Ubiquia.",
        "OK"
      );

      if (choice === "OK") {
        // Placeholder for future logic:
        // - Docker/K8s install
        // - Call a local Ubiquia HTTP endpoint
        // - Generate configuration files, etc.
      }
    }
  );

  // -------------------------------------------------------------------------
  // Command: Ubiquia Show DAG Visualizer
  // -------------------------------------------------------------------------
  const visualizeDagCommand = vscode.commands.registerCommand(
    "ubiquia.visualizeDag",
    async () => {
      // Optionally, you could later pull this from a configuration setting:
      // const config = vscode.workspace.getConfiguration("ubiquia");
      // const dagUrl = config.get<string>("dagUrl") ?? DEFAULT_UBIQUIA_DAG_URL;
      const dagUrl = DEFAULT_UBIQUIA_DAG_URL;

      let dagDefinition: string;

      try {
        dagDefinition = await vscode.window.withProgress(
          {
            location: vscode.ProgressLocation.Notification,
            title: "Fetching DAG from Ubiquia...",
            cancellable: false
          },
          async () => {
            return await fetchDagFromUbiquia(dagUrl);
          }
        );
      } catch (err: any) {
        console.error("Failed to fetch DAG from Ubiquia:", err);
        vscode.window.showErrorMessage(
          `Failed to fetch DAG from Ubiquia: ${err?.message ?? String(err)}`
        );

        // Fallback to a simple placeholder DAG so the view still works.
        dagDefinition = `
          graph TD
            A[Agent A] --> B[Agent B]
            B --> C[Belief State]
        `;
      }

      const panel = vscode.window.createWebviewPanel(
        "ubiquiaDag",
        "Ubiquia DAG Visualizer",
        vscode.ViewColumn.One,
        {
          enableScripts: true,
          retainContextWhenHidden: true,
          localResourceRoots: [
            vscode.Uri.file(path.join(context.extensionPath, "node_modules"))
          ]
        }
      );

      panel.webview.html = getWebviewContent(
        context,
        panel.webview,
        dagDefinition
      );
    }
  );

  // Register all commands
  context.subscriptions.push(installCommand, visualizeDagCommand);
}

/**
 * Fetches the DAG (Mermaid definition) from a Ubiquia HTTP endpoint.
 * Assumes the endpoint returns plain text containing a Mermaid diagram, e.g.:
 *
 *   graph TD
 *     A --> B
 *     B --> C
 */
async function fetchDagFromUbiquia(urlString: string): Promise<string> {
  const url = new URL(urlString);

  return new Promise<string>((resolve, reject) => {
    const isHttps = url.protocol === "https:";
    const lib = isHttps ? https : http;

    const req = lib.request(
      {
        hostname: url.hostname,
        port: url.port ? Number(url.port) : isHttps ? 443 : 80,
        path: url.pathname + url.search,
        method: "GET"
      },
      (res) => {
        if (res.statusCode && res.statusCode >= 400) {
          reject(
            new Error(
              `Ubiquia DAG endpoint responded with status ${res.statusCode}`
            )
          );
          return;
        }

        const chunks: Uint8Array[] = [];
        res.on("data", (chunk) => chunks.push(chunk));
        res.on("end", () => {
          const body = Buffer.concat(chunks).toString("utf8");
          resolve(body.trim());
        });
      }
    );

    req.on("error", (err) => reject(err));
    req.end();
  });
}

/**
 * Constructs the Webview HTML that embeds Mermaid locally.
 */
function getWebviewContent(
  context: vscode.ExtensionContext,
  webview: vscode.Webview,
  dag: string
): string {
  // Local Mermaid file path bundled in the extension
  const mermaidPath = vscode.Uri.file(
    path.join(
      context.extensionPath,
      "node_modules",
      "mermaid",
      "dist",
      "mermaid.min.js"
    )
  );
  const mermaidUri = webview.asWebviewUri(mermaidPath);

  const nonce = getNonce();

  return /* html */ `
    <!DOCTYPE html>
    <html lang="en">
      <head>
        <meta charset="UTF-8" />
        <meta http-equiv="Content-Security-Policy"
              content="default-src 'none'; script-src 'nonce-${nonce}'; style-src 'unsafe-inline'; img-src data: blob:;">
        <meta name="viewport" content="width=device-width, initial-scale=1.0" />
        <title>Ubiquia DAG Visualizer</title>
        <style>
          body {
            background-color: #1e1e1e;
            color: #eee;
            padding: 16px;
            font-family: sans-serif;
          }
          h2 {
            color: #9cdcfe;
            font-weight: 400;
          }
          .mermaid {
            margin-top: 1em;
            text-align: center;
          }
        </style>
      </head>
      <body>
        <h2>Ubiquia DAG Visualizer</h2>
        <div class="mermaid">
${dag}
        </div>
        <script nonce="${nonce}" src="${mermaidUri}"></script>
        <script nonce="${nonce}">
          mermaid.initialize({ startOnLoad: true, theme: "dark" });
        </script>
      </body>
    </html>
  `;
}

/**
 * Generates a random nonce to satisfy VS Code’s Content Security Policy (CSP).
 */
function getNonce(): string {
  const chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
  return Array.from({ length: 16 }, () =>
    chars.charAt(Math.floor(Math.random() * chars.length))
  ).join("");
}

/**
 * Called when the extension is deactivated.
 */
export function deactivate() {
  // Cleanup logic if needed
}
