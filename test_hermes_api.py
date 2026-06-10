import requests
import json

BASE = "http://127.0.0.1:9119"
TOKEN = "placeholder_token"
headers = {"X-Hermes-Session-Token": TOKEN}

def get_json(path):
    url = f"{BASE}{path}"
    try:
        r = requests.get(url, headers=headers, timeout=10)
        print(f"GET {url} -> {r.status_code}")
        return r.status_code, r.json() if r.status_code == 200 else r.text
    except Exception as e:
        print(f"GET {url} -> ERROR: {e}")
        return None, str(e)

print("=== 1. List sessions ===")
status, data = get_json("/api/sessions")
if status == 200 and isinstance(data, dict):
    sessions = data.get("sessions", [])
    print(f"Total sessions: {data.get('total')}, returned: {len(sessions)}")
    # Search for placeholder_session_id
    target = "placeholder_session_id"
    found = [s for s in sessions if target in str(s.get("id", ""))]
    print(f"\n=== Searching for '{target}' in all {len(sessions)} sessions ===")
    print(f"Found {len(found)} matches")
    for s in found:
        print(f"  id={s.get('id')!r} title={s.get('title', '')!r} mc={s.get('message_count', 0)}")

    # Also test GET with the target sid directly
    print(f"\n=== Direct GET /api/sessions/{target}/messages ===")
    mstatus, mdata = get_json(f"/api/sessions/{target}/messages")
    print(f"Status: {mstatus}, Data: {mdata}")

    for s in sessions[:5]:
        sid = s.get("id")
        title = s.get("title", "")
        mc = s.get("message_count", 0)
        preview = s.get("preview", "")
        print(f"\n  id={sid!r} title={title!r} message_count={mc} preview={preview!r}")

        print(f"  --- GET /api/sessions/{sid}/messages ---")
        mstatus, mdata = get_json(f"/api/sessions/{sid}/messages")
        if mstatus == 200 and isinstance(mdata, dict):
            msgs = mdata.get("messages", [])
            print(f"  messages count: {len(msgs)}")
            for m in msgs[:3]:
                print(f"    id={m.get('id')} role={m.get('role')!r} content={m.get('content', '')[:60]!r}")
        else:
            print(f"  ERROR: {mdata}")
else:
    print(f"Failed: {data}")
