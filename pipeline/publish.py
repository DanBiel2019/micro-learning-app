"""Publishes pipeline/out to GitHub Releases.

  GITHUB_TOKEN=... python publish.py --repo OWNER/REPO [--out ./out]

Two releases are touched, neither of which is marked "latest" (that badge belongs to the
app's APK release, so the Releases page leads with the app):

  ep-YYYY-MM-DD   the episode archive: one MP3 per segment, plus that day's feed.json
  daily-feed      a rolling release whose feed.json and ledger.json are replaced every day;
                  the app reads releases/download/daily-feed/feed.json
"""
from __future__ import annotations

import argparse
import json
import mimetypes
import os
import urllib.error
import urllib.request
from pathlib import Path

API = "https://api.github.com"
FEED_TAG = "daily-feed"


def call(method: str, url: str, token: str, body: bytes | None = None, content_type: str = "application/json"):
    req = urllib.request.Request(url, data=body, method=method, headers={
        "Authorization": f"Bearer {token}",
        "Accept": "application/vnd.github+json",
        "Content-Type": content_type,
    })
    with urllib.request.urlopen(req, timeout=300) as r:
        raw = r.read()
        return json.loads(raw) if raw else None


def release_by_tag(repo: str, tag: str, token: str) -> dict | None:
    try:
        return call("GET", f"{API}/repos/{repo}/releases/tags/{tag}", token)
    except urllib.error.HTTPError as e:
        if e.code == 404:
            return None
        raise


def upload(release: dict, path: Path, token: str, replace: bool = False) -> None:
    if replace:
        for asset in release.get("assets", []):
            if asset["name"] == path.name:
                call("DELETE", asset["url"], token)
    ctype = mimetypes.guess_type(path.name)[0] or "application/octet-stream"
    call("POST", f"{release['upload_url'].split('{')[0]}?name={path.name}", token, path.read_bytes(), ctype)
    print(f"  uploaded {path.name} -> {release['tag_name']}")


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--repo", required=True)
    ap.add_argument("--out", type=Path, default=Path(__file__).parent / "out")
    args = ap.parse_args()
    token = os.environ["GITHUB_TOKEN"]
    repo = args.repo

    feed = json.loads((args.out / "feed.json").read_text())
    tag = feed["id"]
    notes = [f"**{feed['theme']}** · {feed['totalDurationMs'] // 60000} min", ""]
    notes += [f"{i}. {s['title']}: {s['source']['title']} ({s['source']['author']})"
              for i, s in enumerate(feed["segments"], 1)]

    # 1. The dated episode archive.
    episode = release_by_tag(repo, tag, token) or call("POST", f"{API}/repos/{repo}/releases", token, json.dumps({
        "tag_name": tag,
        "name": f"Episode {feed['date']}: {feed['title']}",
        "body": "\n".join(notes),
        "make_latest": "false",
    }).encode())
    for path in sorted(args.out.glob("seg-*.mp3")) + [args.out / "feed.json"]:
        upload(episode, path, token, replace=True)

    # 2. The rolling feed the app reads.
    rolling = release_by_tag(repo, FEED_TAG, token)
    body = json.dumps({
        "name": "Daily feed (used by the app)",
        "body": f"Today's episode: [{feed['title']}](../../releases/tag/{tag}). Updated automatically every morning; "
                "the app reads `feed.json` from here.",
        "make_latest": "false",
    })
    if rolling is None:
        rolling = call("POST", f"{API}/repos/{repo}/releases", token,
                       json.dumps({**json.loads(body), "tag_name": FEED_TAG}).encode())
    else:
        rolling = call("PATCH", rolling["url"], token, body.encode())
    for path in (args.out / "feed.json", args.out / "ledger.json"):
        upload(rolling, path, token, replace=True)

    print(f"Published {episode['html_url']} and refreshed {FEED_TAG}")


if __name__ == "__main__":
    main()
