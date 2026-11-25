"""Development tokens, signed with the shared dev secret the gateway accepts when
GATEWAY_SECURITY_DEV_JWT_SECRET is set. In production the agent gets its token from the
identity provider (OAuth2 client credentials) and the gateway validates it against the issuer."""

import os
import time

import jwt

DEV_SECRET = os.environ.get("GATEWAY_SECURITY_DEV_JWT_SECRET", "dev-only-secret-change-me-at-least-32-bytes")


def token_for(client_id: str, scope: str, lifetime_seconds: int = 300) -> str:
    now = int(time.time())
    claims = {"sub": client_id, "azp": client_id, "scope": scope, "iat": now, "exp": now + lifetime_seconds}
    return jwt.encode(claims, DEV_SECRET, algorithm="HS256")


if __name__ == "__main__":
    import sys

    if len(sys.argv) != 3:
        sys.exit("usage: python dev_tokens.py <client-id> '<scope ...>'")
    print(token_for(sys.argv[1], sys.argv[2]))
