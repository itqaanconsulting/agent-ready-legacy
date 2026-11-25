# Threat model

Scope: the gateway between AI agents (and internal applications) and the legacy case system. The assets are the case data, in particular the personal data of citizens, and the availability of the legacy system.

## Threats and mitigations

| # | Threat | Mitigation | Verified by |
|---|---|---|---|
| T1 | An agent impersonates another agent to gain its permissions | Identity is taken from the validated JWT (`azp`), never from tool arguments | `AgentGovernanceTest` (one token per agent) |
| T2 | A client with a valid token, but never approved, uses the tools ("shadow agent") | Deny by default: only registered client ids get access | `unregisteredAgentGetsNothingEvenWithAValidToken` |
| T3 | An agent uses data for another purpose than it was approved for | Purpose binding: every call states a purpose, checked against the registry and audited | `reportingAgentMayOnlyUseTheStatusToolForReporting` |
| T4 | Personal data leaks to the agent and so to the LLM provider | No initiator for agents; free text redacted (names, BSN, IBAN, phone, e-mail, postcode) | `customerContactAgentGetsARedactedSummary`, `PiiRedactorTest` |
| T5 | An agent loops or is abused to scrape the case system | Rate limit per agent, applied before the other checks; each call audited | `agentsAreRateLimited` |
| T6 | A slow or hanging legacy system takes the gateway (and the agents) down with it | Connect timeout 2 s, read timeout 3 s; a clear, retryable error | `legacyFailuresReachTheAgentAsToolErrorsWithoutDetails` |
| T7 | Injection into the SOAP request through the case number | Case numbers validated against a strict pattern; values XML-escaped | `rejectsMalformedCaseNumbersBeforeCallingTheLegacySystem` |
| T8 | A compromised or misbehaving legacy system returns malicious XML (XXE) | DOCTYPEs and external entities disabled in the parser | `SecureXmlParsingTest`, `refusesXmlWithExternalEntitiesFromTheLegacySystem` |
| T9 | Personal data or forged entries end up in the audit trail | The audit trail stores no case content; agent-supplied values only when well-formed | `everyCallIsAuditedWithoutCaseContent` |
| T10 | Error messages reveal internals of the legacy system | Fixed error texts and problem details; legacy faults are not passed on | `mapsLegacyFaultsToProblemDetails` |
| T11 | A vulnerable dependency (supply chain) | CycloneDX SBOM on every build, scanned with Grype; the build fails on high or critical findings | CI |
| T12 | The development token secret is used in production | Only active when explicitly configured; logs a warning at startup | `SecurityConfig` |

## Residual risks

These are not solved by this gateway, and a real deployment should address them:

- **Prompt injection through case notes.** A note can contain text such as "ignore your instructions". Redaction removes personal data, not instructions. The calling agent must treat tool output as data. The gateway limits the damage, because the agent cannot reach more than its registry entry allows.
- **Redaction is pattern based.** It catches known names and common formats, not every way personal data can be written in free text (a birth date in words, a house description). For high-risk case types, leave the notes out of the agent tools altogether.
- **`tools/list` is not filtered per agent.** Every agent with the `mcp` scope sees both tools, even if it may only call one. The call itself is refused, so this reveals capability, not data.
- **In-memory rate limits** are per instance; see the production notes in the [architecture](architecture.md).
- **The LLM provider.** What the agent sends on to its model provider is outside the gateway. Data minimisation here reduces what can be sent; contracts and data residency remain necessary.

## Mapping to NIS2 (Cyberbeveiligingswet)

NIS2 article 21 requires appropriate and proportionate risk-management measures. In the Netherlands it is implemented by the Cyberbeveiligingswet, in force since 15 August 2026, under which municipalities are *essential entities* and use the BIO2 (Baseline Informatiebeveiliging Overheid) as their framework. Relevant here:

| Article 21(2) measure | In this project |
|---|---|
| (i) access control policies | Per-agent registry, scopes per endpoint, deny by default |
| (d) supply chain security | SBOM per build, vulnerability scan in CI |
| (e) security in acquisition, development and maintenance | Threats above covered by automated tests on every change |
| (b) incident handling | Audit trail with reasons; refusals and errors can be monitored and alerted on |
| (h) cryptography | Signed JWTs; TLS at the ingress (not part of the demo) |

Under the GDPR the same controls support purpose limitation (art. 5(1)(b)), data minimisation (art. 5(1)(c)) and accountability (art. 5(2)). Where an agent falls under the AI Act's high-risk rules, the audit trail also supports the record-keeping and human oversight asked of deployers.

*This document describes the design of a portfolio project; it is not legal advice.*
