# Agent Instructions for Context Maintenance

**CRITICAL INSTRUCTION FOR ALL FUTURE AI AGENTS:**

To prevent context loss across different chat sessions, this repository maintains a persistent written memory. 
Whenever you start a new session or are asked to make changes, you **MUST** follow this workflow:

## 1. Onboarding (Before making changes)
Read the following files to gain context on what has been done, the current architecture, and past pitfalls:
- `docs/ARCHITECTURE_STATE.md` - Current architecture, algorithms, and features.
- `docs/AGENT_CONTEXT_LOG.md` - Log of past changes, why they were made, and implementation flow.
- `docs/AGENT_MISTAKES_WORKAROUNDS.md` - List of previous mistakes, failed approaches, and their workarounds.

## 2. Execution
Perform the tasks requested by the user, adhering to the architectural constraints established in `ARCHITECTURE_STATE.md` and keeping in mind the known limitations in `AGENT_MISTAKES_WORKAROUNDS.md`.

## 3. Offboarding (After making changes)
Before completing your task or pushing to Git, **you MUST update the context files**:
- **Update `docs/AGENT_CONTEXT_LOG.md`**: Add a new timestamped entry detailing exactly what you changed, why you changed it (the reasoning), and why it was the best option. Include mermaid flowcharts if you altered logic flows.
- **Update `docs/AGENT_MISTAKES_WORKAROUNDS.md`**: If you hit any roadblocks, errors, or made mistakes during your session, document them here along with the workaround or fix you applied.
- **Update `docs/ARCHITECTURE_STATE.md`**: If you added new features, modified the algorithms, or changed the system architecture, update this document and its flowcharts to reflect the *current* state of the codebase.

By strictly adhering to this protocol, we ensure that DroidBot's complex context is never lost between agent invocations.
