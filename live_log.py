import os, sys, time, subprocess, re
from pyrogram import Client

app = Client("userbot_session", api_id=int(os.environ["API_ID"]),
             api_hash=os.environ["API_HASH"],
             session_string=os.environ["SESSION_STRING"], in_memory=True)

chat_id   = int(os.environ["TELEGRAM_CHAT_ID"])
_thread   = os.environ.get("LOG_THREAD_ID", "").strip()
thread    = int(_thread) if _thread else None   # None = group biasa, tanpa topik
title     = os.environ.get("LOG_TITLE", "Build")
cmd       = sys.argv[1:]
LOG       = "build.log"
ID_FILE   = "log_msg_id.txt"      # dibaca send_telegram_debug.py untuk hapus pesan log
INTERVAL  = 5
MAX_LINES = 25

KEEP = re.compile(r"(> Task|error:|warning:|FAILED|BUILD|\.java:\d+|\.kt:\d+|What went wrong)")

def render(lines, status):
    body = "\n".join(lines[-MAX_LINES:])[-3500:]
    return f"**{title}** — {status}\n```\n{body}\n```"

with app:
    msg = app.send_message(chat_id, render(["Memulai build..."], "⏳ berjalan"),
                           message_thread_id=thread)
    with open(ID_FILE, "w") as f:
        f.write(str(msg.id))

    tail, last, last_text = [], 0, ""
    with open(LOG, "w") as f:
        p = subprocess.Popen(cmd, stdout=subprocess.PIPE,
                             stderr=subprocess.STDOUT, text=True, bufsize=1)
        for line in p.stdout:
            print(line, end="")
            f.write(line)
            line = line.rstrip()
            if KEEP.search(line):
                tail.append(line[:200])
            if time.time() - last > INTERVAL and tail:
                text = render(tail, "⏳ berjalan")
                if text != last_text:
                    try: msg.edit_text(text)
                    except Exception: pass
                    last_text, last = text, time.time()
        code = p.wait()

    if code == 0:
        # sukses: pesan log dibiarkan dulu, nanti dihapus setelah APK terkirim
        try: msg.edit_text(render(tail, "✅ sukses, mengirim APK..."))
        except Exception: pass
    else:
        # gagal: pesan log dipertahankan + kirim file log lengkap
        try: msg.edit_text(render(tail, "❌ gagal"))
        except Exception: pass
        app.send_document(chat_id, LOG, caption="Log lengkap build gagal",
                          message_thread_id=thread)
        try: os.remove(ID_FILE)
        except Exception: pass
sys.exit(code)