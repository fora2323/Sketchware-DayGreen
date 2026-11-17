package org.sketchware.daygreen.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.besome.sketch.lib.base.BaseAppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import dev.aldi.sayuti.editor.manage.LocalLibrariesUtil
import mod.agus.jcoderz.editor.manage.library.locallibrary.ManageLocalLibrary
import pro.sketchware.R
import pro.sketchware.databinding.ActivityProjectDependenciesBinding
import pro.sketchware.utility.ProjectDependencyCache
import pro.sketchware.utility.SketchwareUtil
import pro.sketchware.utility.UI

class ProjectDependenciesActivity : BaseAppCompatActivity() {

    private lateinit var binding: ActivityProjectDependenciesBinding
    private lateinit var projectId: String
    private val gson = Gson()
    private val libraries = ArrayList<HashMap<String, Any>>()
    private lateinit var adapter: DependencyAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdgeNoContrast()
        super.onCreate(savedInstanceState)

        binding = ActivityProjectDependenciesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Scope Library"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        UI.addSystemWindowInsetToPadding(binding.list, false, false, false, true)

        projectId = savedInstanceState?.getString("sc_id")
            ?: intent.getStringExtra("sc_id")
            ?: ""

        if (projectId.isEmpty()) {
            finish()
            return
        }

        adapter = DependencyAdapter()
        binding.list.adapter = adapter
        binding.list.setOnItemLongClickListener { _, _, position, _ ->
            showDeleteDialog(position)
            true
        }

        binding.fab.setOnClickListener {
            showAddDependencyDialog()
        }

        reload()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("sc_id", projectId)
        super.onSaveInstanceState(outState)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(Menu.NONE, 0, Menu.NONE, "Clear cache")
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == 0) {
            ProjectDependencyCache.clear(projectId)
            for (lib in libraries) {
                clearGenerated(lib)
            }
            save()
            reload()
            SketchwareUtil.toast("Dependency cache cleared")
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun reload() {
        libraries.clear()
        libraries.addAll(LocalLibrariesUtil.getLocalLibraries(projectId))
        adapter.notifyDataSetChanged()

        if (libraries.isEmpty()) {
            binding.emptyText.visibility = View.VISIBLE
            binding.list.visibility = View.GONE
        } else {
            binding.emptyText.visibility = View.GONE
            binding.list.visibility = View.VISIBLE
        }
    }

    private fun save() {
        LocalLibrariesUtil.rewriteLocalLibFile(projectId, gson.toJson(libraries))
    }

    private fun clearGenerated(lib: HashMap<String, Any>) {
        if (lib.containsKey("dependency")) {
            lib.remove("jarPath")
            lib.remove("dexPath")
            lib.remove("resPath")
            lib.remove("manifestPath")
            lib.remove("pgRulesPath")
            lib.remove("assetsPath")
            lib.remove("packageName")
        }
    }

    private fun cycleScope(position: Int) {
        if (position !in libraries.indices) return
        val lib = libraries[position]
        val currentScope = ManageLocalLibrary.getScope(lib)
        val nextScope = when (currentScope) {
            ManageLocalLibrary.SCOPE_IMPLEMENTATION -> ManageLocalLibrary.SCOPE_COMPILE_ONLY
            ManageLocalLibrary.SCOPE_COMPILE_ONLY -> ManageLocalLibrary.SCOPE_RUNTIME
            else -> ManageLocalLibrary.SCOPE_IMPLEMENTATION
        }
        lib[ManageLocalLibrary.SCOPE_KEY] = nextScope
        clearGenerated(lib)
        save()
        reload()
        SketchwareUtil.toast("Scope: $nextScope")
    }

    private fun showAddDependencyDialog() {
        val container = FrameLayout(this).apply {
            val horizontalPadding = SketchwareUtil.dpToPx(24)
            val topPadding = SketchwareUtil.dpToPx(12)
            setPadding(horizontalPadding, topPadding, horizontalPadding, 0)
        }
        val input = EditText(this).apply {
            hint = "groupId:artifactId:version"
            isSingleLine = true
        }
        container.addView(input)

        MaterialAlertDialogBuilder(this)
            .setTitle("Add dependency")
            .setView(container)
            .setPositiveButton("Next") { _, _ ->
                val raw = input.text?.toString()?.trim().orEmpty()
                val parts = raw.split(":")
                if (parts.size != 3 || parts.any { it.trim().isEmpty() }) {
                    SketchwareUtil.toastError("Invalid format. Use groupId:artifactId:version")
                    return@setPositiveButton
                }
                val dep = "${parts[0].trim()}:${parts[1].trim()}:${parts[2].trim()}"
                showSelectScopeDialog(dep)
            }
            .setNegativeButton(R.string.common_word_cancel, null)
            .show()
    }

    private fun showSelectScopeDialog(dependency: String) {
        val scopes = arrayOf(
            ManageLocalLibrary.SCOPE_IMPLEMENTATION,
            ManageLocalLibrary.SCOPE_COMPILE_ONLY,
            ManageLocalLibrary.SCOPE_RUNTIME
        )

        MaterialAlertDialogBuilder(this)
            .setTitle("Select scope")
            .setItems(scopes) { _, which ->
                val selectedScope = scopes[which]
                val map = LocalLibrariesUtil.createPendingLibraryMap(dependency, selectedScope, false, 21)
                libraries.add(map)
                save()
                reload()
                SketchwareUtil.toast("Added $dependency ($selectedScope)")
            }
            .setNegativeButton(R.string.common_word_cancel, null)
            .show()
    }

    private fun showDeleteDialog(position: Int) {
        if (position !in libraries.indices) return
        val lib = libraries[position]
        val displayName = (lib["dependency"] as? String) ?: lib["name"]?.toString() ?: "this dependency"

        MaterialAlertDialogBuilder(this)
            .setTitle("Delete dependency")
            .setMessage("Remove $displayName from this project?")
            .setPositiveButton(R.string.common_word_yes) { _, _ ->
                if (position in libraries.indices) {
                    libraries.removeAt(position)
                    save()
                    reload()
                    SketchwareUtil.toast("Deleted")
                }
            }
            .setNegativeButton(R.string.common_word_no, null)
            .show()
    }

    private inner class DependencyAdapter : BaseAdapter() {

        override fun getCount(): Int = libraries.size

        override fun getItem(position: Int): HashMap<String, Any> = libraries[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(this@ProjectDependenciesActivity)
                .inflate(R.layout.item_dependency_row, parent, false)

            val dependencyText = view.findViewById<TextView>(R.id.dependencyText)
            val scopeText = view.findViewById<TextView>(R.id.scopeText)
            val scopeIcon = view.findViewById<ImageView>(R.id.scopeIcon)

            val lib = getItem(position)
            val dependency = lib["dependency"] as? String
            val name = lib["name"]?.toString().orEmpty()
            val scope = ManageLocalLibrary.getScope(lib)
            val isResolved = lib.containsKey("jarPath")

            dependencyText.text = if (!dependency.isNullOrEmpty()) dependency else name
            scopeText.text = when {
                dependency.isNullOrEmpty() -> "$scope (local)"
                isResolved -> scope
                else -> "$scope (pending)"
            }

            scopeIcon.setOnClickListener {
                cycleScope(position)
            }
            view.setOnLongClickListener {
                showDeleteDialog(position)
                true
            }

            return view
        }
    }
}