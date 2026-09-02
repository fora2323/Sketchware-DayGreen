package mod.hilal.saif.activities.android_manifest

import a.a.a.jC
import a.a.a.wB
import a.a.a.yq
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.appcompat.widget.Toolbar
import com.besome.sketch.editor.manage.library.LibraryCategoryView
import com.besome.sketch.editor.manage.library.LibraryItemView
import com.besome.sketch.lib.base.BaseAppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import mod.hey.studios.code.SrcCodeEditor
import mod.hey.studios.project.ProjectSettings
import mod.hey.studios.util.Helper
import mod.hilal.saif.android_manifest.AndroidManifestInjector
import mod.remaker.view.CustomAttributeView
import pro.sketchware.R
import pro.sketchware.activities.editor.view.CodeViewerActivity
import pro.sketchware.databinding.AndroidManifestInjectionBinding
import pro.sketchware.utility.FileUtil
import pro.sketchware.utility.GsonUtils.getGson
import pro.sketchware.utility.SketchwareUtil
import java.io.File

@SuppressLint("SetTextI18n")
class AndroidManifestInjection : BaseAppCompatActivity() {

    private val activitiesListMap = ArrayList<HashMap<String, Any>>()
    private lateinit var binding: AndroidManifestInjectionBinding
    private var sc_id: String = ""
    private var currentActivityName: String = ""
    private lateinit var projectSettings: ProjectSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AndroidManifestInjectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (intent.hasExtra("sc_id") && intent.hasExtra("file_name")) {
            sc_id = intent.getStringExtra("sc_id") ?: ""
            currentActivityName = intent.getStringExtra("file_name")?.replace(".java", "") ?: ""
        }

        projectSettings = ProjectSettings(sc_id)

        setupCustomToolbar()
        setupOptions()
        setupToggleManualEdit()

        binding.btnOpenXmlEditor.setOnClickListener { openManifestXmlEditor() }
        binding.addActivity.setOnClickListener { showAddActivityDialog() }

        checkAttrs()
        refreshList()
    }

    private fun isManualEditEnabled(): Boolean {
        return projectSettings.getValue(ProjectSettings.SETTING_MANIFEST_MANUAL_EDIT_ENABLED, ProjectSettings.SETTING_MANIFEST_MANUAL_EDIT_ENABLED_DEFAULT) == "true"
    }

    private fun setupToggleManualEdit() {
        val isEnabled = isManualEditEnabled()

        binding.toggleManualEdit.isChecked = isEnabled
        updateManualEditUI(isEnabled)

        binding.toggleManualEdit.setOnCheckedChangeListener { _, isChecked ->
            projectSettings.setValue(
                ProjectSettings.SETTING_MANIFEST_MANUAL_EDIT_ENABLED,
                if (isChecked) "true" else "false"
            )
            updateManualEditUI(isChecked)
            refreshList()
        }
    }

    private fun updateManualEditUI(enabled: Boolean) {
        binding.cards.isEnabled = enabled
        binding.content.isEnabled = enabled

        binding.activitiesListView.isEnabled = enabled
        binding.activitiesListView.isClickable = enabled

        binding.cardEditManifest.isEnabled = enabled
        binding.btnOpenXmlEditor.isEnabled = enabled
    }

    private fun ensureXmlHeader(xmlContent: String): String {
        val trimmed = xmlContent.trim()
        return if (!trimmed.startsWith("<?xml")) {
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n$trimmed"
        } else {
            trimmed
        }
    }

    private fun openManifestXmlEditor() {
        val manualManifestPath = "${FileUtil.getExternalStorageDir()}/.sketchware/data/$sc_id/Injection/androidmanifest/manual_manifest.xml"
        val manualDir = File(manualManifestPath).parentFile
        if (manualDir != null && !manualDir.exists()) manualDir.mkdirs()

        if (!FileUtil.isExistFile(manualManifestPath)) {
            val generatedManifest = yq(applicationContext, sc_id).getFileSrc("AndroidManifest.xml", jC.b(sc_id), jC.a(sc_id), jC.c(sc_id))
            val finalContent = ensureXmlHeader(generatedManifest)
            FileUtil.writeFile(manualManifestPath, finalContent)
        } else {
            var content = FileUtil.readFile(manualManifestPath)
            content = ensureXmlHeader(content)
            FileUtil.writeFile(manualManifestPath, content)
        }

        val intent = Intent(this, SrcCodeEditor::class.java)
        intent.putExtra("content", manualManifestPath)
        intent.putExtra("xml", "")
        intent.putExtra("title", "AndroidManifest.xml")
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        checkAttrs()
        refreshList()
    }

    private fun checkAttrs() {
        val path = "${FileUtil.getExternalStorageDir()}/.sketchware/data/$sc_id/Injection/androidmanifest/attributes.json"
        if (FileUtil.isExistFile(path)) {
            val data: ArrayList<HashMap<String, Any>> = getGson().fromJson(FileUtil.readFile(path), Helper.TYPE_MAP_LIST)
            for (i in 0 until data.size) {
                val str = data[i]["name"] as? String
                if (str == "_application_attrs") {
                    val str2 = data[i]["value"] as? String
                    if (str2 != null && str2.contains("android:theme")) {
                        return
                    }
                }
            }
            val item = HashMap<String, Any>()
            item["name"] = "_application_attrs"
            item["value"] = "android:theme=\"@style/AppTheme\""
            data.add(item)
            FileUtil.writeFile(path, getGson().toJson(data))
        }
    }

    private fun setupOptions() {
        val options = ArrayList<LibraryCategoryView>()

        val basicCategoryView = LibraryCategoryView(this)
        basicCategoryView.setTitle(null)
        options.add(basicCategoryView)

        basicCategoryView.addLibraryItem(createOption("Application", "Default properties for the app", R.drawable.ic_mtrl_settings_applications) {
            val intent = Intent()
            intent.setClass(applicationContext, AndroidManifestInjectionDetails::class.java)
            intent.putExtra("sc_id", sc_id)
            intent.putExtra("file_name", currentActivityName)
            intent.putExtra("type", "application")
            startActivity(intent)
        }, true)

        basicCategoryView.addLibraryItem(createOption("Permissions", "Add custom Permissions to the app", R.drawable.ic_mtrl_shield_check) {
            val intent = Intent()
            intent.setClass(applicationContext, AndroidManifestInjectionDetails::class.java)
            intent.putExtra("sc_id", sc_id)
            intent.putExtra("file_name", currentActivityName)
            intent.putExtra("type", "permission")
            startActivity(intent)
        }, true)

        basicCategoryView.addLibraryItem(createOption("Launcher Activity", "Change the default Launcher Activity", R.drawable.ic_mtrl_login) {
            showLauncherActDialog(AndroidManifestInjector.getLauncherActivity(sc_id))
        }, true)

        basicCategoryView.addLibraryItem(createOption("All Activities", "Add attributes for all Activities", R.drawable.ic_mtrl_frame_source) {
            val intent = Intent()
            intent.setClass(applicationContext, AndroidManifestInjectionDetails::class.java)
            intent.putExtra("sc_id", sc_id)
            intent.putExtra("file_name", currentActivityName)
            intent.putExtra("type", "all")
            startActivity(intent)
        }, true)

        basicCategoryView.addLibraryItem(createOption("App Components", "Add extra components", R.drawable.ic_mtrl_component) {
            showAppComponentDialog()
        }, false)

        options.forEach { binding.cards.addView(it) }
    }

    private fun createOption(title: String, description: String, icon: Int, onClick: View.OnClickListener): LibraryItemView {
        val card = LibraryItemView(this)
        makeup(card, icon, title, description)
        card.setOnClickListener(onClick)
        return card
    }

    private fun showAppComponentDialog() {
        val intent = Intent()
        intent.setClass(applicationContext, SrcCodeEditor::class.java)

        val appComponentsPath = "${FileUtil.getExternalStorageDir()}/.sketchware/data/$sc_id/Injection/androidmanifest/app_components.txt"
        if (!FileUtil.isExistFile(appComponentsPath)) FileUtil.writeFile(appComponentsPath, "")
        intent.putExtra("content", appComponentsPath)
        intent.putExtra("xml", "")
        intent.putExtra("disableHeader", "")
        intent.putExtra("title", "App Components")
        startActivity(intent)
    }

    private fun showLauncherActDialog(actnamr: String?) {
        val dialog = MaterialAlertDialogBuilder(this)
        dialog.setIcon(R.drawable.ic_mtrl_lifecycle)
        dialog.setTitle(Helper.getResString(R.string.change_launcher_activity_dialog_title))
        val view = wB.a(this, R.layout.dialog_add_custom_activity)

        val activityNameInput = view.findViewById<TextInputEditText>(R.id.activity_name_input)
        activityNameInput.setText(actnamr)

        dialog.setView(view)
        dialog.setPositiveButton(Helper.getResString(R.string.common_word_save)) { v, _ ->
            if (Helper.getText(activityNameInput).trim().isNotEmpty()) {
                AndroidManifestInjector.setLauncherActivity(sc_id, Helper.getText(activityNameInput))
                SketchwareUtil.toast("Saved")
                v.dismiss()
            } else {
                activityNameInput.error = "Enter activity name"
            }
        }
        dialog.setNegativeButton(Helper.getResString(R.string.common_word_cancel), null)
        dialog.show()
    }

    fun showAddActivityDialog() {
        val dialog = MaterialAlertDialogBuilder(this)
        dialog.setIcon(R.drawable.ic_mtrl_add)
        dialog.setTitle(Helper.getResString(R.string.common_word_add_activtiy))
        val inflate = wB.a(this, R.layout.dialog_add_custom_activity)

        val activityNameInput = inflate.findViewById<TextInputEditText>(R.id.activity_name_input)
        activityNameInput.setText(currentActivityName)

        dialog.setView(inflate)
        dialog.setPositiveButton(Helper.getResString(R.string.common_word_save)) { v, _ ->
            if (Helper.getText(activityNameInput).trim().isNotEmpty()) {
                addNewActivity(Helper.getText(activityNameInput))
                SketchwareUtil.toast("New Activity added")
                v.dismiss()
            } else {
                activityNameInput.error = "Enter activity name"
            }
        }
        dialog.setNegativeButton(Helper.getResString(R.string.common_word_cancel), null)
        dialog.show()
    }

    private fun addNewActivity(componentName: String) {
        val path = "${FileUtil.getExternalStorageDir()}/.sketchware/data/$sc_id/Injection/androidmanifest/attributes.json"
        var data = ArrayList<HashMap<String, Any>>()
        if (FileUtil.isExistFile(path)) {
            data = getGson().fromJson(FileUtil.readFile(path), Helper.TYPE_MAP_LIST)
        }

        val item1 = HashMap<String, Any>()
        item1["name"] = componentName
        item1["value"] = "android:configChanges=\"orientation|screenSize|keyboardHidden|smallestScreenSize|screenLayout\""
        data.add(item1)

        val item2 = HashMap<String, Any>()
        item2["name"] = componentName
        item2["value"] = "android:supportsPictureInPicture=\"true\""
        data.add(item2)

        val item3 = HashMap<String, Any>()
        item3["name"] = componentName
        item3["value"] = "android:screenOrientation=\"portrait\""
        data.add(item3)

        val item4 = HashMap<String, Any>()
        item4["name"] = componentName
        item4["value"] = "android:theme=\"@style/AppTheme\""
        data.add(item4)

        val item5 = HashMap<String, Any>()
        item5["name"] = componentName
        item5["value"] = "android:windowSoftInputMode=\"stateHidden\""
        data.add(item5)

        FileUtil.writeFile(path, getGson().toJson(data))
        refreshList()
    }

    private fun refreshList() {
        activitiesListMap.clear()
        val path = "${FileUtil.getExternalStorageDir()}/.sketchware/data/$sc_id/Injection/androidmanifest/attributes.json"
        val temp = ArrayList<String>()
        val data: ArrayList<HashMap<String, Any>>
        if (FileUtil.isExistFile(path)) {
            data = getGson().fromJson(FileUtil.readFile(path), Helper.TYPE_MAP_LIST)
            for (i in 0 until data.size) {
                val name = data[i]["name"]?.toString() ?: ""
                if (!temp.contains(name)) {
                    if (name != "_application_attrs" && name != "_apply_for_all_activities" && name != "_application_permissions") {
                        temp.add(name)
                    }
                }
            }
            for (i in 0 until temp.size) {
                val map = HashMap<String, Any>()
                map["act_name"] = temp[i]
                activitiesListMap.add(map)
            }
            binding.activitiesListView.adapter = ListAdapter(activitiesListMap)
            (binding.activitiesListView.adapter as BaseAdapter).notifyDataSetChanged()
        }
    }

    private fun deleteActivity(pos: Int) {
        val activityName = activitiesListMap[pos]["act_name"] as String
        val path = "${FileUtil.getExternalStorageDir()}/.sketchware/data/$sc_id/Injection/androidmanifest/attributes.json"
        val data: ArrayList<HashMap<String, Any>> = getGson().fromJson(FileUtil.readFile(path), Helper.TYPE_MAP_LIST)
        for (i in data.size - 1 downTo 0) {
            val temp = data[i]["name"] as? String
            if (temp == activityName) {
                data.removeAt(i)
            }
        }
        FileUtil.writeFile(path, getGson().toJson(data))
        refreshList()
        removeComponents(activityName)
        SketchwareUtil.toast("Activity removed")
    }

    private fun removeComponents(str: String) {
        val path = "${FileUtil.getExternalStorageDir()}/.sketchware/data/$sc_id/Injection/androidmanifest/activities_components.json"
        val data: ArrayList<HashMap<String, Any>>
        if (FileUtil.isExistFile(path)) {
            data = getGson().fromJson(FileUtil.readFile(path), Helper.TYPE_MAP_LIST)
            for (i in data.size - 1 downTo 0) {
                val name = data[i]["name"] as? String
                if (name == str) {
                    data.removeAt(i)
                    break
                }
            }
            FileUtil.writeFile(path, getGson().toJson(data))
        }
    }

    private fun setupCustomToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "AndroidManifest Manager"
        toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(Menu.NONE, Menu.NONE, Menu.NONE, "Show Manifest Source")
            .setIcon(getDrawable(R.drawable.ic_mtrl_code))
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        return true
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.title.toString() == "Show Manifest Source") {
            showQuickManifestSourceDialog()
            return true
        }
        return super.onOptionsItemSelected(menuItem)
    }

    private fun showQuickManifestSourceDialog() {
        k()
        Thread {
            val manualManifestPath = "${FileUtil.getExternalStorageDir()}/.sketchware/data/$sc_id/Injection/androidmanifest/manual_manifest.xml"
            
            var rawSource: String = if (isManualEditEnabled()) {
                if (FileUtil.isExistFile(manualManifestPath)) {
                    FileUtil.readFile(manualManifestPath)
                } else {
                    yq(applicationContext, sc_id).getFileSrc("AndroidManifest.xml", jC.b(sc_id), jC.a(sc_id), jC.c(sc_id))
                }
            } else {
                yq(applicationContext, sc_id).getFileSrc("AndroidManifest.xml", jC.b(sc_id), jC.a(sc_id), jC.c(sc_id))
            }

            rawSource = ensureXmlHeader(rawSource)
            val formattedSource = SrcCodeEditor.prettifyXml(rawSource, 4, null) ?: rawSource

            runOnUiThread {
                if (isFinishing) return@runOnUiThread
                h()
                val intent = Intent(this, CodeViewerActivity::class.java)
                intent.putExtra("code", formattedSource.ifEmpty { "Failed to generate source." })
                intent.putExtra("sc_id", sc_id)
                intent.putExtra("scheme", CodeViewerActivity.SCHEME_XML)
                startActivity(intent)
            }
        }.start()
    }

    private fun makeup(parent: LibraryItemView, icon: Int, title: String, description: String) {
        parent.enabled.visibility = View.GONE
        parent.icon.setImageResource(icon)
        parent.title.text = title
        parent.description.text = description
    }

    private inner class ListAdapter(private val _data: ArrayList<HashMap<String, Any>>) : BaseAdapter() {

        override fun getCount(): Int = _data.size

        override fun getItem(position: Int): HashMap<String, Any> = _data[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val attributeView = CustomAttributeView(parent.context)

            attributeView.imageView.visibility = View.GONE
            attributeView.textView.text = _data[position]["act_name"] as? String
            attributeView.setOnClickListener {
                val intent = Intent()
                intent.setClass(applicationContext, AndroidManifestInjectionDetails::class.java)
                intent.putExtra("sc_id", sc_id)
                intent.putExtra("file_name", _data[position]["act_name"] as? String)
                intent.putExtra("type", "activity")
                startActivity(intent)
            }
            attributeView.setOnLongClickListener {
                val dialog = MaterialAlertDialogBuilder(this@AndroidManifestInjection)
                dialog.setIcon(R.drawable.icon_delete)
                dialog.setTitle(Helper.getResString(R.string.delete_custom_activity_dialog_title))
                dialog.setMessage(Helper.getResString(R.string.delete_custom_activity_dialog_message).replace("%1\$s", _data[position]["act_name"] as String))

                dialog.setPositiveButton(Helper.getResString(R.string.common_word_delete)) { v1, _ ->
                    deleteActivity(position)
                    v1.dismiss()
                }
                dialog.setNegativeButton(Helper.getResString(R.string.common_word_cancel), null)
                dialog.show()
                true
            }

            return attributeView
        }
    }
}