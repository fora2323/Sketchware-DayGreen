package mod.hilal.saif.activities.android_manifest

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.TextView
import com.besome.sketch.lib.base.BaseAppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import mod.hey.studios.code.SrcCodeEditor
import mod.hey.studios.util.Helper
import mod.remaker.view.CustomAttributeView
import pro.sketchware.R
import pro.sketchware.databinding.ActivityManageCustomAttributeBinding
import pro.sketchware.databinding.CustomDialogAttributeBinding
import pro.sketchware.databinding.DialogCreateNewFileLayoutBinding
import pro.sketchware.utility.FileUtil
import pro.sketchware.utility.GsonUtils.getGson
import pro.sketchware.utility.SketchwareUtil.getDip
import pro.sketchware.utility.ThemeUtils
import java.util.ArrayList
import java.util.HashMap

class AndroidManifestInjectionDetails : BaseAppCompatActivity() {

    private val listMap = ArrayList<HashMap<String, Any?>>()
    private var attributesFilePath: String? = null
    private var src_id: String? = null
    private var activityName: String? = null
    private var type: String? = null
    private var constant: String? = null

    private lateinit var binding: ActivityManageCustomAttributeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageCustomAttributeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (intent.hasExtra("sc_id") && intent.hasExtra("file_name") && intent.hasExtra("type")) {
            src_id = intent.getStringExtra("sc_id")
            activityName = intent.getStringExtra("file_name")?.replace(".java", "")
            type = intent.getStringExtra("type")
        }
        attributesFilePath = FileUtil.getExternalStorageDir() + "/.sketchware/data/" + src_id + "/Injection/androidmanifest/attributes.json"

        setupConst()
        setToolbar()
        setupViews()
    }

    private fun setupConst() {
        constant = when (type) {
            "all" -> "_apply_for_all_activities"
            "application" -> "_application_attrs"
            "permission" -> "_application_permissions"
            else -> activityName
        }
    }

    private fun setupViews() {
        binding.addAttrFab.setOnClickListener { showAddDial() }
        refreshList()
    }

    private fun refreshList() {
        listMap.clear()
        if (FileUtil.isExistFile(attributesFilePath)) {
            val data: ArrayList<HashMap<String, Any?>>? = getGson().fromJson(
                FileUtil.readFile(attributesFilePath),
                Helper.TYPE_MAP_LIST
            )
            data?.let {
                for (item in it) {
                    val str = item["name"] as? String
                    if (str == constant) {
                        listMap.add(item)
                    }
                }
            }
            binding.addAttrListview.adapter = ListAdapter(listMap)
            (binding.addAttrListview.adapter as? BaseAdapter)?.notifyDataSetChanged()
        }
    }

    private fun setToolbar() {
        val str = when (type) {
            "all" -> "Attributes for all activities"
            "application" -> "Application Attributes"
            "permission" -> "Application Permissions"
            else -> activityName ?: ""
        }
        binding.toolbar.title = str
        binding.toolbar.setNavigationOnClickListener(Helper.getBackPressedClickListener(this))

        if (str != "Attributes for all activities" && str != "Application Attributes" && str != "Application Permissions") {
            binding.toolbar.setOnMenuItemClickListener { item ->
                if (item.itemId == R.id.asd_components) {
                    val intent = Intent(this, SrcCodeEditor::class.java).apply {
                        putExtra(SrcCodeEditor.FLAG_FROM_ANDROID_MANIFEST, true)
                        putExtra("title", "$activityName Components")
                        putExtra("sc_id", src_id)
                        putExtra("activity_name", activityName)
                    }
                    startActivity(intent)
                    true
                } else {
                    false
                }
            }
        } else {
            binding.toolbar.menu.clear()
        }
    }

    private fun showDial(pos: Int) {
        val dialog = MaterialAlertDialogBuilder(this)
        dialog.setTitle("Edit Value")
        val attributeBinding = DialogCreateNewFileLayoutBinding.inflate(layoutInflater)
        attributeBinding.chipGroupTypes.visibility = View.GONE
        dialog.setView(attributeBinding.root)

        attributeBinding.inputText.setText(listMap[pos]["value"] as? String)
        attributeBinding.inputText.hint = "android:attr=\"value\""
        dialog.setPositiveButton(R.string.common_word_save) { _, _ ->
            listMap[pos]["value"] = Helper.getText(attributeBinding.inputText)
            applyChange()
        }

        dialog.show()
    }

    private fun showAddDial() {
        val dialog = MaterialAlertDialogBuilder(this)
        dialog.setTitle(if (type == "permission") "Add new permission" else "Add new attribute")
        val attributeBinding = CustomDialogAttributeBinding.inflate(layoutInflater)
        dialog.setView(attributeBinding.root)
        if (type == "permission") {
            attributeBinding.inputRes.setText("android")
            attributeBinding.inputAttr.setText("name")
            attributeBinding.inputLayoutValue.hint = "permission"
        }
        dialog.setPositiveButton(R.string.common_word_save) { dialog1, _ ->
            val fstr = "${Helper.getText(attributeBinding.inputRes).trim()}:${Helper.getText(attributeBinding.inputAttr).trim()}=\"${Helper.getText(attributeBinding.inputValue).trim()}\""
            val map = HashMap<String, Any?>()
            map["name"] = constant
            map["value"] = fstr
            listMap.add(map)
            applyChange()
            dialog1.dismiss()
        }
        dialog.setNegativeButton(R.string.common_word_cancel) { dialog1, _ -> dialog1.dismiss() }
        dialog.show()
    }

    private fun applyChange() {
        val data: ArrayList<HashMap<String, Any?>>
        if (FileUtil.isExistFile(attributesFilePath)) {
            data = getGson().fromJson(
                FileUtil.readFile(attributesFilePath),
                Helper.TYPE_MAP_LIST
            ) ?: ArrayList()
            for (i in data.size - 1 downTo 0) {
                val str = data[i]["name"] as? String
                if (str == constant) {
                    data.removeAt(i)
                }
            }
            data.addAll(listMap)
        } else {
            data = ArrayList(listMap)
        }
        FileUtil.writeFile(attributesFilePath, getGson().toJson(data))
        refreshList()
    }

    private fun newText(str: String, size: Float, color: Int, width: Int, height: Int, weight: Float): TextView {
        val tempCard = TextView(this)
        tempCard.layoutParams = LinearLayout.LayoutParams(width, height, weight)
        tempCard.setPadding(getDip(4).toInt(), getDip(4).toInt(), getDip(4).toInt(), getDip(4).toInt())
        tempCard.setTextColor(color)
        tempCard.text = str
        tempCard.textSize = size
        return tempCard
    }

    private inner class ListAdapter(private val _data: ArrayList<HashMap<String, Any?>>) : BaseAdapter() {

        override fun getCount(): Int {
            return _data.size
        }

        override fun getItem(position: Int): HashMap<String, Any?> {
            return _data[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val attributeView = CustomAttributeView(parent.context)

            try {
                val violet = ThemeUtils.getColor(attributeView, R.attr.colorViolet)
                val onSurface = ThemeUtils.getColor(attributeView, R.attr.colorOnSurface)
                val green = ThemeUtils.getColor(attributeView, R.attr.colorGreen)

                val valStr = _data[position]["value"] as String
                val spannableString = SpannableString(valStr)
                spannableString.setSpan(ForegroundColorSpan(violet), 0, valStr.indexOf(":"), 33)
                spannableString.setSpan(ForegroundColorSpan(onSurface), valStr.indexOf(":"), valStr.indexOf("=") + 1, 33)
                spannableString.setSpan(ForegroundColorSpan(green), valStr.indexOf("\""), valStr.length, 33)
                attributeView.textView.text = spannableString
            } catch (e: Exception) {
                attributeView.textView.text = _data[position]["value"] as? String
            }

            attributeView.imageView.visibility = View.GONE
            attributeView.setOnClickListener { showDial(position) }
            attributeView.setOnLongClickListener {
                MaterialAlertDialogBuilder(this@AndroidManifestInjectionDetails)
                    .setTitle("Delete this attribute?")
                    .setMessage("This action cannot be undone.")
                    .setPositiveButton(R.string.common_word_delete) { _, _ ->
                        listMap.removeAt(position)
                        applyChange()
                    }
                    .setNegativeButton(R.string.common_word_cancel, null)
                    .show()
                true
            }

            return attributeView
        }
    }
}