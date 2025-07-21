# Architecture Decision Record: SQL Databases

## Decision
Ubiquia will provide the ability to configure agents; these configurations can run different databases (e.g., "test" with an in-memory database, "prod" with a full-up distributed database cluster, etc.) The common denominator is that in each case, the Ubiquia agent will have an SQL-compliant database available to it, be it in-memory, a distributed database, or a database reachable via another Ubiquia agent.

## Status 

### [1.0.0] - 2025-07-01
- Accepted.

### Pros
- ACID-ity
- Piggy-backing off of decades of a well-understood technology
- Cascading updates to data
- Rolling updates to SQL schemas
- No need for "middleware" application logic to handle schemas

### Cons
- SQL Schema enforcement can be a headache for devs
- Schema enforcement makes a domain agnostic system even more difficult to implement

### Alternatives
- NoSQL

## Context

> "I've always loved dinosaurs." - Kanye West


### Context: Foundational
There are two kinds of technologies: dinosaurs, and sharks. New technologies are not their own category - not really. They are really either "pre-sharks" or "proto-dinosaurs"; it takes a seasoned technologist to suss out which category a new technology will ultimately become. To further complicate matters, business people have become incredibly good at hyping and marketing new technologies ("this shiny new thing will solve all your problems!") - look no further than the [Gartner Hype Cycle](https://en.wikipedia.org/wiki/Gartner_hype_cycle). In this echo chamber, it is all-too-easy to want to use a flashy new technology (i.e., a potential proto-dinosaur) in a greenfield software system, but we will resist the urge on the Ubiquia project.

[Structured Query Language](https://en.wikipedia.org/wiki/SQL) (SQL) is a shark technology. It is the author's opinion that SQL has not only endured, but been _the_ dominant database technology since the 1970's not because the language is necessarily developer-friendly (it is not, or else industry would not keep trying to replace it) but because it addresses a fundamental aspect of software: ***customers don't care about software, they care about data***.

Due to its foundations in [Relational Algebra](https://en.wikipedia.org/wiki/Relational_algebra) and [Tuple Calculus](https://en.wikipedia.org/wiki/Tuple_relational_calculus), SQL treats data in relational terms. It forces developers to think of their data in relational terms and attempt to [normalize](https://en.wikipedia.org/wiki/Database_normalization) it. Coincidentally, it is the opinion of the author that customers/clients/operators almost always think about their data in relational terms, even if they don't necessarily think in SQL terms. Consider a military operator: they know that their weapon system (defined as "X") will have "N" potential capabilities (defined as "Y"), only one of which might be configured at any point in time. On future date "T", military operators may want to know which weapon systems (again, "X") will be configured with capability "Y". Doing this sort of query (which is really a software form of [Set Theory](https://en.wikipedia.org/wiki/Set_theory)) in SQL is trivial, as is adding capabilities and updating configurations. It is trivial because SQL not only allows normalized, relational data...it enforces it. 

It also happens that data often outlives the software that manipulates it. Again, customers/clients/operators don't care about software, they care about data. The software is only a mechanism to bus around data. And _precisely_ because SQL is structured, it allows any future software to "pick up" where any previous software may have "left off." 

### Context: Future-Proofing

Because SQL has been the dominant database technology for ~50 years (long even for a shark technology), many implementations have sprung up around it: in-memory databases like H2, HSQL (both useful for testing), monolithic databases like MySQL, Postgres, Oracle, SaaS databases like Amazon's RDS, and even "NewSQL" databases like YugabyteDB and CockroachDB. The common denominator--in each case--is "SQL compliance." In reality, this (basically) means that each database understands the same API calls; only very-small "dialect" differences exist. These rare-few "dialect differences" are themselves trivial because many frameworks (e.g., Spring Boot) allow for "plug-in" drivers and Object Relational Mappers (e.g., Hibernate) that are made precisely to address any dialect differences.

Put succinctly: if Ubiquia uses "SQL" as its database technology, it is then free to assume the use of any SQL-compliant database technology depending on the need. In a sense, this is tying Ubiquia's fate to a "shark" technology so that it, too, can be a shark.

### Context: Enabling Secondary Databases

Another consideration: because SQL databases ensure [ACID-ity](https://en.wikipedia.org/wiki/ACID), they also allow for discretized, [OLTP](https://en.wikipedia.org/wiki/Online_transaction_processing) style transactions. With this "rigorous" truth data, it is entirely possible to then use "secondary" database technologies for [OLAP](https://en.wikipedia.org/wiki/Online_analytical_processing) and/or caching. Concretely, this would be like using ElasticSearch "on top of" an SQL database to [Map Reduce](https://en.wikipedia.org/wiki/MapReduce) the data into searchable terms, or using a cache like [Redis](https://en.wikipedia.org/wiki/Redis). These secondary technologies only work because they go from more-rigor to less; it is not possible to go the other way around (i.e., to use an ElasticSearch database as the primary database and then use an SQL as the secondary database.)

### Context: Schemas

SQL uses schemas to create tables, indexes, etc. Importantly, the values of these schemas are typed (e.g., VARCHAR, BLOB, JSONB, etc.) A consequence of having these schemas is that it's possible to version schemas and evolve them over time....which is something that any operational software system should _expect_. If a software system cannot easily-change data over time, customers will eventually find one that can. Remember: ***customers don't care about software, they care about data***.

Several tools have popped up with the ability to evolve schemas over time, like [Liquibase](https://www.liquibase.com/). Because SQL enforces these rigorous schemas, it's entirely possible to evolve database data over time, and do so via automated processes.

### Context: Alternative Technologies

At a high level, SQL databases are "general purpose" databases. This is because most data is relational in some form; by extension, relational databases are good at dealing with most data. Rule of thumb: when dealing with relational data (as we mostly do in the DoD), use a relational database. More specifically, this boils down to how data is modeled in a software system. Most of the data we are going to model in Ubiquia will have very explicit relationships with other data.

NoSql databases are--counter to the prevailing marketing--niche databases. They're typically specialized to do one thing and one thing well. If that one thing happens to be the problem your software system is trying to solve (for example, analyzing unstructured data) then use the appropriate specialized database. Otherwise, you're better off with a general purpose database.

There are graph databases (Neo4j) that excel at deriving relationships from unstructured data. There are time series databases (InfluxDB, Prometheus) that do time series events. There are key value pair databases (etcd, hazelcast, redis), search engines, (ElasticSearch, splunk, solr), and even locational databases (PostGIS.) There are also "document" store databases (MongoDB, couchDB, couchBase) that specialize in storing unstructured, schema-less JSON data.

## Consequences & Tradeoffs

SQL enforces schemas; it is the opinion of the author that schema-based frustrations drive many devs to adopt alternative technologies. However, alternative technologies (e.g., MongoDB) don't enforce _explicit_ schemas, but they _do_--sooner or later--create the need for _implicit_ schemas (oftentimes, this need only becomes apparent _after_ a software system has become operational.) It is the author's opinions that ***many devs are--unknowingly--making their own lives easier at the expense of the customer/operator/client when using alternative technologies to SQL***.

Consider how an operational software system using a schema-less database might need to handle version discrepancies in database records. Because the database has no built-in mechanism to do this, the application logic must instead handle it. Over time, this results in bloated middleware - it is unavoidable. Every time an _implied_ schema change is made (or bug is found), it will need a corresponding patch in the application logic. The application logic becomes an unavoidable, ever-growing pile of tech debt as changes are made or bugs are found. If tech debt kills software projects, then selecting a schema-less, NoSQL database for a greenfield software system is akin to automatically putting it in the aforementioned "proto-dinosaur" category.  

SQL, then, represents an "up-front" investment - it will undoubtedly frustrate developers. We will lose developer time fighting with schemas and various other quirks of SQL databases. But those are _good_ pains; we are eschewing the "long and ugly tail" of problems that arise with schema-less databases only long-after a software system has been delivered to operators/clients/customers. 

## Contributors
- **Jeremy Case**: jeremycase@odysseyconsult.com