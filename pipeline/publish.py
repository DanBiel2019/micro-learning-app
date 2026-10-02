"""Publishes pipeline/out as a GitHub Release tagged ep-YYYY-MM-DD and marks it latest.

  GITHUB_TOKEN=... python publish.py --repo OWNER/REPO [--out ./out]

The app downloads https://github.com/OWNER/REPO/releases/latest/download/feed.json, so the
newest episode release must be the repo's "latest" release.
"""
from __future__ import annotations

import argparse
import json
import mimetypes
import os
import urllib.request
from pathlib import Path

API = "https://api.github.com"


def call(method: str, url: str, token: str, body: bytes | None = None, content_type: str = "application/json"):
    req = urllib.request.Request(url, data=body, method=method, headers={
        "Authorization": f"Bearer {token}",
        "Accept": "application/vnd.github+json",
        "Content-Type": content_type,
    })
    with urllib.request.urlopen(req, timeout=120) as r:
        return json.load(r)


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--repo", required=True)
    ap.add_argument("--out", type=Path, default=Path(__file__).parent / "out")
    args = ap.parse_args()
    token = os.environ["GITHUB_TOKEN"]

    feed = json.loads((args.out / "feed.json").read_text())
    tag = feed["id"]
    lines = [f"**{feed['theme']}** · {feed['totalDurationMs'] // 60000} min", ""]
    lines += [f"{i}. {s['title']}: {s['source']['title']} ({s['source']['author']})"
              for i, s in enumerate(feed["segments"], 1)]

    release = call("POST", f"{API}/repos/{args.repo}/releases", token, json.dumps({
        "tag_name": tag,
        "name": f"{feed['date']}: {feed['title']}",
        "body": "\n".join(lines),
        "make_latest": "true",
    }).encode())
    upload = release["upload_url"].split("{")[0]

    assets = sorted(args.out.glob("seg-*.mp3")) + [args.out / "feed.json", args.out / "ledger.json"]
    for path in assets:
        ctype = mimetypes.guess_type(path.name)[0] or "application/octet-stream"
        call("POST", f"{upload}?name={path.name}", token, path.read_bytes(), ctype)
        print(f"  uploaded {path.name}")
    print(f"Published {release['html_url']}")


if __name__ == "__main__":
    main()
