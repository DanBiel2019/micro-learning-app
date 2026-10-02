"""Fetches the source material each segment is written from.

A small local model writes far more accurately from a provided text than from memory, so
every segment is grounded in a real document: a Wikipedia article for evergreen ideas, or a
recent article from an engineering news feed for the "fresh" segment.
"""
from __future__ import annotations

import html
import json
import re
import time
import urllib.error
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from email.utils import parsedate_to_datetime

USER_AGENT = "MicroLearningPodcast/2.0 (https://github.com/DanBiel2019/micro-learning-app)"
WIKI_API = "https://en.wikipedia.org/w/api.php"
MAX_SOURCE_CHARS = 12_000

_last_request = 0.0


def _get(url: str, timeout: int = 30) -> bytes:
    """GET with a descriptive user agent, gentle pacing and retry on 429/5xx."""
    global _last_request
    for attempt in range(5):
        wait = 1.0 - (time.time() - _last_request)
        if wait > 0:
            time.sleep(wait)
        _last_request = time.time()
        req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT, "Accept-Encoding": "identity"})
        try:
            with urllib.request.urlopen(req, timeout=timeout) as r:
                return r.read()
        except urllib.error.HTTPError as e:
            if e.code in (429, 500, 502, 503, 504) and attempt < 4:
                time.sleep(int(e.headers.get("Retry-After", "0") or 0) or 2 ** (attempt + 2))
                continue
            raise
    raise RuntimeError(f"unreachable: {url}")


@dataclass
class Document:
    title: str
    url: str
    text: str
    publisher: str
    published: str | None = None


# ---------------------------------------------------------------- Wikipedia

def wikipedia(title: str) -> Document:
    params = {
        "action": "query", "prop": "extracts|info", "explaintext": 1, "exsectionformat": "plain",
        "redirects": 1, "inprop": "url", "titles": title.replace("_", " "), "format": "json",
    }
    data = json.loads(_get(f"{WIKI_API}?{urllib.parse.urlencode(params)}"))
    page = next(iter(data["query"]["pages"].values()))
    if "missing" in page:
        raise LookupError(f"No Wikipedia article for {title}")
    text = page.get("extract", "")
    # Drop reference-y tail sections.
    text = re.split(r"\n(See also|References|Notes|Further reading|External links|Bibliography)\n", text)[0]
    return Document(page["title"], page["fullurl"], _trim(text), "Wikipedia")


def category_members(category: str) -> list[str]:
    params = {"action": "query", "list": "categorymembers", "cmtitle": f"Category:{category}",
              "cmtype": "page", "cmlimit": 500, "format": "json"}
    data = json.loads(_get(f"{WIKI_API}?{urllib.parse.urlencode(params)}"))
    return [m["title"] for m in data["query"]["categorymembers"]
            if not m["title"].startswith(("List of", "Index of", "Outline of", "Glossary"))]


# ---------------------------------------------------------------- News feeds

@dataclass
class FeedItem:
    feed: str
    title: str
    url: str
    summary: str
    published: datetime | None


def _local(tag: str) -> str:
    return tag.rsplit("}", 1)[-1]


def _child_text(el: ET.Element, *names: str) -> str:
    for child in el:
        if _local(child.tag) in names and (child.text or "").strip():
            return child.text.strip()
    return ""


def _parse_date(value: str) -> datetime | None:
    if not value:
        return None
    try:
        return parsedate_to_datetime(value)
    except (TypeError, ValueError):
        pass
    try:
        return datetime.fromisoformat(value.replace("Z", "+00:00"))
    except ValueError:
        return None


def read_feed(name: str, url: str) -> list[FeedItem]:
    root = ET.fromstring(_get(url))
    items = []
    for el in root.iter():
        if _local(el.tag) not in ("item", "entry"):
            continue
        link = _child_text(el, "link")
        if not link:
            for child in el:
                if _local(child.tag) == "link" and child.get("href") and child.get("rel", "alternate") == "alternate":
                    link = child.get("href")
                    break
        summary = _strip_html(_child_text(el, "description", "summary", "content"))[:300]
        published = _parse_date(_child_text(el, "pubDate", "published", "updated", "date"))
        title = html.unescape(_child_text(el, "title"))
        if title and link:
            items.append(FeedItem(name, title, link, summary, published))
    return items


def recent_items(feeds: list[dict], days: int = 21) -> list[FeedItem]:
    cutoff = datetime.now(timezone.utc) - timedelta(days=days)
    out = []
    for f in feeds:
        try:
            for item in read_feed(f["name"], f["url"]):
                when = item.published
                if when and when.tzinfo is None:
                    when = when.replace(tzinfo=timezone.utc)
                if when is None or when >= cutoff:
                    out.append(item)
        except Exception as e:  # one broken feed shouldn't stop the episode
            print(f"  feed {f['name']} skipped: {e}")
    return out


def article(item: FeedItem) -> Document:
    raw = _get(item.url).decode("utf-8", errors="replace")
    raw = re.sub(r"(?is)<(script|style|nav|header|footer|aside|form)[^>]*>.*?</\1>", " ", raw)
    paragraphs = [_strip_html(p) for p in re.findall(r"(?is)<(?:p|li|h2|h3)[^>]*>(.*?)</(?:p|li|h2|h3)>", raw)]
    text = "\n".join(p for p in paragraphs if len(p.split()) >= 6)
    published = item.published.date().isoformat() if item.published else None
    return Document(item.title, item.url, _trim(text), item.feed, published)


# ---------------------------------------------------------------- helpers

def _strip_html(s: str) -> str:
    s = re.sub(r"(?s)<[^>]+>", " ", s)
    return re.sub(r"\s+", " ", html.unescape(s)).strip()


def _trim(text: str) -> str:
    text = re.sub(r"\n{3,}", "\n\n", text).strip()
    if len(text) <= MAX_SOURCE_CHARS:
        return text
    cut = text[:MAX_SOURCE_CHARS]
    return cut[: cut.rfind(".") + 1] or cut
