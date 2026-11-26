package com.besome.sketch.editor.event;

import android.animation.Animator;
import android.content.Intent;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.besome.sketch.beans.ComponentBean;
import com.besome.sketch.beans.EventBean;
import com.besome.sketch.beans.ProjectFileBean;
import com.besome.sketch.beans.ViewBean;
import com.besome.sketch.editor.makeblock.MoreBlockBuilderView;
import com.besome.sketch.lib.base.BaseAppCompatActivity;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import a.a.a.Ox;
import a.a.a.bB;
import a.a.a.gB;
import a.a.a.jC;
import a.a.a.jq;
import a.a.a.mB;
import a.a.a.oq;
import a.a.a.rs;
import a.a.a.wB;
import dev.chrisbanes.insetter.Insetter;
import extensions.anbui.daydream.settings.DayDreamProjectSettings;
import pro.sketchware.R;
import pro.sketchware.databinding.LogicPopupAddEventBinding;

public class AddEventActivity extends BaseAppCompatActivity implements View.OnClickListener {
	private ArrayList<EventBean> addableDrawerViewEvents;
	private ArrayList<EventBean> eventsToAdd;
	private boolean C;
	private int categoryIndex;
	private MoreBlockBuilderView moreBlockView;
	private String sc_id;
	private ProjectFileBean projectFile;
	private EventAdapter eventAdapter;
	private EventsToAddAdapter eventsToAddAdapter;
	private HashMap<Integer, ArrayList<EventBean>> categories;
	private ArrayList<EventBean> addableEtcEvents;
	private ArrayList<EventBean> addableViewEvents;
	private ArrayList<EventBean> addableComponentEvents;
	private ArrayList<EventBean> addableActivityEvents;

	private LogicPopupAddEventBinding binding;

	private void l() {
		updateAddButtonLabel();
		if (eventsToAdd.isEmpty() && !C) {
			C = true;
			gB.a(binding.eventsPreview, 300, new Animator.AnimatorListener() {
				@Override
				public void onAnimationStart(@NonNull Animator animation) {
				}

				@Override
				public void onAnimationEnd(@NonNull Animator animation) {
					binding.eventsPreview.setVisibility(View.GONE);
				}

				@Override
				public void onAnimationCancel(@NonNull Animator animation) {
				}

				@Override
				public void onAnimationRepeat(@NonNull Animator animation) {
				}
			});
		} else if (!eventsToAdd.isEmpty() && C) {
			C = false;
			binding.eventsPreview.setVisibility(View.VISIBLE);
			gB.b(binding.eventsPreview, 300, null);
		}
	}

	private void updateAddButtonLabel() {
		if (eventsToAdd.isEmpty()) {
			binding.addButton.setText(R.string.common_word_add);
		} else {
			binding.addButton.setText(getString(R.string.common_word_add) + " (" + eventsToAdd.size() + ")");
		}
	}

	private void setupTabs() {
		if (binding.tabLayout.getTabCount() > 0) {
			return;
		}
		for (int i = 0; i < categories.size(); i++) {
			TabLayout.Tab tab = binding.tabLayout.newTab()
					.setText(rs.a(getApplicationContext(), i))
					.setIcon(rs.a(i));
			binding.tabLayout.addTab(tab, i == categoryIndex);
		}
		binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
			@Override
			public void onTabSelected(TabLayout.Tab tab) {
				int pos = tab.getPosition();
				if (pos != categoryIndex) {
					categoryIndex = pos;
					applyCategorySelection(pos);
				}
			}

			@Override
			public void onTabUnselected(TabLayout.Tab tab) {
			}

			@Override
			public void onTabReselected(TabLayout.Tab tab) {
			}
		});
	}

	private void applyCategorySelection(int index) {
		binding.emptyIcon.setImageResource(rs.a(index));
		if (index == 4) {
			binding.moreblockLayout.setVisibility(View.VISIBLE);
			binding.emptyMessage.setVisibility(View.GONE);
			binding.eventList.setVisibility(View.GONE);
		} else {
			binding.moreblockLayout.setVisibility(View.GONE);
			ArrayList<EventBean> list = categories.get(index);
			eventAdapter.setEvents(list != null ? list : new ArrayList<>());
			eventAdapter.notifyDataSetChanged();
		}
	}

	private void initialize() {
		addableEtcEvents.clear();
		addableViewEvents.clear();
		addableActivityEvents.clear();
		addableComponentEvents.clear();
		addableDrawerViewEvents.clear();
		eventsToAdd.clear();
		C = true;
		binding.eventsPreview.setVisibility(View.GONE);
		updateAddButtonLabel();

		boolean isFragment = isFragmentFile();
		for (var activityEvent : oq.getAllActivityEvents()) {
			if (!isFragment && "onDestroyView".equals(activityEvent)) {
				continue;
			}
			boolean exists = false;
			for (var existingEvent : jC.a(sc_id).g(projectFile.getJavaName())) {
				if (existingEvent.eventType == EventBean.EVENT_TYPE_ACTIVITY && activityEvent.equals(existingEvent.eventName)) {
					exists = true;
					break;
				}
			}

			if (!exists) {
				addableActivityEvents.add(new EventBean(EventBean.EVENT_TYPE_ACTIVITY, 0, activityEvent, activityEvent));
			}
		}
		ArrayList<ViewBean> views = jC.a(sc_id).d(projectFile.getXmlName());
		ArrayList<ComponentBean> components = jC.a(sc_id).e(projectFile.getJavaName());
		if (views != null) {
			for (ViewBean view : views) {
				Set<String> toNotAdd = new Ox(new jq(), projectFile).readAttributesToReplace(view);
				for (String viewEvent : oq.getEventsForClass(view.getClassInfo())) {
					boolean exists;
					if (viewEvent.equals("onBindCustomView") && (view.customView.isEmpty() || view.customView.equals("none"))) {
						exists = true;
					} else {
						exists = false;
						for (var existingEvent : jC.a(sc_id).g(projectFile.getJavaName())) {
							if (existingEvent.eventType == EventBean.EVENT_TYPE_VIEW && view.id.equals(existingEvent.targetId) && viewEvent.equals(existingEvent.eventName)) {
								exists = true;
								break;
							}
						}
					}

					if (!exists && !toNotAdd.contains("android:id")) {
						addableViewEvents.add(new EventBean(EventBean.EVENT_TYPE_VIEW, view.type, view.id, viewEvent));
					}
				}
			}
		}
		if (components != null) {
			for (ComponentBean component : components) {
				for (String componentEvent : oq.getComponentEventsForClass(component.getClassInfo())) {
					boolean exists = false;
					for (var existingEvent : jC.a(sc_id).g(projectFile.getJavaName())) {
						if (existingEvent.eventType == EventBean.EVENT_TYPE_COMPONENT && component.componentId.equals(existingEvent.targetId) && componentEvent.equals(existingEvent.eventName)) {
							exists = true;
							break;
						}
					}
					if (!exists) {
						addableComponentEvents.add(new EventBean(EventBean.EVENT_TYPE_COMPONENT, component.type, component.componentId, componentEvent));
					}
				}
			}
		}
		ViewBean fab;
		if (projectFile.hasActivityOption(ProjectFileBean.OPTION_ACTIVITY_FAB) && (fab = jC.a(sc_id).h(projectFile.getXmlName())) != null) {
			for (String fabEvent : oq.getEventsForClass(fab.getClassInfo())) {
				boolean exists = false;
				for (var existingFabEvent : jC.a(sc_id).g(projectFile.getJavaName())) {
					if (existingFabEvent.eventType == EventBean.EVENT_TYPE_VIEW && fab.id.equals(existingFabEvent.targetId) && fabEvent.equals(existingFabEvent.eventName)) {
						exists = true;
						break;
					}
				}
				if (!exists) {
					addableViewEvents.add(new EventBean(EventBean.EVENT_TYPE_VIEW, fab.type, fab.id, fabEvent));
				}
			}
		}
		if (projectFile.hasActivityOption(ProjectFileBean.OPTION_ACTIVITY_DRAWER)) {
			ArrayList<ViewBean> drawerViews = jC.a(sc_id).d(projectFile.getDrawerXmlName());
			if (drawerViews != null) {
				for (ViewBean drawerView : drawerViews) {
					Set<String> toNotAdd = new Ox(new jq(), projectFile).readAttributesToReplace(drawerView);
					for (String drawerViewEvent : oq.getEventsForClass(drawerView.getClassInfo())) {
						boolean exists = false;
						for (var existingEvent : jC.a(sc_id).g(projectFile.getJavaName())) {
							if (existingEvent.eventType == EventBean.EVENT_TYPE_DRAWER_VIEW && drawerView.id.equals(existingEvent.targetId) && drawerViewEvent.equals(existingEvent.eventName)) {
								exists = true;
								break;
							}
						}
						if (!exists && !toNotAdd.contains("android:id")) {
							addableDrawerViewEvents.add(new EventBean(EventBean.EVENT_TYPE_DRAWER_VIEW, drawerView.type, drawerView.id, drawerViewEvent));
						}
					}
				}
			}
		}
		TabLayout.Tab selectedTab = binding.tabLayout.getTabAt(categoryIndex);
		if (selectedTab != null && !selectedTab.isSelected()) {
			selectedTab.select();
		}
		applyCategorySelection(categoryIndex);
	}

	private boolean isFragmentFile() {
		if (projectFile == null) {
			return false;
		}
		return projectFile.fileType == ProjectFileBean.PROJECT_FILE_TYPE_FRAGMENT
				|| (projectFile.fileName != null && projectFile.fileName.contains("_fragment"))
				|| DayDreamProjectSettings.getActivityType(sc_id, projectFile.fileName).contains("_fragment");
	}

	@Override
	public void onClick(View v) {
		boolean finished = false;
		if (!mB.a()) {
			int id = v.getId();
			if (id == R.id.add_button) {
				if (!eventsToAdd.isEmpty() || !moreBlockView.a()) {
					if (!moreBlockView.a()) {
						if (!moreBlockView.b()) {
							categoryIndex = 4;
							TabLayout.Tab moreBlockTab = binding.tabLayout.getTabAt(4);
							if (moreBlockTab != null) {
								moreBlockTab.select();
							}
							applyCategorySelection(4);
							finished = true;
						} else {
							Pair<String, String> blockInformation = moreBlockView.getBlockInformation();
							jC.a(sc_id).a(projectFile.getJavaName(), blockInformation.first, blockInformation.second);
						}
					}
					if (!finished) {
						for (EventBean eventBean : eventsToAdd) {
							jC.a(sc_id).a(projectFile.getJavaName(), eventBean);
						}
						if (eventsToAdd.size() == 1) {
							bB.a(getApplicationContext(), getApplicationContext().getString(R.string.event_message_new_event), bB.TOAST_NORMAL).show();
						} else if (eventsToAdd.size() > 1) {
							bB.a(getApplicationContext(), getApplicationContext().getString(R.string.event_message_new_events), bB.TOAST_NORMAL).show();
						}
						jC.a(sc_id).k();
						setResult(RESULT_OK);
						finish();
					}
				}
			} else if (id == R.id.cancel_button) {
				setResult(RESULT_CANCELED);
				finish();
			}
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		enableEdgeToEdgeNoContrast();
		super.onCreate(savedInstanceState);
		binding = LogicPopupAddEventBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		Insetter.builder()
				.padding(WindowInsetsCompat.Type.navigationBars() | WindowInsetsCompat.Type.ime())
				.applyToView(binding.bottomBar);

		Intent intent = getIntent();
		if (savedInstanceState == null) {
			sc_id = intent.getStringExtra("sc_id");
			projectFile = intent.getParcelableExtra("project_file");
			categoryIndex = intent.getIntExtra("category_index", 0);
		} else {
			sc_id = savedInstanceState.getString("sc_id");
			projectFile = savedInstanceState.getParcelable("project_file");
			categoryIndex = savedInstanceState.getInt("category_index");
		}
		if (categoryIndex < 0 || categoryIndex > 4) {
			categoryIndex = 0;
		}

		setSupportActionBar(binding.toolbar);
		if (getSupportActionBar() != null) {
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
			getSupportActionBar().setTitle(R.string.event_title_add_new_event);
			if (projectFile != null) {
				getSupportActionBar().setSubtitle(projectFile.getJavaName());
			}
		}
		binding.toolbar.setNavigationOnClickListener(v -> {
			setResult(RESULT_CANCELED);
			finish();
		});

		moreBlockView = new MoreBlockBuilderView(this);
		binding.moreblockLayout.addView(moreBlockView);
		binding.moreblockLayout.setVisibility(View.GONE);
		binding.addButton.setOnClickListener(this);
		binding.cancelButton.setOnClickListener(this);
		categories = new HashMap<>();
		addableEtcEvents = new ArrayList<>();
		addableViewEvents = new ArrayList<>();
		addableComponentEvents = new ArrayList<>();
		addableActivityEvents = new ArrayList<>();
		addableDrawerViewEvents = new ArrayList<>();
		categories.put(0, addableActivityEvents);
		categories.put(1, addableViewEvents);
		categories.put(2, addableComponentEvents);
		categories.put(3, addableDrawerViewEvents);
		categories.put(4, addableEtcEvents);
		setupTabs();
		binding.eventList.setLayoutManager(new LinearLayoutManager(getApplicationContext(), RecyclerView.VERTICAL, false));
		eventAdapter = new EventAdapter();
		binding.eventList.setAdapter(eventAdapter);
		eventsToAddAdapter = new EventsToAddAdapter();
		binding.eventsPreview.setLayoutManager(new LinearLayoutManager(getApplicationContext(), RecyclerView.HORIZONTAL, false));
		binding.eventsPreview.setAdapter(eventsToAddAdapter);
		C = true;
		binding.eventsPreview.setVisibility(View.GONE);
		binding.emptyMessage.setVisibility(View.GONE);
		eventsToAdd = new ArrayList<>();
	}

	@Override
	public void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		moreBlockView.setFuncNameValidator(jC.a(sc_id).a(projectFile));
	}

	@Override
	public void onResume() {
		super.onResume();
		if (projectFile != null) {
			initialize();
		}
	}

	@Override
	public void onSaveInstanceState(Bundle newState) {
		newState.putString("sc_id", sc_id);
		newState.putParcelable("project_file", projectFile);
		newState.putInt("category_index", categoryIndex);
		super.onSaveInstanceState(newState);
	}

	private class EventAdapter extends RecyclerView.Adapter<EventAdapter.ViewHolder> {
		private final ArrayList<EventBean> events = new ArrayList<>();
		private boolean e;

		public EventAdapter() {
		}

		@Override
		public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
			e = true;
			holder.events_preview.removeAllViews();
			holder.events_preview.setVisibility(View.VISIBLE);
			EventBean event = events.get(position);
			ImageView imageView = new ImageView(holder.itemView.getContext());
			LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			layoutParams.setMargins(0, 0, (int) wB.a(getApplicationContext(), 2.0f), 0);
			int a = (int) wB.a(getApplicationContext(), 16.0f);
			layoutParams.width = a;
			layoutParams.height = a;
			imageView.setLayoutParams(layoutParams);
			imageView.setImageResource(oq.getEventIconResource(event.eventName));
			imageView.setColorFilter(MaterialColors.getColor(imageView, com.google.android.material.R.attr.colorOutline));
			holder.events_preview.addView(imageView);
			holder.img_icon.setImageResource(EventBean.getEventIconResource(event.eventType, event.targetType));
			int eventType = event.eventType;
			if (eventType == EventBean.EVENT_TYPE_ACTIVITY) {
				holder.tv_target_type.setText("Activity");
				holder.events_preview.setVisibility(View.GONE);
			} else if (eventType == EventBean.EVENT_TYPE_VIEW || eventType == EventBean.EVENT_TYPE_DRAWER_VIEW) {
				holder.tv_target_type.setText(ViewBean.getViewTypeName(event.targetType));
			} else if (eventType == EventBean.EVENT_TYPE_COMPONENT) {
				holder.tv_target_type.setText(ComponentBean.getComponentName(getApplicationContext(), event.targetType));
			} else if (eventType == EventBean.EVENT_TYPE_ETC) {
				holder.events_preview.setVisibility(View.GONE);
			}
			if (event.targetId.equals("_fab")) {
				holder.tv_target_id.setText("fab");
			} else {
				holder.tv_target_id.setText(event.targetId);
			}
			holder.tv_event_name.setText(oq.getEventName(event.eventName));
			holder.checkbox.setChecked(event.isSelected);
			updateCardSelection(holder.card_root, event.isSelected);
			e = false;
		}

		private void updateCardSelection(MaterialCardView card, boolean selected) {
			if (selected) {
				card.setCardBackgroundColor(MaterialColors.getColor(card, com.google.android.material.R.attr.colorSecondaryContainer));
				card.setStrokeColor(MaterialColors.getColor(card, R.attr.colorPrimary));
				card.setStrokeWidth((int) wB.a(getApplicationContext(), 1.5f));
			} else {
				card.setCardBackgroundColor(MaterialColors.getColor(card, com.google.android.material.R.attr.colorSurfaceContainerLow));
				card.setStrokeWidth(0);
			}
		}

		@Override
		@NonNull
		public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.fr_logic_list_item_addevent, parent, false));
		}

		@Override
		public int getItemCount() {
			return events.size();
		}

		private void setEvents(ArrayList<EventBean> newEvents) {
			if (newEvents.isEmpty()) {
				binding.emptyMessage.setVisibility(View.VISIBLE);
				binding.eventList.setVisibility(View.GONE);
			} else {
				binding.emptyMessage.setVisibility(View.GONE);
				binding.eventList.setVisibility(View.VISIBLE);
			}
			events.clear();
			events.addAll(newEvents);
		}

		private class ViewHolder extends RecyclerView.ViewHolder {
			public final MaterialCardView card_root;
			public final LinearLayout events_preview;
			public final ImageView img_icon;
			public final TextView tv_target_type;
			public final TextView tv_target_id;
			public final TextView tv_event_name;
			public final CheckBox checkbox;

			public ViewHolder(@NonNull View itemView) {
				super(itemView);
				card_root = (MaterialCardView) itemView;
				events_preview = itemView.findViewById(R.id.events_preview);
				img_icon = itemView.findViewById(R.id.img_icon);
				tv_target_type = itemView.findViewById(R.id.tv_target_type);
				tv_target_id = itemView.findViewById(R.id.tv_target_id);
				tv_event_name = itemView.findViewById(R.id.tv_event_name);
				checkbox = itemView.findViewById(R.id.checkbox);
				itemView.setOnClickListener(v -> {
					if (!mB.a()) {
						int pos = getLayoutPosition();
						if (pos == RecyclerView.NO_POSITION) return;
						EventBean event = events.get(pos);
						if (event.isSelected) {
							event.isSelected = false;
							int removedIdx = eventsToAdd.indexOf(event);
							if (removedIdx >= 0) {
								eventsToAdd.remove(removedIdx);
								eventsToAddAdapter.notifyItemRemoved(removedIdx);
							}
							l();
						} else {
							event.isSelected = true;
							eventsToAdd.add(event);
							eventsToAddAdapter.notifyItemInserted(eventsToAdd.size() - 1);
							l();
						}
						if (!e) {
							notifyItemChanged(pos);
						}
					}
				});
				checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
					if (e) return;
					int pos = getLayoutPosition();
					if (pos == RecyclerView.NO_POSITION) return;
					EventBean event = events.get(pos);
					if (!event.isSelected && isChecked) {
						event.isSelected = true;
						eventsToAdd.add(event);
						eventsToAddAdapter.notifyItemInserted(eventsToAdd.size() - 1);
						l();
						notifyItemChanged(pos);
					} else if (event.isSelected && !isChecked) {
						event.isSelected = false;
						int removedIdx = eventsToAdd.indexOf(event);
						if (removedIdx >= 0) {
							eventsToAdd.remove(removedIdx);
							eventsToAddAdapter.notifyItemRemoved(removedIdx);
						}
						l();
						notifyItemChanged(pos);
					}
				});
			}
		}
	}

	private class EventsToAddAdapter extends RecyclerView.Adapter<EventsToAddAdapter.ViewHolder> {
		public EventsToAddAdapter() {
		}

		@Override
		public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
			holder.ll_img_event.setVisibility(View.VISIBLE);
			EventBean event = eventsToAdd.get(position);
			int eventType = event.eventType;
			if (eventType == EventBean.EVENT_TYPE_ACTIVITY || eventType == EventBean.EVENT_TYPE_ETC) {
				holder.ll_img_event.setVisibility(View.GONE);
			}
			holder.img_icon.setImageResource(EventBean.getEventIconResource(event.eventType, event.targetType));
			holder.img_event.setImageResource(oq.getEventIconResource(event.eventName));
		}

		@Override
		@NonNull
		public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.fr_logic_list_preview_with_event_item, parent, false));
		}

		@Override
		public int getItemCount() {
			return eventsToAdd.size();
		}

		private static class ViewHolder extends RecyclerView.ViewHolder {
			public LinearLayout ll_img_event;
			public RelativeLayout container;
			public ImageView img_icon;
			public ImageView img_event;

			public ViewHolder(@NonNull View itemView) {
				super(itemView);
				container = itemView.findViewById(R.id.container);
				img_icon = itemView.findViewById(R.id.img_icon);
				img_event = itemView.findViewById(R.id.img_event);
				ll_img_event = itemView.findViewById(R.id.ll_img_event);
			}
		}
	}
}