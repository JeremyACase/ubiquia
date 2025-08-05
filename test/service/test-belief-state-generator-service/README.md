# 🧪 `belief-state-helm-test-service`

> **A Helm-native test harness for validating belief state generation and lifecycle within Ubiquia.**

The `belief-state-helm-test-service` is a **test pod** used in Helm deployments to verify that the `belief-state-generator-service` is fully functional in a Kubernetes environment. It simulates a complete lifecycle of a belief state: generation, deployment, POSTing data, querying it, and then cleaning it up.

This service is intended to be run as a **Helm test hook**, returning a pass/fail signal to Helm charts depending on whether belief states can be successfully generated and used in-cluster.

---

## ✅ Test Goals

- 🛠️ Confirm that the generator service can:
  - Generate belief state services from a test schema
  - Dynamically compile and register endpoints
  - Expose the new API via REST
  - Accept POSTed payloads and return them via GET and query
  - Tear down deployed belief states without error

- 📦 Ensure the Ubiquia platform can correctly handle belief state lifecycle at runtime

---

## ⚙️ How It Works

When run, the Helm test pod:

1. 🧬 Registers a test schema with the `belief-state-generator-service`
2. 🚀 Triggers generation and waits for the new service to become live
3. 📤 Sends a test payload to the new belief state via POST
4. 🔍 Executes a query to verify data integrity and correct indexing
5. 📥 Fetches data via GET to validate ID resolution
6. 🧹 Tears down the test belief state via DELETE
7. ✅ Returns `exit 0` on success or `exit 1` on failure

All actions are logged and validated against expected JSON responses.

---

## 🧪 Running the Helm Test

The test pod is automatically triggered via:

```bash
helm test <release-name>
```

You can also run the test manually for debugging:

```bash
kubectl run belief-state-helm-test   --rm -it   --restart=Never   --image=<your-registry>/belief-state-helm-test:latest
```

---

## 📂 Directory Structure

```text
src/main/java/org/ubiquia/test/beliefstate
├── BeliefStateTestApplication.java    # Entry point for Helm test
├── client/                            # REST clients for generator and generated belief state
├── payload/                           # Static test schema and test data
├── logic/                             # Step-wise lifecycle validation logic
├── config/                            # Spring Boot test config and endpoints
├── Dockerfile                         # Container image for test pod
├── application.yaml                   # Test runtime properties
└── build.gradle                       # Minimal build file for test binary
```

---

## 🌐 Example: Test Schema and Flow

```json
{
  "domain": "test",
  "version": { "major": 0, "minor": 1, "patch": 0 },
  "modelType": "AgentCommunicationLanguage",
  "jsonSchema": {
    "type": "object",
    "properties": {
      "message": { "type": "string" },
      "timestamp": { "type": "string", "format": "date-time" }
    },
    "required": ["message"]
  }
}
```

The test payload:

```json
{
  "message": "Hello from Helm test!",
  "timestamp": "2025-01-01T12:00:00Z"
}
```

---

## 🔐 Kubernetes RBAC

The test pod requires the ability to:

- POST to `belief-state-generator-service`
- Access the generated belief state endpoint
- Optionally: List and delete resources created via Kubernetes API (if testing teardown)

Make sure your test service account has the appropriate permissions via RoleBinding or ClusterRoleBinding.

---

## 📜 License

This module is part of Ubiquia and licensed under [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0).