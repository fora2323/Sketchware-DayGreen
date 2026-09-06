import os
import glob
from pyrogram import Client

api_id = int(os.environ["API_ID"])
api_hash = os.environ["API_HASH"]
session_string = os.environ["SESSION_STRING"]

chat_id = int(os.environ["TELEGRAM_CHAT_ID"])
topic_id = int(os.environ["TELEGRAM_THREAD_ID"])

version = os.environ["VERSION"]
repo = os.environ.get("REPO", "")
author = os.environ.get("COMMIT_AUTHOR", "Unknown")
commit_msg = os.environ.get("COMMIT_MESSAGE", "-").strip()
full_sha = os.environ.get("COMMIT_SHA", "")

short_sha = f"#{full_sha[:7]}" if full_sha else "#unknown"
release_url = f"https://github.com/{repo}/releases/tag/{version}"

all_apks = glob.glob("app/build/outputs/apk/**/release/*.apk", recursive=True)

apk_v33 = None
apk_v26 = None

for apk in all_apks:
    file_name = os.path.basename(apk)
    if "33" in file_name:
        apk_v33 = apk
    elif "26" in file_name:
        apk_v26 = apk

caption_main = (
    f"🚀 **New Release APK ({version})**\n\n"
    f"**Commit by:** {author}\n"
    f"**Commit message:** {commit_msg}\n"
    f"**Commit hash:** {short_sha}\n\n"
    f"🔗 **Release Link:**\n{release_url}"
)

app = Client(
    "userbot_session",
    api_id=api_id,
    api_hash=api_hash,
    session_string=session_string,
    in_memory=True
)

with app:
    if apk_v33:
        print(f"Mengirim APK v33: {apk_v33}")
        app.send_document(
            chat_id=chat_id,
            document=apk_v33,
            caption=f"📱 **Build APK (API 33)**\n\n{caption_main}",
            message_thread_id=topic_id
        )

    if apk_v26:
        print(f"Mengirim APK v26: {apk_v26}")
        app.send_document(
            chat_id=chat_id,
            document=apk_v26,
            caption=f"📱 **Build APK (API 26)**\n\n{caption_main}",
            message_thread_id=topic_id
        )

    if not apk_v33 and not apk_v26:
        for index, apk in enumerate(all_apks):
            print(f"Mengirim APK: {apk}")
            cap = f"📱 **Build APK**\n\n{caption_main}" if index == 0 else "📱 **Build APK**"
            app.send_document(
                chat_id=chat_id,
                document=apk,
                caption=cap,
                message_thread_id=topic_id
            )

print("Semua file APK berhasil diunggah ke topik Telegram!")