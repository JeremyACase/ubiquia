# Multi-agent: Yugabyte Unidirectional xCluster 
This document shows how to run a unidirectional yugabyteDB setup. It assumes Ubiquia agents running, each with their own yugabyteDB.

## Document Version
- **Version:** 0.1
- **Date:** 2026-02-23
- **Author:** Jeremy Case

* [Setup](#setup)
  * [High-Level Architecture Diagram](#high-level-architecture-diagram)
  * [Components](#components)
  * [Message Broker](#message-broker)
  * [Databases](#databases)
  * [Observability](#observability)
  * [Deployment](#deployment)


## Setup

At least two Ubiquia agents must be run. For the purposes of this demo, there is a a script that can be run, as well as several specific configurations.

---

### Firewall Configuration
For my LAN, I had to open up the IP address and ports via my Windows firewall settings.

```powershell
> New-NetFirewallRule -DisplayName "Yugabyte 5433" -Direction Inbound -Action Allow -Protocol TCP -LocalPort 5433
```

### YugabyteDB Configuration
The "producer" cluster needs to be configured to broadcast at a reachable IP address. As of YugabyteDB version 2025.2.0, this can be done via Helm. In the values configuration, the producer needs to be configured to broadcast on reachable IP addresses. 

```yaml
yugabyte:
  master:
    serverBroadcastAddress: "192.168.200.56"

  tserver:
    serverBroadcastAddress: "192.168.200.56"
```

### Installation Script 

The installation script can be found and run at. **It assumes all microservices have already been built successfully locally via the usual Gradle build**. 
```bash
$ ./tools/scripts/devs/multi-agent/install-ubiquia-into-kind-full-local-mas.sh
```

This script should be run in both agents. 

### YugabyteDB Tables
The producer and consumer need to be the same YugabyteDB versions and have set up the database with the same schemas in order for them to synchronize. Then the hex addresses of the tables need to be copied so that the consumer can ingest from them.

We need to get the hex addresses of the tables from the producer by shelling into a pod and querying them from yugabytedb.

```bash
$ kubectl exec --stdin -it yb-master-0 -n ubiquia -- /bin/bash
```

Now that we're shelled in, we need to query the tables.
```bash
$ yb-admin --master_addresses=yb-masters:7100 list_tables
```

This will output all of the tables from yugabyte. We only need the hex addresses of the tables we wish to synchronize. 
```console
yugabyte.abstract_model_entity [ysql_schema=public] [000034d4000030008000000000004000]
yugabyte.abstract_model_entity_tags [ysql_schema=public] [000034d4000030008000000000004005]
yugabyte.agent_deployed_graphs [ysql_schema=public] [000034d4000030008000000000004008]
yugabyte.agent_entity [ysql_schema=public] [000034d400003000800000000000400b]
yugabyte.component_entity [ysql_schema=public] [000034d4000030008000000000004015]
yugabyte.component_entity_environment_variables [ysql_schema=public] [000034d400003000800000000000401b]
yugabyte.component_entity_override_settings [ysql_schema=public] [000034d400003000800000000000401e]
yugabyte.component_entity_volumes [ysql_schema=public] [000034d4000030008000000000004021]
yugabyte.component_post_start_exec_commands [ysql_schema=public] [000034d4000030008000000000004010]
yugabyte.domain_data_contract_entity [ysql_schema=public] [000034d4000030008000000000004024]
yugabyte.domain_data_contract_entity_domain_ontology_contract_join_i_key [ysql_schema=public] [000034d4000030008000000000004029]
yugabyte.domain_ontology_entity [ysql_schema=public] [000034d400003000800000000000402b]
yugabyte.flow_entity [ysql_schema=public] [000034d4000030008000000000004030]
yugabyte.flow_event_entity [ysql_schema=public] [000034d4000030008000000000004035]
yugabyte.flow_event_entity_input_payload_stamps [ysql_schema=public] [000034d400003000800000000000403a]
yugabyte.flow_event_entity_output_payload_stamps [ysql_schema=public] [000034d400003000800000000000403d]
yugabyte.flow_message_entity [ysql_schema=public] [000034d4000030008000000000004040]
yugabyte.graph_entity [ysql_schema=public] [000034d4000030008000000000004045]
yugabyte.node_entity [ysql_schema=public] [000034d400003000800000000000404a]
yugabyte.node_entity_component_node_join_id_key [ysql_schema=public] [000034d4000030008000000000004053]
yugabyte.node_entity_input_sub_schemas [ysql_schema=public] [000034d4000030008000000000004055]
yugabyte.node_entity_override_settings [ysql_schema=public] [000034d4000030008000000000004058]
yugabyte.object_metadata_entity [ysql_schema=public] [000034d400003000800000000000405b]
```

### Producer replication
We need to shell into a pod on the consumer Ubiquia agent and then configure Yugabyte to synchronize with the producer.

```bash 
$ kubectl exec --stdin -it yb-master-0 -n ubiquia -- /bin/bash
```

Once shelled in, we need to "point" our consumer to our producer
```bash
$ yb-admin --master_addresses=yb-masters:7100 setup_universe_replication standby_replication 192.168.200.56:7100 <csv list of tables to be replicated>
```

If successful, you should see the following
```console
> Replication setup successfully
```

### Results
Once this is set up, any data that flows into the producer should flow into the consumer, but not vice versa. Hence, a unidirectional flow of data. This is useful especially in an architecture with a single cluster that receives reads and several clusters that receive writes (such as might be useful with load balancing.)

## Contributors
* __Jeremy Case__: jeremycase@odysseyconsult.com