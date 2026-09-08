import os
import glob
from pyrogram import Client

api_id = int(os.environ["API_ID"])
api_hash = os.environ["API_HASH"]
session_string = os.environ["SESSION_STRING"]

chat_id = int(os.environ["TELEGRAM_CHAT_ID"])
topic_id = int(os.environ["TELEGRAM_THREAD_ID"])
topic_id2 = int(os.environ["TELEGRAM_THREAD_ID2"])

version = os.environ["VERSION"]
author = os.environ.get("COMMIT_AUTHOR", "Unknown")
commit_msg = os.environ.get("COMMIT_MESSAGE", "-").strip()
full_sha = os.environ.get("COMMIT_SHA", "")

short_sha = f"#{full_sha[:7]}" if full_sha else "#unknown"

all_apks = glob.glob("app/build/outputs/apk/**/debug/*.apk", recursive=True)

apk_v33 = None
for apk in all_apks:
    if "android33" in os.path.basename(apk):
        apk_v33 = apk
        break

if not apk_v33:
    print("APK android33 tidak ditemukan, tidak ada yang dikirim.")
    exit(1)

display_name = "app-debug33.apk"

caption = (
    f"📱 **Debug Build APK (API 33)**\n\n"
    f"🚀 **New Debug Build ({version})**\n\n"
    f"**Commit by:** {author}\n"
    f"**Commit message:** {commit_msg}\n"
    f"**Commit hash:** {short_sha}"
)

app = Client(
    "userbot_session",
    api_id=api_id,
    api_hash=api_hash,
    session_string=session_string,
    in_memory=True
)

with app:
    for tid in (topic_id, topic_id2):
        print(f"Mengirim APK debug android33 ke topic {tid}: {apk_v33}")
        app.send_document(
            chat_id=chat_id,
            document=apk_v33,
            file_name=display_name,
            caption=caption,
            message_thread_id=tid
        )

print("APK debug android33 berhasil diunggah ke kedua topik Telegram!")