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

import com.freefallhighscore.android.youtube.OAuthConfig;
import com.freefallhighscore.android.youtube.YouTubeClient;
import com.freefallhighscore.android.youtube.YouTubeProfile;

import android.app.ListActivity;
import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
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
import android.widget.ScrollView;
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
    boolean listPopulated = false;
    SimpleAdapter adapter;
    private boolean loggedIn;
    private String userName;
    
    static final ArrayList<HashMap<String,String>> leaderboardList = 
        	 new ArrayList<HashMap<String,String>>(); 
    static final ArrayList<HashMap<String,String>> myDropsList = 
       	 new ArrayList<HashMap<String,String>>(); 
    
    private ListView   leaderboard, myDrops;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tab_layout);
        
        Resources res = getResources();
        tabHost = getTabHost();
        tabHost.setup();
                
            
        leaderboard    = (ListView)   findViewById(R.id.leaderboard);
        myDrops         = (ListView)   findViewById(R.id.myDrops);
        
        ScrollView instructions   = (ScrollView) findViewById(R.id.instructions);
        ScrollView about		  = (ScrollView) findViewById(R.id.about);

        
        spec = tabHost
        		.newTabSpec("Leaderboard")
        		.setIndicator("Leaderboard",res.getDrawable(R.drawable.ic_leaderboard_tab))
        		.setContent(R.id.leaderboard);
        tabHost.addTab(spec);
        
        spec = tabHost
        		.newTabSpec("My Drops")
        		.setIndicator("My Drops",res.getDrawable(R.drawable.ic_mydrops_tab))
        		.setContent(R.id.myDrops);
        tabHost.addTab(spec);

        spec = tabHost
        		.newTabSpec("Instructions")
        		.setIndicator("Instructions",res.getDrawable(R.drawable.ic_instructions_tab))
        		.setContent(R.id.instructions);
        tabHost.addTab(spec);
        
        spec = tabHost
        		.newTabSpec("About")
        		.setIndicator("About",res.getDrawable(R.drawable.ic_about_tab))
        		.setContent(R.id.about);
        tabHost.addTab(spec);

        Intent creator = getIntent();
        loggedIn = creator.getBooleanExtra("loggedIn", false);
        
        if(!listPopulated){
	        populateList("author", "http://freefallhighscore.com/videos.json", leaderboardList, leaderboard);
	        if(loggedIn){
	        	new Thread(new RetrieveYouTubeProfile()).start();
	        	//populateList("author", "http://freefallhighscore.com/videos.json", myDropsList, myDrops);
	        }

	        listPopulated = true;
        }
    }
    
	private class RetrieveYouTubeProfile implements Runnable {
		@Override
		public void run() {
			try {
				// TODO: this should probably be trigger by authactivity returning. but that makes things more complicated,
				// so this will do for now. We should also check whether or not we have the username when we first load
				// this activity
				YouTubeClient client = YouTubeClient.buildAuthorizedClient(OAuthConfig.getInstance(TabsActivity.this), getString(R.string.devId));
				YouTubeProfile profile = client.getYouTubeProfile();
				
				userName = profile.username;
				userName = "neufuture";
				
				// TODO: maybe store the username in whatever global storage we settle on
				Log.d("MYDROPS", profile.username);
				
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						populateList("author", "http://freefallhighscore.com/users/" + userName + "/videos.json", myDropsList, myDrops);
					}
				});
			} catch (IOException e) {
				Log.e("MYDROPS", "Error", e);
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						
					}
				});
			}
		}
	}
    
    private void populateList(String extraText, String jsonUrl, final ArrayList<HashMap<String, String>> list, ListView lv) {
    	HashMap<String,String> temp = new HashMap<String,String>();
    	        
        try {
            URL url = new URL(jsonUrl);
            URLConnection connection = url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    connection.getInputStream()));
 
            String line;
            while ((line = in.readLine()) != null) {
            	Log.i("MYDROPS", "READ JSON FROM " + jsonUrl + " :: " + line);
            	JSONArray ja = new JSONArray(line);
            	if(ja.length() == 0){
            		temp.put("extraText", "You have no videos. Drop one!");
            		list.add(temp);
            		return;
            	}
                for (int i = 0; i < ja.length(); i++) {
                    JSONObject jo = (JSONObject) ja.get(i);
                    int rank = Integer.valueOf(jo.getString("rank"));
                    switch(rank){
                    case 1:
                    	temp.put("standing", "LEADER");
                    	break;
                    case 2:
                    	temp.put("standing", "SECOND");
                    	break;
                    case 3:
                    	temp.put("standing", "THIRD");
                    	break;
                    default:
                    	temp.put("standing", String.valueOf(rank));
                    	break;
                    }                    
                   	float milliseconds = Float.valueOf(jo.getString("drop_time"));
                	temp.put("score", String.valueOf(milliseconds/1000.0));
                	temp.put("extraText", jo.getString(extraText));
                	temp.put("video_url", jo.getString("video_url"));
                	temp.put("thumbnail_url", jo.getString("thumbnail_url"));
                	
                	adapter = new SimpleAdapter(
                    		this,
                    		list,
                    		R.layout.list_item,
                    		new String[] {"standing","score","extraText"},
                    		new int[] {R.id.standingText,R.id.scoreText, R.id.extraText}
                    		);
                	
                	list.add(temp);
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
  		lv.setAdapter(adapter);
  		
  		lv.setOnItemClickListener(new OnItemClickListener() {
  			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
  				Log.i("State", "position " + position);
  				HashMap<String, String> hm = list.get(position);
  		        String videoUrl = hm.get("video_url");
  				Log.i("State", "url " + videoUrl);
  				Intent browse = new Intent( Intent.ACTION_VIEW , Uri.parse( videoUrl ) );
  			    startActivity( browse );
  			}
  		});
    }
    /*
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.logout_menu, menu);
        return true;
        
    }
    */
    

    
}