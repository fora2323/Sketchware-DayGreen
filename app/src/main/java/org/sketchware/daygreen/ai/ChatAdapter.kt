package org.sketchware.daygreen.ai

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import pro.sketchware.R
import pro.sketchware.utility.SketchwareUtil
import java.util.Locale
import java.util.regex.Pattern

class ChatAdapter(
    private val formatter: ChatTextFormatter,
    private val onEventClick: ChatTextFormatter.OnEventClick
) : RecyclerView.Adapter<ChatAdapter.Holder>() {

    companion object {
        private const val TYPE_USER = 0
        private const val TYPE_AGENT = 1
        private const val TYPE_STATUS = 2
        private const val TYPE_THINKING = 3

        private val FENCE = Pattern.compile("```([A-Za-z0-9_+#.\\-:\\s]*?)\\n([\\s\\S]*?)```")
    }

    class ChatMessage private constructor(
        val type: Int,
        val text: String,
        var expanded: Boolean = false
    ) {
        companion object {
            fun user(text: String) = ChatMessage(TYPE_USER, text)
            fun agent(text: String) = ChatMessage(TYPE_AGENT, text)
            fun status(text: String) = ChatMessage(TYPE_STATUS, text)
            fun thinking(text: String) = ChatMessage(TYPE_THINKING, text)
        }
    }

    private val items = mutableListOf<ChatMessage>()

    fun add(message: ChatMessage) {
        items.add(message)
        notifyItemInserted(items.size - 1)
    }

    fun updateStatus(text: String) {
        removeStatus()
        add(ChatMessage.status(text))
    }

    fun removeStatus() {
        for (i in items.indices.reversed()) {
            if (items[i].type == TYPE_STATUS) {
                items.removeAt(i)
                notifyItemRemoved(i)
            }
        }
    }

    override fun getItemViewType(position: Int): Int = items[position].type

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val layout = when (viewType) {
            TYPE_USER -> R.layout.item_chat_user
            TYPE_STATUS -> R.layout.item_chat_status
            TYPE_THINKING -> R.layout.item_chat_thinking
            else -> R.layout.item_chat_agent
        }
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        when (item.type) {
            TYPE_USER -> holder.text?.text = item.text
            TYPE_AGENT -> bindAgent(holder, item)
            TYPE_STATUS -> holder.text?.text = item.text
            TYPE_THINKING -> bindThinking(holder, item)
        }
    }

    private fun bindAgent(holder: Holder, item: ChatMessage) {
        val parts = holder.parts ?: return
        parts.removeAllViews()
        val inflater = LayoutInflater.from(parts.context)

        for (seg in splitSegments(item.text)) {
            if (seg.isCode) {
                parts.addView(buildCodePart(inflater, parts, seg.lang, seg.text))
            } else {
                val plain = seg.text.trim()
                if (plain.isNotEmpty()) {
                    parts.addView(buildTextPart(inflater, parts, plain))
                }
            }
        }
    }

    private fun buildTextPart(inflater: LayoutInflater, parent: ViewGroup, text: String): View {
        val view = inflater.inflate(R.layout.item_chat_text, parent, false)
        val tv = view.findViewById<TextView>(R.id.tv_message)
        tv.text = formatter.format(text, onEventClick)
        tv.movementMethod = LinkMovementMethod.getInstance()
        return view
    }

    private fun buildCodePart(inflater: LayoutInflater, parent: ViewGroup, rawLang: String?, code: String): View {
        val view = inflater.inflate(R.layout.item_chat_code, parent, false)
        var lang = rawLang?.trim() ?: ""
        if (lang.contains(" ") || lang.contains(":")) {
            val parts = lang.split(Regex("[:\\s]+"), 2)
            lang = parts[0].trim()
        }
        val displayLang = if (lang.isEmpty()) "code" else lang.lowercase(Locale.ROOT)

        val langTv = view.findViewById<TextView>(R.id.tv_lang)
        langTv?.text = displayLang

        val codeTv = view.findViewById<TextView>(R.id.tv_code)
        codeTv.text = ChatCodeHighlighter.highlight(code)

        val copy = view.findViewById<ImageButton>(R.id.btn_copy)
        copy.setOnClickListener { v ->
            val ctx = v.context
            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText(displayLang, code))
                SketchwareUtil.toast("Code copied to clipboard")
            }
        }
        return view
    }

    private fun bindThinking(holder: Holder, item: ChatMessage) {
        holder.title?.text = "Reasoning (${if (item.expanded) "hide" else "show"})"
        holder.chevron?.rotation = if (item.expanded) 180f else 0f
        holder.body?.text = item.text
        holder.body?.visibility = if (item.expanded) View.VISIBLE else View.GONE

        val toggle = View.OnClickListener {
            val pos = holder.adapterPosition
            if (pos == RecyclerView.NO_POSITION) return@OnClickListener
            item.expanded = !item.expanded
            notifyItemChanged(pos)
        }

        if (holder.thinkingHeader != null) {
            holder.thinkingHeader.setOnClickListener(toggle)
        } else {
            holder.title?.setOnClickListener(toggle)
        }
    }

    class Segment private constructor(
        val isCode: Boolean,
        val lang: String,
        val text: String
    ) {
        companion object {
            fun plain(text: String) = Segment(false, "", text)
            fun code(lang: String, text: String) = Segment(true, lang, text)
        }
    }

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val text: TextView? = itemView.findViewById(R.id.tv_message)
        val parts: LinearLayout? = itemView.findViewById(R.id.ll_parts)
        val thinkingHeader: View? = itemView.findViewById(R.id.ll_thinking_header)
        val chevron: ImageView? = itemView.findViewById(R.id.iv_thinking_chevron)
        val title: TextView? = itemView.findViewById(R.id.tv_thinking_title)
        val body: TextView? = itemView.findViewById(R.id.tv_thinking_body)

        init {
            text?.movementMethod = LinkMovementMethod.getInstance()
        }
    }

    private fun splitSegments(text: String): List<Segment> {
        val out = mutableListOf<Segment>()
        val m = FENCE.matcher(text)
        var last = 0
        while (m.find()) {
            if (m.start() > last) {
                out.add(Segment.plain(text.substring(last, m.start())))
            }
            val code = m.group(2)!!.replace(Regex("\\n+$"), "")
            out.add(Segment.code(m.group(1) ?: "", code))
            last = m.end()
        }
        if (last < text.length) {
            out.add(Segment.plain(text.substring(last)))
        }
        return out
    }
}
