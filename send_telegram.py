import os
import glob
import time
import html
from urllib.parse import quote
from pyrogram import Client
from pyrogram.enums import ParseMode
from pyrogram.types import InputMediaDocument

api_id = int(os.environ["API_ID"])
api_hash = os.environ["API_HASH"]
session_string = os.environ["SESSION_STRING"]

chat_id = int(os.environ["TELEGRAM_CHAT_ID"])
_topic = os.environ.get("TELEGRAM_THREAD_ID", "").strip()
topic_id = int(_topic) if _topic else None

version = os.environ["VERSION"]
tag = os.environ["TAG"]
app_name = os.environ.get("APP_NAME", "Sketchware DayGreen")
package = os.environ.get("APP_PACKAGE", "-")
repo = os.environ["REPO"]
notes_path = os.environ.get("NOTES_FILE", "notes.md")

notes = ""
if os.path.exists(notes_path):
    notes = open(notes_path, encoding="utf-8").read().strip()

def find_apk(flavor):
    files = sorted(glob.glob(f"app/build/outputs/apk/{flavor}/release/*.apk"))
    return files[0] if files else None

apk26 = find_apk("android26")
apk33 = find_apk("android33")

if not apk26 or not apk33:
    print("APK android26 atau android33 tidak ditemukan.")
    exit(1)

def download_url(path):
    name = quote(os.path.basename(path))
    return f"https://github.com/{repo}/releases/download/{quote(tag, safe='')}/{name}"

def build_caption(body):
    head = (
        f"📢 <b>New update available for {html.escape(app_name)}</b>\n\n"
        f"• <b>App name:</b> {html.escape(app_name)}\n"
        f"• <b>Package:</b> <code>{html.escape(package)}</code>\n"
        f"• <b>Version:</b> <code>{html.escape(version)}</code>\n"
        f"• <b>Minimum SDK:</b> 26 / 33\n\n"
        f"• <b>Download (SDK 26):</b> <a href=\"{download_url(apk26)}\">{html.escape(os.path.basename(apk26))}</a>\n"
        f"• <b>Download (SDK 33):</b> <a href=\"{download_url(apk33)}\">{html.escape(os.path.basename(apk33))}</a>\n"
        f"• <b>Repository:</b> <a href=\"https://github.com/{repo}\">{html.escape(repo)}</a>\n\n"
        f"• <b>What's new?</b>\n<blockquote>"
    )
    tail = "</blockquote>"
    limit = 1024
    room = limit - len(head) - len(tail)
    escaped = html.escape(body)
    if len(escaped) > room:
        escaped = escaped[:max(0, room - 3)] + "..."
    return head + escaped + tail

caption = build_caption(notes)

def make_client():
    return Client(
        "userbot_session",
        api_id=api_id,
        api_hash=api_hash,
        session_string=session_string,
        in_memory=True,
        max_concurrent_transmissions=1,
    )

MAX_TRIES = 5
sent = False
for attempt in range(1, MAX_TRIES + 1):
    try:
        with make_client() as app:
            tujuan = f"topik {topic_id}" if topic_id else "group"
            print(f"[{attempt}/{MAX_TRIES}] Mengirim APK SDK 26 & 33 ke {tujuan}")
            app.send_media_group(
                chat_id=chat_id,
                media=[
                    InputMediaDocument(apk26, caption=caption, parse_mode=ParseMode.HTML),
                    InputMediaDocument(apk33),
                ],
                message_thread_id=topic_id,
            )
        sent = True
        break
    except BaseException as e:
        if isinstance(e, KeyboardInterrupt):
            raise
        print(f"Percobaan {attempt} gagal: {type(e).__name__}: {e}")
        time.sleep(10 * attempt)

if not sent:
    print("Gagal mengirim APK setelah semua percobaan.")
    exit(1)

print("Rilis berhasil dikirim ke Telegram!")