import os
import glob
import time
from pyrogram import Client

api_id = int(os.environ["API_ID"])
api_hash = os.environ["API_HASH"]
session_string = os.environ["SESSION_STRING"]

chat_id = int(os.environ["TELEGRAM_CHAT_ID"])
_topic = os.environ.get("TELEGRAM_THREAD_ID2", "").strip()
topic_id2 = int(_topic) if _topic else None   # None = group biasa, tanpa topik

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

id_file = "log_msg_id.txt"
log_msg_id = None
if os.path.exists(id_file):
    try:
        log_msg_id = int(open(id_file).read().strip())
    except Exception:
        pass


def make_client():
    # Client baru tiap percobaan supaya koneksi yang timeout tidak terbawa
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
            tujuan = f"topik {topic_id2}" if topic_id2 else "group"
            print(f"[{attempt}/{MAX_TRIES}] Mengirim APK debug android33 ke {tujuan}: {apk_v33}")
            app.send_document(
                chat_id=chat_id,
                document=apk_v33,
                file_name=display_name,
                caption=caption,
                message_thread_id=topic_id2,
            )
        sent = True
        break
    except BaseException as e:
        if isinstance(e, KeyboardInterrupt):
            raise
        print(f"Percobaan {attempt} gagal: {type(e).__name__}: {e}")
        time.sleep(10 * attempt)

if not sent:
    # Beri tahu lewat pesan log, APK tetap ada di GitHub Artifacts
    if log_msg_id:
        try:
            with make_client() as app:
                app.edit_message_text(
                    chat_id, log_msg_id,
                    f"❌ Build #{version} sukses, tapi gagal kirim APK ke Telegram.\n"
                    f"APK ada di GitHub Actions > Artifacts."
                )
        except BaseException as e:
            print(f"Gagal update pesan log: {e}")
    print("Gagal mengirim APK setelah semua percobaan.")
    exit(1)

print("APK debug android33 berhasil diunggah!")

# Hapus pesan log live setelah APK terkirim
if log_msg_id:
    try:
        with make_client() as app:
            app.delete_messages(chat_id, log_msg_id)
        print("Pesan log live dihapus.")
    except BaseException as e:
        print(f"Gagal menghapus pesan log: {e}")