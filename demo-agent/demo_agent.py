"""Demo: AI agents of Gemeente Voorbeeldstad using the legacy case system through the gateway.

The agents are scripted, so the demo is repeatable and needs no LLM or API key: each step is the
tool call an LLM-driven agent would make. Any MCP client (an LLM agent framework, Claude Desktop,
...) can connect to the same endpoint with its own token.

    python demo_agent.py [--gateway http://localhost:8080]
"""

import argparse
import asyncio
import json
import sys
import time
import urllib.error
import urllib.request

from mcp import ClientSession
from mcp.client.streamable_http import create_mcp_http_client, streamable_http_client

from dev_tokens import token_for

BOLD, DIM, GREEN, RED, RESET = "\033[1m", "\033[2m", "\033[32m", "\033[31m", "\033[0m"


async def call_tool(gateway: str, agent: str, tool: str, arguments: dict):
    """One tool call as `agent`: connect with the agent's own token, call, disconnect."""
    headers = {"Authorization": f"Bearer {token_for(agent, 'mcp')}"}
    async with create_mcp_http_client(headers=headers) as http:
        async with streamable_http_client(f"{gateway}/mcp", http_client=http) as (read, write):
            async with ClientSession(read, write) as session:
                await session.initialize()
                return await session.call_tool(tool, arguments)


async def list_tools(gateway: str, agent: str):
    headers = {"Authorization": f"Bearer {token_for(agent, 'mcp')}"}
    async with create_mcp_http_client(headers=headers) as http:
        async with streamable_http_client(f"{gateway}/mcp", http_client=http) as (read, write):
            async with ClientSession(read, write) as session:
                await session.initialize()
                return (await session.list_tools()).tools


async def step(gateway: str, title: str, agent: str, tool: str, zaaknummer: str, doel: str) -> bool:
    print(f"\n{BOLD}{title}{RESET}")
    print(f"{DIM}  {agent} -> {tool}(zaaknummer={zaaknummer}, doel={doel}){RESET}")
    result = await call_tool(gateway, agent, tool, {"zaaknummer": zaaknummer, "doel": doel})
    text = result.content[0].text
    if result.is_error:
        print(f"  {RED}✗ {text}{RESET}")
        return False
    for key, value in json.loads(text).items():
        print(f"  {GREEN}✓{RESET} {key}: {value}")
    return True


def print_audit(gateway: str) -> None:
    request = urllib.request.Request(f"{gateway}/api/v1/audit?limit=6",
                                     headers={"Authorization": f"Bearer {token_for('privacy-officer', 'audit.lezen')}"})
    with urllib.request.urlopen(request, timeout=5) as response:
        events = json.load(response)
    print(f"\n{BOLD}Audit trail, as the privacy officer sees it (GET /api/v1/audit){RESET}")
    print(f"  {'agent':<20} {'tool':<18} {'doel':<13} {'zaak':<17} uitkomst")
    for event in reversed(events):
        outcome = event["uitkomst"] + (f" ({event['reden']})" if event.get("reden") else "")
        print(f"  {event['agent']:<20} {event['tool']:<18} {event['doel'] or '':<13} "
              f"{event['zaaknummer'] or '':<17} {outcome}")


def wait_until_up(gateway: str, seconds: int = 90) -> None:
    deadline = time.time() + seconds
    while True:
        try:
            with urllib.request.urlopen(f"{gateway}/actuator/health", timeout=2) as response:
                if response.status == 200:
                    return
        except (urllib.error.URLError, ConnectionError):
            pass
        if time.time() > deadline:
            sys.exit(f"Gateway at {gateway} did not come up within {seconds} s")
        time.sleep(2)


async def main(gateway: str) -> int:
    wait_until_up(gateway)
    tools = await list_tools(gateway, "klantcontact-agent")
    print(f"{BOLD}Gateway {gateway}/mcp offers:{RESET} " + ", ".join(tool.name for tool in tools))

    results = [
        await step(gateway, "1. A citizen calls about her permit; the customer-contact agent looks it up",
                   "klantcontact-agent", "zaak_samenvatting", "ZAAK-2026-000123", "KLANTCONTACT"),
        not await step(gateway, "2. The same agent wants the data for another purpose",
                       "klantcontact-agent", "zaak_samenvatting", "ZAAK-2026-000123", "RAPPORTAGE"),
        await step(gateway, "3. The reporting agent may see the status...",
                   "rapportage-agent", "zaak_status", "ZAAK-2026-000456", "RAPPORTAGE"),
        not await step(gateway, "4. ...but not the case notes",
                       "rapportage-agent", "zaak_samenvatting", "ZAAK-2026-000456", "RAPPORTAGE"),
        not await step(gateway, "5. An agent nobody registered, with a valid token",
                       "schaduw-agent", "zaak_status", "ZAAK-2026-000123", "KLANTCONTACT"),
        not await step(gateway, "6. The legacy system is slow: the gateway times out instead of hanging",
                       "klantcontact-agent", "zaak_status", "ZAAK-2026-000500", "KLANTCONTACT"),
    ]
    print_audit(gateway)
    if all(results):
        print(f"\n{GREEN}All steps behaved as expected.{RESET}")
        return 0
    print(f"\n{RED}Some steps did not behave as expected.{RESET}")
    return 1


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--gateway", default="http://localhost:8080")
    sys.exit(asyncio.run(main(parser.parse_args().gateway.rstrip("/"))))
