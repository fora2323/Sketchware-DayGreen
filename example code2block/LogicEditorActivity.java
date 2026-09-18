package ma.swblockeditor.core;

import static io.edward.blockcraft.core.BlocksLoader.toStringList;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import io.edward.blockcraft.R;
import io.edward.blockcraft.blocks.records.BlockButton;

import io.edward.blockcraft.blocks.records.BlockPalette;
import io.edward.blockcraft.core.BlocksLoader;
import io.edward.blockcraft.core.code_editor.CodeEditorActivity;
import io.edward.blockcraft.libs.ui.WavyProgressIndicatorDialog;
import io.edward.blockcraft.utils.InsetsUtils;

@SuppressLint("ClickableViewAccessibility")
public class LogicEditorActivity extends AppCompatActivity implements OnClickListener, OnBlockCategorySelectListener, OnTouchListener {

    private String filePath = "";

    private ObjectAnimator aniHidePalette;
    private ObjectAnimator aniShowPalette;
    private ObjectAnimator aniShowTopMenu;
    private ObjectAnimator aniHideTopMenu;
    private LinearLayout areaPalette;
    private boolean bInitPaletteAnimation = false;
    private boolean bShowTopMenu = false;
    private View currentTouchedView = null;
    private ViewDummy dummy;
    private ViewLogicEditor editor;
    private FloatingActionButton fab;
    private final Handler handler = new Handler();
    private LogicTopMenu logicTopMenu;
    private boolean isDragged = false;
    private boolean isPaletteOpened = false;
    private LinearLayout layoutPalette;
    private final Runnable longPressed = this::dragStart;
    private int minDist = 0;
    private int originalArgIndex;
    private int originalInsertOption;
    private Block originalParent;
    private PaletteBlock paletteBlock;
    private PaletteSelector paletteSelector;
    private BlockPane pane;
    private final int[] posDummy = new int[2];
    private float posInitX = 0.0f;
    private float posInitY = 0.0f;
    private boolean useVibrate;
    private Vibrator vibrator;


    public int BLOCK_DRAG_X = 0;
    public int BLOCK_DRAG_Y = -30;

    private WavyProgressIndicatorDialog loadingDialog;

    private ArrayList<BlockBean> copiedBlocks = new ArrayList<>();

    private void addBlockToPalette(BlockBean blockBean) {
        BlockBase addBlock = paletteBlock.addBlock(blockBean);
        addBlock.setClickable(true);
        addBlock.setOnTouchListener(this);
    }

    private void addButtonToPalette(String str, String str2) {
        View addButton = paletteBlock.addButton(str);
        addButton.setTag(str2);
        addButton.setSoundEffectsEnabled(true);
        addButton.setOnClickListener(this);
    }

    private void allocatePalette(int var1) {
        if (2 == var1) {
            LayoutParams var2 = new LayoutParams((int) LayoutUtil.getDip(this, 320.0F), -1);
            areaPalette.setLayoutParams(var2);
            LayoutParams var3 = new LayoutParams(-2, -2);
            var3.gravity = 81;
            int var4 = (int) getResources().getDimension(R.dimen.action_button_margin);
            var3.setMargins(var4, var4, var4, var4);
            fab.setLayoutParams(var3);
            RelativeLayout.LayoutParams var5 = new RelativeLayout.LayoutParams(-2, -1);
            var5.addRule(10);
            var5.addRule(11);
            var5.topMargin = Objects.requireNonNull(getSupportActionBar()).getHeight();
            layoutPalette.setOrientation(LinearLayout.HORIZONTAL);
            layoutPalette.setLayoutParams(var5);
        } else {
            LayoutParams var6 = new LayoutParams(-1, (int) LayoutUtil.getDip(this, 240.0F));
            areaPalette.setLayoutParams(var6);
            LayoutParams var7 = new LayoutParams(-2, -2);
            var7.gravity = 21;
            int var8 = (int) getResources().getDimension(R.dimen.action_button_margin);
            var7.setMargins(var8, var8, var8, var8);
            fab.setLayoutParams(var7);
            RelativeLayout.LayoutParams var9 = new RelativeLayout.LayoutParams(-1, -2);
            var9.addRule(9);
            var9.addRule(12);
            layoutPalette.setOrientation(LinearLayout.VERTICAL);
            layoutPalette.setLayoutParams(var9);
        }

        initPaletteAnimation(var1);
    }

    private void cancelTopMenuAnimation() {
        if (aniShowTopMenu != null && aniShowTopMenu.isRunning()) {
            aniShowTopMenu.cancel();
        }
        if (aniHideTopMenu != null && aniHideTopMenu.isRunning()) {
            aniHideTopMenu.cancel();
        }
    }

    private void cancelPaletteAnimation() {
        if (aniShowPalette.isRunning()) {
            aniShowPalette.cancel();
        }
        if (aniHidePalette.isRunning()) {
            aniHidePalette.cancel();
        }
    }

    private void dragStart() {
        if (currentTouchedView != null) {
            paletteBlock.setDragEnabled(false);
            editor.setScrollEnabled(false);
            if (useVibrate) {
                vibrator.vibrate(100);
            }
            isDragged = true;
            if (((Block) currentTouchedView).getBlockType() == 0) {
                getOriginalState((Block) currentTouchedView);
                showTopMenu(true);
                dummy.makeDummyWithBlock((Block) currentTouchedView);
                pane.setVisibleBlock((Block) currentTouchedView, 8);
                pane.removeRelation((Block) currentTouchedView);
            } else {
                dummy.makeDummyWithBlock((Block) currentTouchedView);
            }
            pane.prepareToDrag((Block) currentTouchedView);
            dummy.moveDummy(currentTouchedView, posInitX, posInitY, posInitX, posInitY, (float) BLOCK_DRAG_X, (float) BLOCK_DRAG_Y);
            dummy.getDummyPosition(posDummy);
            if (editor.hitTest((float) posDummy[0], (float) posDummy[1])) {
                dummy.setAllow(true);
                pane.updateFeedbackFor((Block) currentTouchedView, posDummy[0], posDummy[1]);
                return;
            }
            dummy.setAllow(false);
            pane.hideFeedbackShape();
        }
    }

    private void getOriginalState(Block block) {
        originalParent = null;
        originalArgIndex = -1;
        originalInsertOption = 0;
        int[] posOriginal = new int[2];
        block.getLocationOnScreen(posOriginal);
        if (block.parentBlock != null) {
            originalParent = block.parentBlock;
        }
        if (originalParent != null) {
            if (originalParent.nextBlock == (Integer) block.getTag()) {
                originalInsertOption = 0;
            } else if (originalParent.subStack1 == (Integer) block.getTag()) {
                originalInsertOption = 2;
            } else if (originalParent.subStack2 == (Integer) block.getTag()) {
                originalInsertOption = 3;
            } else if (originalParent.args.contains(block)) {
                originalInsertOption = 5;
                originalArgIndex = originalParent.args.indexOf(block);
            }
        }
    }


    private void initTopMenuAnimation() {
        aniShowTopMenu = ObjectAnimator.ofFloat(logicTopMenu, "TranslationY", 0.0f);
        aniShowTopMenu.setDuration(300);
        aniShowTopMenu.setInterpolator(new DecelerateInterpolator());
        aniHideTopMenu = ObjectAnimator.ofFloat(logicTopMenu, "TranslationY", -100.0f * LayoutUtil.getDip(this, 1.0f));
        aniHideTopMenu.setDuration(300);
        aniHideTopMenu.setInterpolator(new DecelerateInterpolator());
    }

    private void initPaletteAnimation(int i) {
        if (2 == i) {
            if (isPaletteOpened) {
                layoutPalette.setTranslationX(0.0f);
                layoutPalette.setTranslationY(0.0f);
            } else {
                layoutPalette.setTranslationX((float) ((int) LayoutUtil.getDip(this, 320.0f)));
                layoutPalette.setTranslationY(0.0f);
            }
        } else if (isPaletteOpened) {
            layoutPalette.setTranslationX(0.0f);
            layoutPalette.setTranslationY(0.0f);
        } else {
            layoutPalette.setTranslationX(0.0f);
            layoutPalette.setTranslationY((float) ((int) LayoutUtil.getDip(this, 240.0f)));
        }
        if (2 == i) {
            aniShowPalette = ObjectAnimator.ofFloat(layoutPalette, "TranslationX", 0.0f);
            aniHidePalette = ObjectAnimator.ofFloat(layoutPalette, "TranslationX", (float) ((int) LayoutUtil.getDip(this, 320.0f)));
        } else {
            aniShowPalette = ObjectAnimator.ofFloat(layoutPalette, "TranslationY", 0.0f);
            aniHidePalette = ObjectAnimator.ofFloat(layoutPalette, "TranslationY", (float) ((int) LayoutUtil.getDip(this, 240.0f)));
        }
        aniShowPalette.removeAllListeners();
        aniHidePalette.removeAllListeners();
        aniShowPalette.addListener(new Animator.AnimatorListener() {
            public void onAnimationCancel(@NonNull Animator var1) {
            }

            public void onAnimationEnd(@NonNull Animator var1) {
            }

            public void onAnimationRepeat(@NonNull Animator var1) {
            }

            public void onAnimationStart(@NonNull Animator var1) {
            }
        });
        aniHidePalette.addListener(new Animator.AnimatorListener() {
            public void onAnimationCancel(@NonNull Animator var1) {
            }

            public void onAnimationEnd(@NonNull Animator var1) {
            }

            public void onAnimationRepeat(@NonNull Animator var1) {
            }

            public void onAnimationStart(@NonNull Animator var1) {
            }
        });
        aniShowPalette.setDuration(500);
        aniShowPalette.setInterpolator(new DecelerateInterpolator());
        aniHidePalette.setDuration(300);
        aniHidePalette.setInterpolator(new DecelerateInterpolator());
        bInitPaletteAnimation = true;
    }

    private void loadLogic(ArrayList<BlockBean> blocks) {
        Map<Object, Object> hashMap = new HashMap<>();
        Iterator<BlockBean> it = blocks.iterator();
        int i = 1;
        while (it.hasNext()) {
            Block makeBlockFromBean = makeBlockFromBean(it.next());
            hashMap.put(makeBlockFromBean.getTag(), makeBlockFromBean);
            pane.blockId = Math.max(pane.blockId, (Integer) makeBlockFromBean.getTag() + 1);
            pane.addBlock(makeBlockFromBean, 0, 0);
            makeBlockFromBean.setOnTouchListener(this);
            if (i != 0) {
                pane.getRoot().insertBlock(makeBlockFromBean);
                i = 0;
            }
        }
        for (BlockBean blockBean : blocks) {
            Block block = (Block) hashMap.get(Integer.valueOf(blockBean.id));
            if (block != null) {
                Block block2;
                if (blockBean.subStack1 >= 0) {
                    block2 = (Block) hashMap.get(blockBean.subStack1);
                    if (block2 != null) {
                        block.insertBlockSub1(block2);
                    }
                }
                if (blockBean.subStack2 >= 0) {
                    block2 = (Block) hashMap.get(blockBean.subStack2);
                    if (block2 != null) {
                        block.insertBlockSub2(block2);
                    }
                }
                if (blockBean.nextBlock >= 0) {
                    block2 = (Block) hashMap.get(blockBean.nextBlock);
                    if (block2 != null) {
                        block.insertBlock(block2);
                    }
                }
                int size = blockBean.parameters.size();
                Log.d("ABCD", "processing : " + blockBean.parameters + " for spec : " + blockBean.spec);
                for (int i2 = 0; i2 < size; i2++) {
                    String str = blockBean.parameters.get(i2);
                    if (str != null && !str.isEmpty()) {
                        if (str.charAt(0) == '\uF000') {
                            block2 = (Block) hashMap.get(Integer.valueOf(str.substring(1)));
                            if (block2 != null) {
                                block.replaceArgWithBlock((BlockBase) block.args.get(i2), block2);
                            }
                        } else {
                            ((BlockArg) block.args.get(i2)).setArgValue(str);
                            block.recalcWidthToParent();
                        }
                    }
                }
            }
        }
        pane.getRoot().fixLayout();
        pane.calculateWidthHeight();
        pane.post(() -> loadingDialog.dismiss());
    }

    private void openPalette(boolean z) {
        if (!bInitPaletteAnimation) {
            initPaletteAnimation(getResources().getConfiguration().orientation);
        }
        if (isPaletteOpened != z) {
            isPaletteOpened = z;
            cancelPaletteAnimation();
            if (z) {
                aniShowPalette.start();
            } else {
                aniHidePalette.start();
            }
        }
    }

    private void pasteCopiedBlocks() {
        int i;
        BlockBean blockBean;
        int i2;
        int i3;
        Block makeBlockFromBean;
        Map<Object, Object> hashMap = new HashMap<>();
        Map<Object, Object> hashMap2 = new HashMap<>();
        for (BlockBean object : copiedBlocks) {
            Integer valueOf = Integer.valueOf(object.id);
            BlockPane blockPane = pane;
            i = blockPane.blockId;
            blockPane.blockId = i + 1;
            hashMap2.put(valueOf, i);
        }
        for (BlockBean bean : copiedBlocks) {
            blockBean = bean;
            if (blockBean.opCode.equals("getArg")) {
                i = 0;
                i2 = 0;
                while (i < pane.getRoot().args.size()) {
                    View view = pane.getRoot().args.get(i);
                    i3 = ((view instanceof Block) && blockBean.type.equals(((Block) view).mType) && blockBean.spec.equals(((Block) view).mSpec)) ? 1 : i2;
                    i++;
                    i2 = i3;
                }
                if (i2 == 0) {
                    hashMap2.put(Integer.valueOf(blockBean.id), 0);
                }
            }
        }
        Iterator<BlockBean> it3 = copiedBlocks.iterator();
        while (it3.hasNext()) {
            blockBean = it3.next();
            blockBean.id = String.valueOf(hashMap2.get(Integer.valueOf(blockBean.id)));
            i2 = blockBean.parameters.size();
            for (i3 = 0; i3 < i2; i3++) {
                String str = blockBean.parameters.get(i3);
                if (str != null && !str.isEmpty() && str.charAt(0) == '\uF000') {
                    Integer num = (Integer) hashMap2.get(Integer.valueOf(str.substring(1)));
                    if (num == null) {
                        blockBean.parameters.set(i3, "");
                    } else {
                        blockBean.parameters.set(i3, '\uF000' + String.valueOf(num));
                    }
                }
            }
            if (blockBean.subStack1 >= 0) {
                blockBean.subStack1 = ((Integer) Objects.requireNonNull(hashMap2.get(blockBean.subStack1)));
            }
            if (blockBean.subStack2 >= 0) {
                blockBean.subStack2 = (Integer) Objects.requireNonNull(hashMap2.get(blockBean.subStack2));
            }
            if (blockBean.nextBlock >= 0) {
                blockBean.nextBlock = (Integer) Objects.requireNonNull(hashMap2.get(blockBean.nextBlock));
            }
        }
        int[] iArr = new int[2];
        editor.getLocationOnScreen(iArr);
        int width = iArr[0] + (editor.getWidth() / 2);
        i3 = ((int) LayoutUtil.getDip(getApplicationContext(), 4.0f)) + iArr[1];
        it3 = copiedBlocks.iterator();
        Block block = null;
        while (it3.hasNext()) {
            blockBean = it3.next();
            if (!blockBean.id.equals("0")) {
                makeBlockFromBean = makeBlockFromBean(blockBean);
                hashMap.put(Integer.valueOf(makeBlockFromBean.getTag().toString()), makeBlockFromBean);
                pane.addBlock(makeBlockFromBean, width, i3);
                makeBlockFromBean.setOnTouchListener(this);
                block = makeBlockFromBean;
            }
        }
        for (BlockBean bean : copiedBlocks) {
            blockBean = bean;
            if (!blockBean.id.equals("0")) {
                Block block2 = (Block) hashMap.get(Integer.valueOf(blockBean.id));
                if (block2 != null) {
                    Block block3;
                    int size = blockBean.parameters.size();
                    for (int i4 = 0; i4 < size; i4++) {
                        String str2 = blockBean.parameters.get(i4);
                        if (str2 != null && !str2.isEmpty()) {
                            if (str2.charAt(0) == '\uF000') {
                                block3 = (Block) hashMap.get(Integer.valueOf(str2.substring(1)));
                                if (block3 != null) {
                                    block2.replaceArgWithBlock((BlockBase) block2.args.get(i4), block3);
                                }
                            } else {
                                ((BlockArg) block2.args.get(i4)).setArgValue(str2);
                                block2.recalcWidthToParent();
                            }
                        }
                    }
                    if (blockBean.subStack1 >= 0) {
                        block3 = (Block) hashMap.get(blockBean.subStack1);
                        if (block3 != null) {
                            block2.insertBlockSub1(block3);
                        }
                    }
                    if (blockBean.subStack2 >= 0) {
                        block3 = (Block) hashMap.get(blockBean.subStack2);
                        if (block3 != null) {
                            block2.insertBlockSub2(block3);
                        }
                    }
                    if (blockBean.nextBlock >= 0) {
                        makeBlockFromBean = (Block) hashMap.get(blockBean.nextBlock);
                        if (makeBlockFromBean != null) {
                            block2.insertBlock(makeBlockFromBean);
                        }
                    }
                }
            }
        }
        assert block != null;
        block.topBlock().fixLayout();
        pane.calculateWidthHeight();
    }

    private void showTopMenu(boolean show) {
        if (aniShowTopMenu == null) {
            initTopMenuAnimation();
        }
        if (bShowTopMenu != show) {
            bShowTopMenu = show;
            cancelTopMenuAnimation();
            if (show) {
                aniShowTopMenu.start();
            } else {
                aniHideTopMenu.start();
            }
        }
    }

    public void onClick(View view) {
    }

    public Block makeBlockFromBean(BlockBean blockBean) {
        return new Block(this, blockBean);
    }

    public void onBlockCategorySelect(BlockPalette blockPalette) {
        paletteBlock.removeAllBlocks();
        assert blockPalette != null;
        for (BlockButton blockButton : blockPalette.buttons()) {
            addButtonToPalette(blockButton.text(), blockButton.id());
        }
        long start = System.nanoTime();
        for (BlockBean blockBean : blockPalette.blocks()) {
            if (!blockBean.getHeaderText().isEmpty()) {
                paletteBlock.addHeaderText(blockBean.getHeaderText());
            }
            addBlockToPalette(blockBean);
        }
        long end = System.nanoTime();
        Log.d("perfTests", "rust: loading the palette \"" + blockPalette.name() + "\" took " + ((end - start) / 1000) + "µs");
    }

    public void onConfigurationChanged(@NonNull Configuration configuration) {
        super.onConfigurationChanged(configuration);
        allocatePalette(configuration.orientation);
    }

    @SuppressLint("MissingInflatedId")
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        EdgeToEdge.enable(this);
        setContentView(R.layout.logic_editor);

        Context context = getApplicationContext();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        BLOCK_DRAG_Y = (int) LayoutUtil.getDip(this, BLOCK_DRAG_Y);

        useVibrate = true;
        minDist = ViewConfiguration.get(context).getScaledTouchSlop();
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        filePath = getIntent().getStringExtra("filePath");

        String extension;
        try {
            assert filePath != null;
            extension = filePath.substring(filePath.lastIndexOf('.') + 1);
            String folderAndFile = filePath.substring(filePath.lastIndexOf('/', filePath.lastIndexOf('/') - 1) + 1);
            toolbar.setSubtitle(folderAndFile);
        } catch (Exception e) {
            finish();
            return;
        }

        paletteSelector = findViewById(R.id.palette_selector);
        paletteSelector.initialize(getApplicationContext(), extension);
        paletteSelector.setOnBlockCategorySelectListener(this);
        paletteBlock = findViewById(R.id.palette_block);
        dummy = findViewById(R.id.dummy);
        logicTopMenu = findViewById(R.id.logic_top_menu);
        editor = findViewById(R.id.editor);
        pane = editor.getBlockPane();
        paletteSelector.performClickByIndex(0);
        layoutPalette = findViewById(R.id.layout_palette);
        areaPalette = findViewById(R.id.area_palette);
        fab = findViewById(R.id.fab_toggle_palette);
        fab.setOnClickListener(v -> openPalette(!isPaletteOpened));
        findViewById(R.id.search_header).setOnClickListener(view -> paletteSelector.showSearchDialog());
        InsetsUtils.applyBottomInsets(fab);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isPaletteOpened) {
                    openPalette(false);
                } else {
                    finish();
                }
            }
        });
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.logic_menu, menu);
        return true;
    }

    protected void onDestroy() {
        super.onDestroy();
    }

    public boolean onOptionsItemSelected(MenuItem menuItem) {

        if (menuItem.getItemId() == R.id.menu_show_source) {
            showSourceCode();
        }
        return super.onOptionsItemSelected(menuItem);
    }

    private void showSourceCode() {
        JavaSourceMaker jsm = new JavaSourceMaker(pane.getBlocks());

        String result = jsm.getSource();

        Builder b = new Builder(this);
        b.setTitle("Source code");
        b.setMessage(result);
        b.setPositiveButton("OK", null);
        b.create().show();
    }

    protected void onPostCreate(@Nullable Bundle var1) {
        super.onPostCreate(var1);
        String var2 = "Hat Content %s.arg1_str message: %b.arg2_bool double %d.arg3_dbl";

        pane.addRoot(var2);
        Block root = pane.getRoot();
        if (root != null && root.args != null) {
            for (View v : root.args) {
                v.setOnTouchListener(this);
            }
        }

        pane.getRoot().fixLayout();
        pane.calculateWidthHeight();
        allocatePalette(getResources().getConfiguration().orientation);
        startGenerating();
    }

    protected void onResume() {
        super.onResume();
    }

    public boolean onTouch(View view, MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        if (action == 0) {
            isDragged = false;
            handler.postDelayed(longPressed, (ViewConfiguration.getLongPressTimeout() / 2));
            posInitX = motionEvent.getX();
            posInitY = motionEvent.getY();
            currentTouchedView = view;
            return true;
        } else if (action == 2) {
            if (isDragged) {
                handler.removeCallbacks(longPressed);
                dummy.moveDummy(view, motionEvent.getX(), motionEvent.getY(), posInitX, posInitY, (float) BLOCK_DRAG_X, (float) BLOCK_DRAG_Y);
                if (logicTopMenu.isInsideDeleteArea(motionEvent.getRawX(), motionEvent.getRawY())) {
                    dummy.setAllow(true);
                    logicTopMenu.setDeleteActive(true);
                    logicTopMenu.setDuplicateActive(false);
                    return true;
                }
                logicTopMenu.setDeleteActive(false);

                if (logicTopMenu.isInsideDuplicateArea(motionEvent.getRawX(), motionEvent.getRawY())) {
                    dummy.setAllow(true);
                    logicTopMenu.setDuplicateActive(true);
                    return true;
                }
                logicTopMenu.setDeleteActive(false);
                dummy.getDummyPosition(posDummy);
                if (editor.hitTest((float) posDummy[0], (float) posDummy[1])) {
                    dummy.setAllow(true);
                    pane.updateFeedbackFor((Block) view, posDummy[0], posDummy[1]);
                } else {
                    dummy.setAllow(false);
                    pane.hideFeedbackShape();
                }
                return true;
            } else if (Math.abs(posInitX - motionEvent.getX()) < ((float) minDist) && Math.abs(posInitY - motionEvent.getY()) < ((float) minDist)) {
                return false;
            } else {
                currentTouchedView = null;
                handler.removeCallbacks(longPressed);
                return false;
            }
        } else if (action == 1) {
            currentTouchedView = null;
            handler.removeCallbacks(longPressed);
            if (isDragged) {
                paletteBlock.setDragEnabled(true);
                editor.setScrollEnabled(true);
                dummy.setDummyVisibility(8);
                if (dummy.getAllow()) {
                    if (logicTopMenu.isDeleteActive) {
                        logicTopMenu.setDeleteActive(false);
                        pane.removeBlock((Block) view);
                    } else if (logicTopMenu.isDuplicateActive) {
                        logicTopMenu.setDuplicateActive(false);
                        copyBlocks(((Block) view).getAllChildren());

                        pane.setVisibleBlock((Block) view, 0);
                        if (originalParent != null) {
                            if (originalInsertOption == 0) {
                                originalParent.nextBlock = (Integer) view.getTag();
                            }
                            if (originalInsertOption == 2) {
                                originalParent.subStack1 = (Integer) view.getTag();
                            }
                            if (originalInsertOption == 3) {
                                originalParent.subStack2 = (Integer) view.getTag();
                            }
                            if (originalInsertOption == 5) {
                                originalParent.replaceArgWithBlock((BlockBase) originalParent.args.get(originalArgIndex), (Block) view);
                            }
                            ((Block) view).parentBlock = originalParent;
                            originalParent.topBlock().fixLayout();
                        } else {
                            ((Block) view).topBlock().fixLayout();
                        }

                        pasteCopiedBlocks();
                    } else if (view instanceof Block block) {
                        dummy.getDummyPosition(posDummy);
                        if (block.getBlockType() == 1) {
                            Block droppedBlock = pane.blockDropped(block, posDummy[0], posDummy[1], false);
                            for (Block child : droppedBlock.getAllChildren()) {
                                child.setOnTouchListener(this);
                            }
                        } else {
                            pane.setVisibleBlock(block, 0);
                            pane.blockDropped(block, posDummy[0], posDummy[1], true);
                        }
                        pane.draggingDone();
                    }
                } else if (((Block) view).getBlockType() == 0) {
                    pane.setVisibleBlock((Block) view, 0);
                    if (originalParent != null) {
                        if (originalInsertOption == 0) {
                            originalParent.nextBlock = (Integer) view.getTag();
                        }
                        if (originalInsertOption == 2) {
                            originalParent.subStack1 = (Integer) view.getTag();
                        }
                        if (originalInsertOption == 3) {
                            originalParent.subStack2 = (Integer) view.getTag();
                        }
                        if (originalInsertOption == 5) {
                            originalParent.replaceArgWithBlock((BlockBase) originalParent.args.get(originalArgIndex), (Block) view);
                        }
                        ((Block) view).parentBlock = originalParent;
                        originalParent.topBlock().fixLayout();
                    } else {
                        ((Block) view).topBlock().fixLayout();
                    }
                }
                dummy.setAllow(false);
                showTopMenu(false);
                isDragged = false;
                return true;
            }
            if ((view instanceof Block) && ((Block) view).getBlockType() == 0) {
                ((Block) view).actionClick(motionEvent.getX(), motionEvent.getY());
            }
            return false;
        } else if (action == 3) {
            handler.removeCallbacks(longPressed);
            isDragged = false;
            return false;
        } else if (action != 8) {
            return true;
        } else {
            handler.removeCallbacks(longPressed);
            isDragged = false;
            return false;
        }
    }

    private void copyBlocks(ArrayList<Block> arrayList) {
        copiedBlocks = new ArrayList<>();
        for (Block block : arrayList) {
            BlockBean mainBean = block.getBean();
            BlockBean copyBean = mainBean.clone();
            int VAR_TYPE_INT = 1;
            Object[] objArr = new Object[VAR_TYPE_INT];
            int VAR_TYPE_BOOLEAN = 0;
            objArr[VAR_TYPE_BOOLEAN] = Integer.valueOf(mainBean.id);
            copyBean.id = String.format(Locale.US, "99%06d", objArr);
            if (mainBean.subStack1 > 0) {
                copyBean.subStack1 = mainBean.subStack1 + 99000000;
            }
            if (mainBean.subStack2 > 0) {
                copyBean.subStack2 = mainBean.subStack2 + 99000000;
            }
            if (mainBean.nextBlock > 0) {
                copyBean.nextBlock = mainBean.nextBlock + 99000000;
            }
            copyBean.parameters.clear();
            for (String str2 : mainBean.parameters) {
                if (str2.length() <= VAR_TYPE_INT || str2.charAt(VAR_TYPE_BOOLEAN) != '\uF000') {
                    copyBean.parameters.add(str2);
                } else {
                    Object[] objArr2 = new Object[VAR_TYPE_INT];
                    objArr2[VAR_TYPE_BOOLEAN] = Integer.valueOf(str2.substring(VAR_TYPE_INT));
                    copyBean.parameters.add('\uF000' + String.format(Locale.US, "99%06d", objArr2));
                }
            }
            copiedBlocks.add(copyBean);
        }
    }

    private void startGenerating() {
        loadingDialog = new WavyProgressIndicatorDialog(this, "Running Code2Blocks").show();

        new Thread(() -> {
            try {
                String result = BlocksLoader.BlockBeansGenerator(
                        filePath
                );

                Object parsed = new JSONTokener(result).nextValue();

                if (parsed instanceof JSONObject obj) {
                    if (obj.has("Error")) {
                        String error = obj.optString("Error", "Unknown error");
                        runOnUiThread(() -> showErrorDialog(error));
                        return;
                    }
                }

                assert parsed instanceof JSONArray;
                JSONArray jsonArray = (JSONArray) parsed;
                ArrayList<BlockBean> blocks = new ArrayList<>();

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject b = jsonArray.getJSONObject(i);
                    BlockBean blockBean = new BlockBean()
                            .setId(b.optString("id"))
                            .setOpCode(b.optString("op_code"))
                            .setHeaderText(b.optString("header_text"))
                            .setColor(b.optInt("color"))
                            .setType(b.optString("block_type"))
                            .setCode(b.optString("code"))
                            .setSpec(b.optString("spec"))
                            .setSpec2(b.optString("spec2"))
                            .setTokenizedSpec(toStringList(Objects.requireNonNull(b.optJSONArray("tokenized_spec"))))
                            .setParameters(toStringList(Objects.requireNonNull(b.optJSONArray("parameters"))))
                            .setNextBlock(b.optInt("next_block", -1))
                            .setSubStack1(b.optInt("sub_stack1", -1))
                            .setSubStack2(b.optInt("sub_stack2", -1));

                    blocks.add(blockBean);
                }

                runOnUiThread(() -> loadLogic(blocks));

            } catch (Exception e) {
                runOnUiThread(() -> showErrorDialog(e.getMessage()));
            }
        }).start();
    }

    private void showErrorDialog(String errorMessage) {
        loadingDialog.dismiss();
        new MaterialAlertDialogBuilder(this)
                .setTitle("Code2Blocks crashed!!")
                .setMessage(errorMessage)
                .setCancelable(false)
                .setPositiveButton("Open Code Editor", (d, w) -> {
                    d.dismiss();
                    Intent intent = new Intent(this, CodeEditorActivity.class);
                    intent.putExtra("filePath", filePath);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Exit", (d, w) -> finish())
                .show();
    }

}
