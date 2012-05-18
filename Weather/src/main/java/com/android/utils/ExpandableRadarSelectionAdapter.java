package com.android.utils;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;

import com.android.weather.RadarCenter;

public class ExpandableRadarSelectionAdapter extends BaseExpandableListAdapter {

	private final ExpandableRadarSelectionListener listener;
	private final ExpandableListView radarComboList;
	private final String[] groups;
	private final RadarCenter[][] children;
	private final Context context;

	public ExpandableRadarSelectionAdapter(final ExpandableListView radarComboList,
			final ExpandableRadarSelectionListener listener) {
		this.context = radarComboList.getContext();
		this.listener = listener;
		this.radarComboList = radarComboList;

		String[] nodeIds = new String[RadarCenter.values().length];
		int i = 0;
		for(RadarCenter radarCenter : RadarCenter.values()) {
			nodeIds[i] = radarCenter.getId();
			i++;
		}
		String[] groups = { "Radar" };
		RadarCenter[][] children = {RadarCenter.values()};

		this.groups = groups;
		this.children = children;

		radarComboList.setAdapter(this);
		radarComboList.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
			@Override
			public boolean onChildClick(final ExpandableListView parent,
					final View v, final int groupPosition, final int childPosition, final long id) {
				return selectChild(groupPosition, childPosition, id, true);
			}
		});

	}


	@Override
	public Object getChild(final int groupPosition, final int childPosition) {
		return children[groupPosition][childPosition];
	}

	@Override
	public long getChildId(final int groupPosition, final int childPosition) {
		return childPosition;
	}

	@Override
	public int getChildrenCount(final int groupPosition) {
		return children[groupPosition].length;
	}

	public TextView getGenericView() {
		// Layout parameters for the ExpandableListView
		AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
				ViewGroup.LayoutParams.FILL_PARENT, 64);

		TextView textView = new TextView(context);
		textView.setLayoutParams(lp);
		// Center the text vertically
		textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.FILL);
		// Set the text starting position
		textView.setPadding(60, 0, 0, 0);
		return textView;
	}

	@Override
	public View getChildView(final int groupPosition, final int childPosition, final boolean isLastChild,
			final View convertView, final ViewGroup parent) {
		TextView textView = getGenericView();
		textView.setText(getChild(groupPosition, childPosition).toString());
		return textView;
	}

	@Override
	public Object getGroup(final int groupPosition) {
		return groups[groupPosition];
	}

	@Override
	public int getGroupCount() {
		return groups.length;
	}

	@Override
	public long getGroupId(final int groupPosition) {
		return groupPosition;
	}

	@Override
	public View getGroupView(final int groupPosition, final boolean isExpanded, final View convertView,
			final ViewGroup parent) {

		TextView textView = getGenericView();
		textView.setText(getGroup(groupPosition).toString());
		return textView;
	}

	@Override
	public boolean isChildSelectable(final int groupPosition, final int childPosition) {
		return true;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}


	public void setSelected(final RadarCenter selectedRadar, final boolean notifyListener) {
		for (int i = 0; i < groups.length; i++) {
			for (int j = 0; j < this.children[i].length; j++) {
				RadarCenter current = children[i][j];
				if(current.getId().equals(selectedRadar.getId())) {
					selectChild(i, j, getChildId(i,j), notifyListener);
					return;
				}
			}
		}
	}

	private boolean selectChild(final int groupPosition, final int childPosition, final long id, final boolean notifyListener) {
		final RadarCenter radarKey = (RadarCenter) getChild(groupPosition, childPosition);
		if(notifyListener) {
			listener.itemSelected(radarKey);
		}
		this.groups[groupPosition] = radarKey.toString();
		radarComboList.collapseGroup(groupPosition);
		return true;
	}

}
