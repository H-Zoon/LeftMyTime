"""Generate the public policy page from the existing policy source."""

from pathlib import Path
from hashlib import sha256

HOSTING = Path(__file__).resolve().parent
SOURCE = HOSTING.parent / "docs" / "privacy.html"
TARGET = HOSTING / "public" / "privacy.html"

body = SOURCE.read_text(encoding="utf-8").strip()
style_version = sha256((HOSTING / "public" / "styles.css").read_bytes()).hexdigest()[:12]
page = """<!doctype html>
<!-- Generated from docs/privacy.html by hosting/sync-privacy.py. -->
<html lang="ko">
  <head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="color-scheme" content="light dark">
    <meta name="description" content="TimeLeft 개인정보처리방침 — 개인정보의 처리 항목과 목적, PointHome 리워드 서비스, 이용자의 권리와 문의 방법을 안내합니다.">
    <title>개인정보처리방침 — TimeLeft</title>
    <link rel="stylesheet" href="/styles.css?v=__STYLE_VERSION__">
  </head>
  <body>
    <div class="page">
      <header class="site-header">
        <a class="brand" href="/" aria-label="TimeLeft 홈">TimeLeft<span class="brand-dot" aria-hidden="true">.</span></a>
        <span class="publisher">by dev-idea</span>
      </header>
      <main class="policy">
__POLICY_BODY__
      </main>
      <footer>
        <span>© 2026 dev-idea</span>
        <a href="/">TimeLeft 홈으로</a>
      </footer>
    </div>
  </body>
</html>
""".replace("__POLICY_BODY__", body).replace("__STYLE_VERSION__", style_version)

TARGET.write_text(page, encoding="utf-8")
print(f"Generated {TARGET}")
