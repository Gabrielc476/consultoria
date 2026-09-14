#!/usr/bin/env python3
import json
import sys
import time
import urllib.error
import urllib.request

try:
    import jwt
except ImportError:
    import subprocess
    subprocess.check_call([sys.executable, "-m", "pip", "install", "-q", "PyJWT"])
    import jwt

SECRET = "GovFlowLocalDevJwtSecretKeyChangeMe32b!"
TENANT = "11111111-1111-1111-1111-111111111111"
USER = "22222222-2222-2222-2222-222222222222"
BASE = "http://host.docker.internal:8080"


def req(method, path, headers=None):
    url = BASE + path
    request = urllib.request.Request(url, method=method, headers=headers or {})
    try:
        with urllib.request.urlopen(request, timeout=10) as resp:
            body = resp.read().decode()
            return resp.status, dict(resp.headers), body
    except urllib.error.HTTPError as e:
        body = e.read().decode()
        return e.code, dict(e.headers), body


def main():
    token = jwt.encode(
        {
            "sub": USER,
            "tenant_id": TENANT,
            "username": "analista",
            "roles": ["ANALISTA"],
            "exp": int(time.time()) + 3600,
        },
        SECRET,
        algorithm="HS256",
    )

    failures = []

    def check(name, cond, detail=""):
        if cond:
            print(f"PASS: {name}")
        else:
            print(f"FAIL: {name} {detail}")
            failures.append(name)

    status, headers, body = req("GET", "/api/v1/auth/login")
    check("public auth", status == 200, f"status={status} body={body}")

    status, headers, body = req("GET", "/api/v1/whatsapp/webhook/evolution")
    check("public webhook", status == 200, f"status={status} body={body}")

    status, headers, body = req("GET", "/api/v1/core/prefeituras")
    ct = headers.get("Content-Type", headers.get("content-type", ""))
    check("401 status", status == 401, f"status={status} body={body}")
    check("401 content-type", "application/problem+json" in ct, f"ct={ct}")
    try:
        problem = json.loads(body)
    except json.JSONDecodeError:
        problem = {}
    check("401 type", "type" in problem, body)
    check("401 title", "title" in problem, body)
    check("401 status field", problem.get("status") == 401, body)
    check("401 detail", "detail" in problem, body)
    check("401 instance", problem.get("instance") == "/api/v1/core/prefeituras", body)

    status, headers, body = req(
        "GET",
        "/api/v1/core/docs",
        {
            "Authorization": f"Bearer {token}",
            "X-Tenant-Id": "99999999-9999-9999-9999-999999999999",
            "X-Correlation-Id": "corr-test-123",
        },
    )
    check("core authenticated", status == 200, f"status={status} body={body}")
    check("tenant propagated", TENANT in body, body)
    check("user propagated", USER in body, body)
    check("correlation propagated", "corr-test-123" in body, body)
    check("forged tenant stripped", "99999999" not in body, body)

    for path in (
        "/api/v1/whatsapp/messages",
        "/api/v1/ai/extract",
        "/api/v1/transferegov/convenios",
    ):
        status, headers, body = req("GET", path, {"Authorization": f"Bearer {token}"})
        check(f"route {path}", status == 200 and TENANT in body, f"status={status} body={body}")
        check(
            f"correlation auto {path}",
            '"x_correlation_id":"' in body and body.split('"x_correlation_id":"')[1][:36],
            body,
        )

    if failures:
        print("FAILURES:", ", ".join(failures))
        sys.exit(1)
    print("ALL ACCEPTANCE CHECKS PASSED")


if __name__ == "__main__":
    main()
