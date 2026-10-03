"""Runs an open-weights model locally with llama.cpp's llama-server and asks it for JSON.

Default model: Gemma 4 26B-A4B (Apache-2.0), a mixture-of-experts model with 25B weights but
only ~4B active per token, quantised to ~4 bits (unsloth UD-IQ4_XS, 13.6 GB). It fits a GitHub
Actions runner's 16 GB, needs no GPU or API key, and on the runner's 4 CPUs it is several times
faster than a dense 14B model while writing better and staying as faithful to its sources.
Output is constrained to a JSON schema by llama.cpp's grammar sampler, so every response parses.

Sampling defaults and template quirks are chosen per model family from the file name
(see PROFILES), so LLM_MODEL can still point at a Qwen3 GGUF.
"""
from __future__ import annotations

import json
import os
import socket
import subprocess
import time
import urllib.request
from pathlib import Path

DEFAULT_MODEL = Path(os.environ.get("LLM_MODEL", Path.home() / "models" / "gemma-4-26B-A4B-it-UD-IQ4_XS.gguf"))
DEFAULT_SERVER = os.environ.get("LLAMA_SERVER", "llama-server")

# Per-family settings, from each model card's recommended (non-thinking) sampling.
PROFILES = {
    # https://huggingface.co/google/gemma-4-26B-A4B-it recommends temperature 1.0, top_p 0.95,
    # top_k 64. We write a little cooler (0.8) because sticking to the source's facts matters
    # more here than variety; judging calls (fact-check, ranking) pass their own low value.
    # Thinking is off unless the system prompt starts with <|think|>; with
    # enable_thinking=false the chat template pre-fills an empty thought channel, so the
    # grammar-constrained answer starts straight away. No presence penalty: it would
    # also punish the JSON keys every line repeats.
    "gemma": {"sampling": {"temperature": 0.8, "top_p": 0.95, "top_k": 64, "min_p": 0.0},
              "no_think_suffix": ""},
    # https://huggingface.co/Qwen/Qwen3-14B: non-thinking temperature 0.7, top_p 0.8, top_k 20.
    "qwen": {"sampling": {"temperature": 0.7, "top_p": 0.8, "top_k": 20, "min_p": 0.0, "presence_penalty": 1.0},
             "no_think_suffix": "\n/no_think"},
}


def profile_for(model: Path) -> dict:
    name = Path(model).name.lower()
    return PROFILES["gemma"] if "gemma" in name else PROFILES["qwen"]


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
        self.profile = profile_for(model)

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

    def json(self, system: str, user: str, schema: dict, max_tokens: int = 3000,
             temperature: float | None = None) -> dict:
        """temperature=None uses the model card's recommendation; pass a low value for judging tasks."""
        sampling = dict(self.profile["sampling"])
        if temperature is not None:
            sampling["temperature"] = temperature
        body = {
            "messages": [
                {"role": "system", "content": system},
                # Thinking is switched off: the grammar needs the answer to be pure JSON
                # (Qwen3 also wants the /no_think soft switch in the user turn).
                {"role": "user", "content": user + self.profile["no_think_suffix"]},
            ],
            "response_format": {"type": "json_schema", "json_schema": {"name": "output", "schema": inline_refs(schema)}},
            "chat_template_kwargs": {"enable_thinking": False},
            "max_tokens": max_tokens,
            **sampling,
            # DRY discourages verbatim repetition; JSON punctuation breaks sequences so structure is unaffected.
            "dry_multiplier": 0.8,
            "dry_base": 1.75,
            "dry_allowed_length": 4,
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
        timings = data.get("timings", {})
        speed = (f" (prompt {timings['prompt_per_second']:.1f} tok/s, generation {timings['predicted_per_second']:.1f} tok/s)"
                 if timings.get("prompt_per_second") and timings.get("predicted_per_second") else "")
        print(f"    llm: {usage.get('prompt_tokens')} in / {usage.get('completion_tokens')} out in {time.time() - t0:.0f}s{speed}", flush=True)
        content = choice["message"]["content"].strip()
        # Belt and braces in case a template leaves an empty thinking block in front.
        for opener, closer in (("<think>", "</think>"), ("<|channel>", "<channel|>")):
            if content.startswith(opener):
                content = content.split(closer, 1)[-1].strip()
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
