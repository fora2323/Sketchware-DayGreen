package extensions.anbui.daydream.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast

object TextUtils {
    @JvmStatic
    fun isSingleLine(text : String) : Boolean {
        val lines = text.lines().filter { it.isNotBlank() }
        return lines.size == 1
    }

    @JvmStatic
    fun isNumberOnly(content: String): Boolean {
        //Includes do not accept empty strings.
        return content.matches("\\d+".toRegex())
    }

    @JvmStatic
    fun isValidInteger(content: String): Boolean {
        return isNumberOnly(content) && content.toIntOrNull() != null
    }

    @JvmStatic
    fun isValidFloat(content: String?): Boolean {
        if (content.isNullOrEmpty() || !isNumberOnly(content.trim().lowercase().replace(".", "").removeSuffix("f"))) return false
        val finalnumber = content.trim().lowercase().removeSuffix("f")
        return finalnumber.toFloatOrNull() != null
    }

    @JvmStatic
    fun copyToClipboard(context: Context, text: String?) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("From Sketchware DayGreen", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied.", Toast.LENGTH_SHORT).show()
    }

    @JvmStatic
    fun countACharacter(text: String, characterToCount: Char): Int {
        return text.count { it == characterToCount }
    }

    @JvmStatic
    fun countACharacterInsideCharacterPairs(text: String, characterToCount: Char, characterPair: String): Int {
        val regex = "${characterPair}([^${characterPair}]*)${characterPair}".toRegex()

        var total = 0
        regex.findAll(text).forEach { match ->
            val content = match.groupValues[1]
            total += content.count { it == characterToCount }
        }
        return total
    }

    @JvmStatic
    fun countACharacterOutsideCharacterPairs(text: String, characterToCount: Char, characterPair: String): Int {
        val regex = "${characterPair}([^${characterPair}]*)${characterPair}".toRegex()

        val total = countACharacter(text, characterToCount)
        var totalInside = 0
        regex.findAll(text).forEach { match ->
            val content = match.groupValues[1]
            totalInside += content.count { it == characterToCount }
        }
        return total - totalInside
    }
}