"""Runs an open-weights model locally with llama.cpp's llama-server and asks it for JSON.

Default model: Qwen3-8B (Apache-2.0), quantised to Q4_K_M, which fits comfortably in a
GitHub Actions runner's 16 GB and needs no GPU or API key. Output is constrained to a JSON
schema by llama.cpp's grammar sampler, so every response parses.
"""
from __future__ import annotations

import json
import os
import socket
import subprocess
import time
import urllib.request
from pathlib import Path

DEFAULT_MODEL = Path(os.environ.get("LLM_MODEL", Path.home() / "models" / "Qwen3-8B-Q4_K_M.gguf"))
DEFAULT_SERVER = os.environ.get("LLAMA_SERVER", "llama-server")


def _free_port() -> int:
    with socket.socket() as s:
        s.bind(("127.0.0.1", 0))
        return s.getsockname()[1]


def inline_refs(schema: dict) -> dict:
    """Resolves $ref/$defs so the grammar converter sees one self-contained schema."""
    defs = schema.get("$defs", {})

    def walk(node):
        if isinstance(node, dict):
            if "$ref" in node:
                return walk(defs[node["$ref"].split("/")[-1]])
            out = {}
            for k, v in node.items():
                if k == "$defs" or (k == "title" and isinstance(v, str)):
                    continue  # pydantic's display titles, not fields
                if k == "properties":
                    out[k] = {name: walk(prop) for name, prop in v.items()}  # field names are kept as-is
                else:
                    out[k] = walk(v)
            return out
        if isinstance(node, list):
            return [walk(v) for v in node]
        return node

    return walk(schema)


class LocalLLM:
    def __init__(self, model: Path = DEFAULT_MODEL, server: str = DEFAULT_SERVER,
                 context: int = 12288, threads: int | None = None):
        if not Path(model).exists():
            raise FileNotFoundError(f"Model not found: {model} (set LLM_MODEL)")
        self.port = _free_port()
        args = [server, "-m", str(model), "--port", str(self.port), "--host", "127.0.0.1",
                "-c", str(context), "--jinja", "-t", str(threads or os.cpu_count() or 4),
                "--no-webui", "-np", "1"]
        self.log = open(Path(os.environ.get("LLM_LOG", "/tmp/llama-server.log")), "w")
        self.proc = subprocess.Popen(args, stdout=self.log, stderr=subprocess.STDOUT)
        self._wait_ready()
        self.model_name = Path(model).stem

    def _wait_ready(self, timeout: int = 600):
        start = time.time()
        while time.time() - start < timeout:
            if self.proc.poll() is not None:
                raise RuntimeError(f"llama-server exited with {self.proc.returncode}; see {self.log.name}")
            try:
                with urllib.request.urlopen(f"http://127.0.0.1:{self.port}/health", timeout=5) as r:
                    if json.load(r).get("status") == "ok":
                        return
            except Exception:
                pass
            time.sleep(2)
        raise TimeoutError("llama-server did not become ready")

    def json(self, system: str, user: str, schema: dict, max_tokens: int = 3000, temperature: float = 0.7) -> dict:
        body = {
            "messages": [
                {"role": "system", "content": system},
                # Qwen3 reasons before answering unless told not to; the grammar needs pure JSON.
                {"role": "user", "content": user + "\n/no_think"},
            ],
            "response_format": {"type": "json_schema", "json_schema": {"name": "output", "schema": inline_refs(schema)}},
            "chat_template_kwargs": {"enable_thinking": False},
            "max_tokens": max_tokens,
            "temperature": temperature,
            "top_p": 0.8,
            "top_k": 20,
            "presence_penalty": 1.0,
        }
        req = urllib.request.Request(
            f"http://127.0.0.1:{self.port}/v1/chat/completions",
            data=json.dumps(body).encode(), headers={"Content-Type": "application/json"},
        )
        t0 = time.time()
        with urllib.request.urlopen(req, timeout=3600) as r:
            data = json.load(r)
        choice = data["choices"][0]
        if choice.get("finish_reason") == "length":
            raise RuntimeError("Model hit max_tokens before finishing the JSON")
        usage = data.get("usage", {})
        print(f"    llm: {usage.get('prompt_tokens')} in / {usage.get('completion_tokens')} out in {time.time() - t0:.0f}s", flush=True)
        content = choice["message"]["content"].strip()
        # Belt and braces in case a template leaves an empty think block in front.
        if content.startswith("<think>"):
            content = content.split("</think>", 1)[-1].strip()
        return json.loads(content)

    def close(self):
        if self.proc.poll() is None:
            self.proc.terminate()
            try:
                self.proc.wait(timeout=20)
            except subprocess.TimeoutExpired:
                self.proc.kill()
        self.log.close()

    def __enter__(self):
        return self

    def __exit__(self, *exc):
        self.close()
