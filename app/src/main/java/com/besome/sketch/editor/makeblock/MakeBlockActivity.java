package com.besome.sketch.editor.makeblock;

import android.content.Intent;
import android.os.Bundle;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import com.besome.sketch.beans.BlockBean;
import com.besome.sketch.beans.ProjectFileBean;
import com.besome.sketch.lib.base.BaseAppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.HashMap;

import a.a.a.FB;
import a.a.a.bB;
import a.a.a.eC;
import a.a.a.jC;
import a.a.a.mB;
import a.a.a.mq;
import mod.hey.studios.moreblock.ReturnMoreblockManager;
import mod.hey.studios.util.Helper;
import pro.sketchware.R;

public class MakeBlockActivity extends BaseAppCompatActivity {

    private String sc_id;
    private ProjectFileBean project;
    private MoreBlockBuilderView makeBlock;
    private boolean isEdit;
    private String oldBlockName;
    private String oldBlockSpec;

    private void goBackDialog() {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        dialog.setTitle(Helper.getResString(R.string.logic_editor_more_block_dialog_message_confirm_goback));
        dialog.setIcon(R.drawable.exit_96);
        dialog.setMessage(Helper.getResString(R.string.logic_editor_more_block_dialog_description_goback));
        dialog.setPositiveButton(Helper.getResString(R.string.common_word_goback), (v, which) -> {
            if (!mB.a()) {
                v.dismiss();
                finish();
            }

        });
        dialog.setNegativeButton(Helper.getResString(R.string.common_word_cancel), null);
        dialog.show();
    }

    @Override
    public void onBackPressed() {
        if (isEdit) {
            if (makeBlock.a()) {
                super.onBackPressed();
                return;
            }
            Pair<String, String> info = makeBlock.getBlockInformation();
            if (info.first.equals(oldBlockName) && info.second.equals(oldBlockSpec)) {
                super.onBackPressed();
            } else {
                goBackDialog();
            }
        } else if (makeBlock.a()) {
            super.onBackPressed();
        } else {
            goBackDialog();
        }

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        enableEdgeToEdgeNoContrast();
        super.onCreate(savedInstanceState);
        if (!isStoragePermissionGranted()) {
            finish();
            return;
        }

        setContentView(R.layout.make_block);
        if (savedInstanceState == null) {
            sc_id = getIntent().getStringExtra("sc_id");
            project = getIntent().getParcelableExtra("project_file");
            isEdit = getIntent().getBooleanExtra("is_edit", false);
            oldBlockName = getIntent().getStringExtra("old_block_name");
            oldBlockSpec = getIntent().getStringExtra("old_block_spec");
        } else {
            sc_id = savedInstanceState.getString("sc_id");
            project = savedInstanceState.getParcelable("project_file");
            isEdit = savedInstanceState.getBoolean("is_edit", false);
            oldBlockName = savedInstanceState.getString("old_block_name");
            oldBlockSpec = savedInstanceState.getString("old_block_spec");
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        findViewById(R.id.layout_main_logo).setVisibility(View.GONE);
        if (getSupportActionBar() != null) {
            if (isEdit) {
                getSupportActionBar().setTitle(getString(R.string.common_word_edit) + " " + getString(R.string.common_word_moreblock));
            } else {
                getSupportActionBar().setTitle(Helper.getResString(R.string.logic_editor_more_block_actionbar_title_create_more_block));
            }
            if (project != null) {
                getSupportActionBar().setSubtitle(project.getJavaName());
            }
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
        }
        toolbar.setNavigationOnClickListener(Helper.getBackPressedClickListener(this));
        makeBlock = new MoreBlockBuilderView(this);
        ArrayList<String> existingNames = new ArrayList<>(jC.a(sc_id).a(project));
        if (isEdit && oldBlockName != null) {
            existingNames.remove(oldBlockName);
            existingNames.remove(ReturnMoreblockManager.getMbName(oldBlockName));
        }
        makeBlock.setFuncNameValidator(existingNames);
        if (isEdit && oldBlockName != null) {
            makeBlock.loadFromExisting(oldBlockName, oldBlockSpec);
        }
        LinearLayout makeBlockContainer = findViewById(R.id.makeblock_view);
        handleInsetts(makeBlockContainer);
        makeBlockContainer.addView(this.makeBlock);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.moreblock_menu, menu);
        menu.findItem(R.id.moreblock_create).setTitle(Helper.getResString(isEdit ? R.string.common_word_save : R.string.common_word_create));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.moreblock_create) {
            if (makeBlock.a()) return false;

            if (makeBlock.b()) {
                Pair<String, String> blockInformation = makeBlock.getBlockInformation();
                if (isEdit) {
                    updateMoreBlock(oldBlockName, oldBlockSpec, blockInformation.first, blockInformation.second);
                }
                Intent intent = new Intent();
                intent.putExtra("block_name", blockInformation.first);
                intent.putExtra("block_spec", blockInformation.second);
                setResult(RESULT_OK, intent);
                finish();
                return true;
            }
        }

        return super.onOptionsItemSelected(menuItem);
    }

    private void updateMoreBlock(String oldName, String oldSpec, String newName, String newSpec) {
        if (project == null || oldName == null) {
            return;
        }
        eC logicManager = jC.a(sc_id);
        String javaName = project.getJavaName();

        ArrayList<Pair<String, String>> moreBlocks = logicManager.i(javaName);
        for (int i = 0; i < moreBlocks.size(); i++) {
            if (moreBlocks.get(i).first.equals(oldName)) {
                moreBlocks.set(i, new Pair<>(newName, newSpec));
                break;
            }
        }

        HashMap<String, ArrayList<BlockBean>> blocksMap = logicManager.b(javaName);
        String oldKey = oldName + "_moreBlock";
        String newKey = newName + "_moreBlock";
        if (!oldKey.equals(newKey) && blocksMap.containsKey(oldKey)) {
            ArrayList<BlockBean> moreBlockLogic = blocksMap.remove(oldKey);
            blocksMap.put(newKey, moreBlockLogic);
        }

        String oldCallerSpec = ReturnMoreblockManager.getMbName(oldSpec != null ? oldSpec : oldName);
        String newCallerSpec = ReturnMoreblockManager.getMbName(newSpec);
        String newMoreblockChar = ReturnMoreblockManager.getMoreblockChar(newSpec);
        String newType;
        String newTypeName;
        if (newMoreblockChar.contains("|")) {
            String[] split = newMoreblockChar.split("\\|");
            newType = split[0];
            newTypeName = split.length > 1 ? split[1] : "";
        } else {
            newType = newMoreblockChar;
            newTypeName = "";
        }

        ArrayList<String> oldParamTokens = extractParamTokens(oldCallerSpec);
        ArrayList<String> newParamTokens = extractParamTokens(newCallerSpec);

        for (ArrayList<BlockBean> eventBlocks : blocksMap.values()) {
            if (eventBlocks == null) continue;
            for (BlockBean blockBean : eventBlocks) {
                if ("definedFunc".equals(blockBean.opCode) && oldCallerSpec.equals(blockBean.spec)) {
                    ArrayList<String> oldParams = blockBean.parameters != null
                            ? new ArrayList<>(blockBean.parameters)
                            : new ArrayList<>();
                    ArrayList<String> remappedParams = remapCallerParameters(oldParamTokens, newParamTokens, oldParams);
                    blockBean.spec = newCallerSpec;
                    blockBean.type = newType;
                    blockBean.typeName = newTypeName;
                    blockBean.classInfo = mq.a(newType, newTypeName);
                    blockBean.paramClassInfo = mq.a(newCallerSpec);
                    blockBean.parameters = remappedParams;
                }
            }
        }

        logicManager.k();
        bB.a(getApplicationContext(), getString(R.string.common_word_saved), bB.TOAST_NORMAL).show();
    }

    private static ArrayList<String> extractParamTokens(String spec) {
        ArrayList<String> tokens = new ArrayList<>();
        if (spec == null || spec.isEmpty()) return tokens;
        for (String part : FB.c(spec)) {
            if (part.startsWith("%")) {
                tokens.add(part);
            }
        }
        return tokens;
    }

    private static ArrayList<String> remapCallerParameters(
            ArrayList<String> oldTokens,
            ArrayList<String> newTokens,
            ArrayList<String> oldValues
    ) {
        ArrayList<String> result = new ArrayList<>(newTokens.size());
        boolean[] used = new boolean[oldTokens.size()];
        for (int i = 0; i < newTokens.size(); i++) {
            String newToken = newTokens.get(i);
            String mapped = "";
            int matchedIndex = -1;
            for (int j = 0; j < oldTokens.size(); j++) {
                if (!used[j] && newToken.equals(oldTokens.get(j))) {
                    matchedIndex = j;
                    break;
                }
            }
            if (matchedIndex == -1 && i < oldTokens.size() && !used[i] && sameParamType(newToken, oldTokens.get(i))) {
                matchedIndex = i;
            }
            if (matchedIndex != -1) {
                used[matchedIndex] = true;
                if (matchedIndex < oldValues.size()) {
                    mapped = oldValues.get(matchedIndex);
                }
            }
            result.add(mapped);
        }
        return result;
    }

    private static boolean sameParamType(String tokenA, String tokenB) {
        int dotA = tokenA.lastIndexOf('.');
        int dotB = tokenB.lastIndexOf('.');
        if (dotA <= 0 || dotB <= 0) return false;
        return tokenA.substring(0, dotA).equals(tokenB.substring(0, dotB));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isStoragePermissionGranted()) {
            finish();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("sc_id", sc_id);
        outState.putParcelable("project_file", project);
        outState.putBoolean("is_edit", isEdit);
        outState.putString("old_block_name", oldBlockName);
        outState.putString("old_block_spec", oldBlockSpec);
        super.onSaveInstanceState(outState);
    }
}