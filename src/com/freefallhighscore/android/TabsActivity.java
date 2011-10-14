package com.freefallhighscore.android;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ListActivity;
import android.app.TabActivity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TabHost;
import android.widget.Toast;

/**
 * This activity allows you to have multiple views (in this case two {@link ListView}s)
 * in one tab activity.  The advantages over separate activities is that you can
 * maintain tab state much easier and you don't have to constantly re-create each tab
 * activity when the tab is selected.
 */
public class TabsActivity extends TabActivity{

	TabHost tabHost;
    TabHost.TabSpec spec;


    static final ArrayList<HashMap<String,String>> myList = 
        	 new ArrayList<HashMap<String,String>>(); 

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tab_layout);
        
        
        Resources res = getResources();
        tabHost = getTabHost();
        tabHost.setup();
                
            
        ListView leaderboard      = (ListView) findViewById(R.id.leaderboard);
        ListView personalScores   = (ListView) findViewById(R.id.personalScores);
        LinearLayout instructions = (LinearLayout) findViewById(R.id.instructions);
        
        spec = tabHost
        		.newTabSpec("Leaderboard")
        		.setIndicator("Leaderboard",res.getDrawable(R.drawable.ic_leaderboard_tab))
        		.setContent(R.id.leaderboard);
        tabHost.addTab(spec);
        
        spec = tabHost
        		.newTabSpec("My Drops")
        		.setIndicator("My Drops",res.getDrawable(R.drawable.ic_mydrops_tab))
        		.setContent(R.id.personalScores);
        tabHost.addTab(spec);

        spec = tabHost
        		.newTabSpec("Instructions")
        		.setIndicator("Instructions",res.getDrawable(R.drawable.ic_instructions_tab))
        		.setContent(R.id.instructions);
        tabHost.addTab(spec);
        
        
        SimpleAdapter adapter = new SimpleAdapter(
        		this,
        		myList,
        		R.layout.leaderboard_item,
        		new String[] {"standing","score","user", "thumbnail_url"},
        		new int[] {R.id.standingText,R.id.scoreText, R.id.userText, R.id.videoThumb}
        		);
        populateList();
  		leaderboard.setAdapter(adapter);
  		
  		leaderboard.setOnItemClickListener(new OnItemClickListener() {
  			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
  				Log.i("State", "view " + view);
  				//Toast.makeText(getApplicationContext(), "position: " + position, Toast.LENGTH_SHORT).show();
  			}
  		});
  		
  	
    }

    private void populateList() {
    	HashMap<String,String> temp = new HashMap<String,String>();
    	        
        try {
            URL url = new URL(
                    "http://freefallhighscore.com/videos.json");
            URLConnection connection = url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    connection.getInputStream()));
 
            String line;
            while ((line = in.readLine()) != null) {
                JSONArray ja = new JSONArray(line);
 
                for (int i = 0; i < ja.length(); i++) {
                    JSONObject jo = (JSONObject) ja.get(i);
                    Log.i("State", jo.getString("rank"));
                	temp.put("standing",jo.getString("rank"));
                	float milliseconds = Float.valueOf(jo.getString("drop_time"));
                	temp.put("score", String.valueOf(milliseconds/1000.0));
                	temp.put("user", jo.getString("author"));
                	temp.put("video_url", jo.getString("video_url"));
                	temp.put("thumbnail_url", jo.getString("thumbnail_url"));
                	myList.add(temp);
                	temp = new HashMap<String,String>();
                }
            }
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    /*
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.logout_menu, menu);
        return true;
        
    }
    */
    
}