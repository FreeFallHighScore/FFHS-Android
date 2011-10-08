package com.freefallhighscore.android;

import java.util.ArrayList;
import java.util.List;

import android.app.TabActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabContentFactory;
import android.widget.Toast;

/**
 * This activity allows you to have multiple views (in this case two {@link ListView}s)
 * in one tab activity.  The advantages over separate activities is that you can
 * maintain tab state much easier and you don't have to constantly re-create each tab
 * activity when the tab is selected.
 */
public class TabsActivity extends TabActivity implements OnTabChangeListener {

	private static final String LIST1_TAB_TAG = "List1";
	private static final String LIST2_TAB_TAG = "List2";

	// The two views in our tabbed example
	private ListView listView1;
	private ListView listView2;

	private TabHost tabHost;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

		tabHost = getTabHost();
		tabHost.setOnTabChangedListener(this);

		// setup list view 1
		listView1 = (ListView) findViewById(R.id.list1);

		// create some dummy strings to add to the list
		List<String> list1Strings = new ArrayList<String>();
		list1Strings.add("Item 1");
		list1Strings.add("Item 2");
		list1Strings.add("Item 3");
		list1Strings.add("Item 4");
		listView1.setAdapter(new ArrayAdapter(this, android.R.layout.simple_list_item_1, list1Strings));

		// setup list view 2
		listView2 = (ListView) findViewById(R.id.list2);

		// create some dummy strings to add to the list (make it empty initially)
		List<String> list2Strings = new ArrayList<String>();
		listView2.setAdapter(new ArrayAdapter(this, android.R.layout.simple_list_item_1, list2Strings));

		// add an onclicklistener to add an item from the first list to the second list
		listView1.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView parent, View view, int position, long id) {
				String item = (String) listView1.getAdapter().getItem(position);
				if(item != null) {
					((ArrayAdapter) listView2.getAdapter()).add(item);
					Toast.makeText(TabsActivity.this, item + " added to list 2", Toast.LENGTH_SHORT).show();
				}
			}
		});

		// add views to tab host
		tabHost.addTab(tabHost.newTabSpec(LIST1_TAB_TAG).setIndicator(LIST1_TAB_TAG).setContent(new TabContentFactory() {
			public View createTabContent(String arg0) {
				return listView1;
			}
		}));
		tabHost.addTab(tabHost.newTabSpec(LIST2_TAB_TAG).setIndicator(LIST2_TAB_TAG).setContent(new TabContentFactory() {
			public View createTabContent(String arg0) {
				return listView2;
			}
		}));

    }

	/**
	 * Implement logic here when a tab is selected
	 */
	public void onTabChanged(String tabName) {
		if(tabName.equals(LIST2_TAB_TAG)) {
			//do something
		}
		else if(tabName.equals(LIST1_TAB_TAG)) {
			//do something
		}
	}
}