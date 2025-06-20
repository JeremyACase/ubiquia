# Belief State Service Design Document
This is a design document for the Belief State.

## Document Version
- **Version:** 0.1
- **Date:** 2025-05-27
- **Author:** Jeremy Case
- **Reviewed by:** [TBD]

* [Overview](#overview)
* [Goals and Non-Goals](#goals-and-non-goals)


## Overview

The **Belief State Service** is a component within a **Ubiquia** deployment. It is intended to realize and distribute data at runtime when Ubiquia implements an Agent Communication Language. 

---

## Goals and Non-Goals

### Goals
- Enable runtime realization of Agent Communication Languages that can be translated into database schema in YugabyteDB
- Generate a dynamic RESTful API that can allow clients to interface with the generated database schemas 
- Allow data to propagate over a cluster (network) of Ubiquia agents allowing for location-transparency of data and consistency as communication.
- Allow the DBMS to handle cascading relationships so that developers need not write domain-specific application logic towards this end
- Leverage database schemas where appropriate to ensure the ability to automatically "evolve" and migrate database schemas. 


## Problem Statement
Ubiquia is a software development framework to enable Multi-Agent-Systems represented as Directed Acyclic Graphs. It must be able to realize Agent Communication Languages dynamically **a posteriori** to system design. Doing so, however, incurs a tradeoff between developer ease-of-use and data integrity (and who--or what--must ensure data integrity.)


### Unstructured data Example
Example: In a document-based database, a developer can easily POST the following JSON and persist it:

```json
{
  "id": "a5c2e1f3-9bd4-4f7a-a3e9-2b3e21c4bcb1",
  "modelType": "Sensor",
  "nameEntity": "Example Sensor",
  "status": "ACTIVE",
  "lastCalibrated": "2024-11-15T13:45:00Z",
  "location": {
    "id": "b9175ea2-e7c4-403e-8c87-6c77a06ebf94",
    "modelType": "Location",
    "nameEntity": "Data Center East Wing",
    "floor": 2,
    "building": "HQ"
  },
  "linkedSensor": {
    "id": "27adf1fa-73d2-4982-bd9e-50d8c1db6c42",
    "modelType": "Sensor",
    "nameEntity": "Another Example Sensor",
    "status": "STANDBY"
  }
}
```

This is maximally-easy for developers **with respect to ingress and egress**, but the data isn't normalized. It is, therefore, maximally-difficult for developers **with respect to maintaining application logic** (because they must now write code that interfaces with the database to ensure cascading updates/relationships are properly handled.) 

Consider what happens if the "Another Example Sensor" needs to be updated. In a relational database, this is trivial. Here, a developer must have logic that traverses the entire database looking for any potential (nested) references to this sensor and update accordingly. This leads to bloated application code, and all-but-guarantees data issues (data redundancy, data mismatches, etc.) 

### Structured data Example
Example: In a relational database, the JSON can be collapsed, assuming **foreign keys**.

```json
{
  "id": "a5c2e1f3-9bd4-4f7a-a3e9-2b3e21c4bcb1",
  "modelType": "Sensor",
  "nameEntity": "Example Sensor",
  "status": "ACTIVE",
  "lastCalibrated": "2024-11-15T13:45:00Z",
  "locationId": "b9175ea2-e7c4-403e-8c87-6c77a06ebf94",
  "linkedSensorId": "27adf1fa-73d2-4982-bd9e-50d8c1db6c42"
}
```
 **But this assumption can only be made because a schema has been generated and initialized for the database.** In other words, this approach optimizes for data integrity at the cost of requiring developers to define schemas (but at least they won't have to write application logic for updates!)

## Historical Context
There is some historical context here with MACHINA's belief state as we attempted to solve a problem (dynamic domain realization) that has never successfully been done before. We did some stuff right, and some stuff wrong. Let's break it down.

### What we got wrong
- **Hydration/Egress**: We tried solving every problem with the Belief State: allowing for devs to query deeply-nested objects back (if they wanted them)...or allowing them to optimally make multiple queries (rarely the case) and doing so via URL parameters
- **Ignore Tokens**: We tried allowing devs/clients to ignore certain fields of models when doing RESTful calls.
- **N+1**: The N+1 problem is always going to incur a performance hit unless specifically addressed; this is doubly-difficult with the way we were trying to egress models from the database **a posteriori** to domain design time.
- **Nested Models**: There is nothing inherently wrong with nested models; in fact, they're optimal with respect to data normalization. However, the problem is that we should not have tried egressing "nested formats" during REST calls (the team requested this early-on, in retrospect, it was a huge mistake on behalf of the lead dev to make it thus.)
- **Performance**: All of this combined meant it was too easy to allow devs to make queries that had huge performance implications.

### What we got right
- **Data-driven generation**: The ability to generate models from a data file (in MACHINA's case, a Swagger file) was largely successful. It need not take the same form in Ubiquia, but it should be the same "in spirit."
- **Query Params**: The MACHINA library's ability to do **a posteriori** queries based on parameters, typing, and polymoprhism was useful.
- **Ingress Response**: A simple response to POSTed data with a newly-generated UUID and metadata was the correct approach for the belief state; it also allows for simple publishing/subscribing of this data should it be needed.
- **a posteriori**: For better or for worse, we were able to generate both the database schemas and a Belief State server **a posteriori** to domain design time. 

## Ubiquia's Belief State

### One Input to Rule Them All 
MACHINA took a "common models" Swagger file and generated from it a JSON Schema, and Java code. The Java code in turn became both database entities and Data Transfer Objects. Ubiquia should use a single input file for everything--an Agent Communication Language file--and Ubiquia should interpret that accordingly--seamlessly and dynamically--for the developers.

### Simplicity as a first principle
Simplicity should be first and foremost. Any tradeoffs between simplicity and anything else should always favor simplicity.

### Foreign-Key-Based Flat Models
Database entities have to be related to each other, but we should not egress any 1:many or many:many relationships in the database. We can, however, egress the "1 side" of any relationship as a foreign key. Example:

```json
{
  "id": "a5c2e1f3-9bd4-4f7a-a3e9-2b3e21c4bcb1",
  "modelType": "Sensor",
  "nameEntity": "Example Sensor",
  "status": "ACTIVE",
  "lastCalibrated": "2024-11-15T13:45:00Z",
  "locationId": "b9175ea2-e7c4-403e-8c87-6c77a06ebf94",
}
```

```sql
CREATE TABLE sensor (
  id UUID PRIMARY KEY,
  model_type TEXT,
  nameEntity TEXT,
  status TEXT,
  last_calibrated TIMESTAMP,
  location_id UUID
);
```

```json
{
  "id": "b5c2e1f3-9bd4-4f7a-a3e9-2b3e21c4bcb1",
  "modelType": "Observation",
  "nameEntity": "Example Observation",
  "sensorId": "a5c2e1f3-9bd4-4f7a-a3e9-2b3e21c4bcb1",
}
```

```sql
CREATE TABLE observation (
  id UUID PRIMARY KEY
  sensor_id UUID,
  nameEntity TEXT,
)
```

Note that with the Ubiquia Belief State--provided the above schemas--a RESTful query via the URL would look like:

```http
http://localhost:8080/ubiquia/belief-state/observation/query/params?page=0&size=2&sensor.id=a5c2e1f3-9bd4-4f7a-a3e9-2b3e21c4bcb1
```

You'd get this response:

```json
{
  "content": [
    {
      "id": "b5c2e1f3-9bd4-4f7a-a3e9-2b3e21c4bcb1",
      "modelType": "Observation",
      "nameEntity": "Example Observation",
      "sensorId": "a5c2e1f3-9bd4-4f7a-a3e9-2b3e21c4bcb1",
    },
    {
      "id": "56643fb3-b97b-4289-99c2-c47ec25648fd",
      "modelType": "Observation",
      "nameEntity": "Example Observation",
      "sensorId": "a5c2e1f3-9bd4-4f7a-a3e9-2b3e21c4bcb1",
    },
  ],
  "number": 0,
  "size": 2,
  "totalElements": 2,
  "pageable": {
    "pageNumber": 0,
    "pageSize": 2,
    "sort": {
      "empty": true,
      "sorted": false,
      "unsorted": true
    },
    "offset": 0,
    "unpaged": false,
    "paged": true
  },
  "last": true,
  "totalPages": 1,
  "sort": {
    "empty": true,
    "sorted": false,
    "unsorted": true
  },
  "first": true,
  "numberOfElements": 2,
  "empty": false
}
```

## Implementation
Implementation should look like the Belief State "implementing" an Agent Communication Language and creating database schemas from it. This should happen at registration-time, and should happen transparently to clients. Implementation will have to make some **rather strong assumptions**. For example, any "list" field becomes either a 1:many or many:many relationship, provided a corresponding bidirectional relationship.

Consider the following example of a personEntity with pets. The Belief State generation process should assume that there is a 1:many relationship between a Person and Pets, and create the normalized relationship in the database. However, the "many" half of the relationship will never be egressed to clients.

```json
{
  "Person": {
    "description": "A model of a personEntity.",
    "type": "object",
    "allOf": [
      {
        "$ref": "#/definitions/BaseModel"
      }
    ],
    "properties": {
      "hairColor": {
        "$ref": "#/definitions/ColorType"
      },
      "nameEntity": {
        "$ref": "#/definitions/Name"
      },
      "pets": {
        "type": "array",
        "items": {
          "$ref": "#/definitions/Person"
        }
      }
    },
    "required": [
      "modelType"
    ]
  },
  "Animal": {
    "description": "The model of an animalEntity.",
    "allOf": [
      {
        "$ref": "#/definitions/BaseModel"
      }
    ],
    "properties": {
      "color": {
        "$ref": "#/definitions/ColorType"
      },
      "owner": {
        "$ref": "#/definitions/Person"
      }
    }
  }
}
  
```



## Contributors
* __Jeremy Case__: jeremycase@odysseyconsult.com
