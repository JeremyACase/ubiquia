# Architecture Decision Record: Fail Fast

## Decision
Ubiquia should adopt a "fail first, fail fast" philosophy. Ubiquia components should make no qualms about complaining when something is wrong - they should do it loudly and proudly.

Concretely, this philosophy means Ubiquia should allow for configurable "strict checking of payloads" (and this should be on, by default, in production.) It also means we should be as pedantic with our schemas as is possible via the ***Agent Communication Language***.

## Status 

### [1.0.0] - 2024-10-08
- Accepted.

## Summary 

### Pros
- Assures high-quality data flowing through the system
- Reduces potentially "propagating" issues

### Cons
- Can increase the friction to introducing hotfixes/features

### Alternatives
- Be less stringent on quality checks

## Context

> "With schemas, all things are possible." - Abraham Lincoln

Software represents infinite possibilities. Unlike hardware, the only physical laws that software is beholden to are--really--the laws that define how electrons fly over transistors and wires. Beyond this fundamental constraint, anything is possible with software.

It is precisely this capacity for infinite possibilities which makes building software systems at scale so fiendishly-difficult: there are many ways to make software work; some of those ways even make software work correctly.

Put differently, just because something in a software system worked doesn't mean it worked correctly. Who is to say that something downstream isn't broken as a consequence of a bug slipping by upstream? In a sense, software is nothing more than a distributed mathematical proof - the more variables, the harder the proof. We should be zealous in our attempt to reduce the amount of variables we must account for in ensuring correct behavior of our software.

Worse--and perhaps without analogue to a mathematical proof--the further a bug propagates through a system, often the more developer time it takes to diagnose. Put differently, a propagating bug causes compounding issues, each possibly requiring successive developer time to diagnose.

Take, for instance, a payload sent to a Ubiquia agent. Let's assume this payload has a malformed payload per the ***Agent Communication Language*** schema. There are two scenarios to consider.

In the first scenario, Ubiquia is configured with strict checking. When the client sends the payload, Ubiquia loudly complains that the payload is malformed - it even echoes back to the client which specific fields were in error. It even goes so far as to explain specifically how the fields are in violation (all of which is possible because Ubiquia is using schemas.)

The client gets this error message back, and squashes this bug immediately and without mercy. After fixing this bug, they send another payload to Ubiquia and everything works precisely as intended. It is beautiful - the clouds part, the sun begins to shine, and one begins to wonder if they can--in fact--possibly handle any more beauty before spontaneously exploding into little rainbows.

In the second scenario, Ubiquia is configured without strict checking. When the client sends the malformed payload, Ubiquia doesn't complain. It "just rolls with it, man." The payload propagates through all the way to the database before being persisted. Ubiquia even echoes the payload back to the client, denoting success.

This is a beautiful thing, right? Right? Spoiler alert - it is not. This scenario doesn't occur in a world of beauty. This is a dark world, with threats lurking behind every rock and every shrub. It is a world of evil, of unknown unknowns.

In this world, downstream errors start popping up, their root cause unclear to those desperately trying to diagnose them. "But why is X happening," our intrepid heroes loudly shout, shaking their fists at the heavens. "It doesn't make sense!"

But the heavens don't listen. Our intrepid heroes are forced to rely on their own grit and determination. They spend hours--days, even--attempting to get to the root cause of the erratic behavior. "But none of this should be possible," they mutter to themselves, hunched over their keyboards, their only solace the bright screenlight of their monitors in an otherwise-dark world.

Hours of diagnosing later, haggard and at wit's end, they find the root issue. "Oh," they proclaim, battered and mostly broken. "The issues were all due to turning off strict checking? That's all it was?"

## Consequences & Tradeoffs
Strict checking will of course be annoying - because no one LIKES being told that they're doing something wrong. But the amount of overhead developer time spent doing things like ensuring that our payloads match our schemas pales in comparison to the hours "saved" by "letting the little things slide, man."


## Contributors
- **Jeremy Case**: jeremycase@odysseyconsult.com