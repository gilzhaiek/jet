package com.reconinstruments.recontool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import android.app.Activity;
import android.app.ListActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ToolActivity extends ListActivity {
	
	String TAG = "TOOL";
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.item_list);

		try
		{
			String[] values = getResources().getAssets().list("scripts");

			// First paramenter - Context
			// Second parameter - Layout for the row
			// Third parameter - ID of the View to which the data is written
			// Forth - the Array of data
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_2, android.R.id.text1, values);

			// Assign adapter to ListView
			getListView().setAdapter(adapter);
		} catch (IOException e1)
		{
			e1.printStackTrace();
		}
	}
	
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id)
	{
		super.onListItemClick(l, v, position, id);
		
		TextView tv = (TextView) v.findViewById(android.R.id.text1);
		String fileName = tv.getText().toString();
		Log.d(TAG, "fileName: "+fileName);
		
		execCommandLine(loadScript("scripts/"+fileName));
	}


	public String[] loadScript(String fileName){
		ArrayList<String> script = new ArrayList<String>();
		
		try
		{
			//InputStream is = this.getResources().openRawResource(id);
			InputStream is = getAssets().open(fileName);
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String readLine = null;
			try {
				// While the BufferedReader readLine is not null 
				while ((readLine = br.readLine()) != null) {
					Log.d(TAG, readLine);
					script.add(readLine);
				}
				// Close the InputStream and BufferedReader
				is.close();
				br.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
			String[] array = new String[script.size()];
			for(int i=0;i<array.length;i++){
				array[i] = script.get(i);
			}
			return array;
		} catch (IOException e1)
		{
			e1.printStackTrace();
		}
		
		return new String[0];
	}
	void execCommandLine(String[] commands)
	{
		for(String command:commands){
			execCommandLine(command);
		}
	}
	void execCommandLine(String command)
	{
	    Runtime runtime = Runtime.getRuntime();
	    Process proc = null;
	    //OutputStreamWriter osw = null;
	    //StringBuilder sbstdOut = new StringBuilder();
	    //StringBuilder sbstdErr = new StringBuilder();

	    try
	    {
	        proc = runtime.exec(command);
	        //osw = new OutputStreamWriter(proc.getOutputStream());
	        //osw.write(command);
	        //osw.flush();
	        //osw.close();
	    }
	    catch (IOException ex)
	    {
	        Log.e("execCommandLine()", "Command resulted in an IO Exception: " + command);
	    }
	    finally
	    {
	        /*if (osw != null)
	        {
	            try
	            {
	                osw.close();
	            }
	            catch (IOException e){}
	        }*/
	    }

	    try 
	    {
	        proc.waitFor();
	    }
	    catch (InterruptedException e){}

	    Log.d(TAG, "process input stream:");
	    printStream(proc.getInputStream());
	    Log.d(TAG, "process output stream:");
	    printStream(proc.getErrorStream());
	    
	    if (proc.exitValue() != 0)
	    {
	        Log.e("execCommandLine()", "Command returned error: " + command + "\n  Exit code: " + proc.exitValue());

			Toast.makeText(this, "Command returned error: " + command + "\n  Exit code: " + proc.exitValue(), Toast.LENGTH_LONG);
	    }
	}
	void printStream(InputStream is){
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String readLine = null;
		try {
			// While the BufferedReader readLine is not null 
			while ((readLine = br.readLine()) != null) {
				Log.d(TAG, readLine);
				Toast.makeText(this, readLine, Toast.LENGTH_SHORT);
			}
			// Close the InputStream and BufferedReader
			is.close();
			br.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}