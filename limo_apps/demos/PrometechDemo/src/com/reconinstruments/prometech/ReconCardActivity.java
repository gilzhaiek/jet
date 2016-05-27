package com.reconinstruments.prometech;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.ImageView;

public class ReconCardActivity extends Activity {
	
	private HashMap<Integer, ReconCard> cards = new HashMap<Integer, ReconCard>();
	private ImageView mainImage;
	private int currentCard;
	
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mainImage = (ImageView) findViewById(R.id.main_image);
        
        prepareCardsFromXml();
        
        setImageIndex(1);
    }
    
    public void setImageIndex(int x) {
    	currentCard = x;
    	//mainImage.setImageBitmap(cards.get(currentCard).getImage());
    	mainImage.setImageResource(cards.get(currentCard).getImageResourceId());
    	Log.v("ReconCardActivity", "Card: " + Integer.toString(currentCard));
    }
    
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	if (cards.size() < 1)
    	{return false;}
    	
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
        	int nextCard = cards.get(currentCard).getRightIndex();
        	Log.v("ReconCardActivity", "NextCard: " + Integer.toString(nextCard));
        	if (nextCard > -1) {
        		setImageIndex(nextCard);
        	}
        }
        else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
        	int nextCard = cards.get(currentCard).getLeftIndex();
        	Log.v("ReconCardActivity", "NextCard: " + Integer.toString(nextCard));
        	if (nextCard > -1) {
        		setImageIndex(nextCard);
        	}
        }
        else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
        	int nextCard = cards.get(currentCard).getUpIndex();
        	Log.v("ReconCardActivity", "NextCard: " + Integer.toString(nextCard));
        	if (nextCard > -1) {
        		setImageIndex(nextCard);
        	}
        }
        else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
        	int nextCard = cards.get(currentCard).getDownIndex();
        	Log.v("ReconCardActivity", "NextCard: " + Integer.toString(nextCard));
        	if (nextCard > -1) {
        		setImageIndex(nextCard);
        	}
        }
        else if (keyCode == KeyEvent.KEYCODE_BACK) {
        	int nextCard = cards.get(currentCard).getBackIndex();
        	Log.v("ReconCardActivity", "NextCard: " + Integer.toString(nextCard));
        	if (nextCard > -1) {
        		setImageIndex(nextCard);
        	} else {
        		finish();
        	}
        } 
        else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
        	int nextCard = cards.get(currentCard).getSelectIndex();
        	Log.v("ReconCardActivity", "NextCard: " + Integer.toString(nextCard));
        	if (nextCard > -1) {
        		setImageIndex(nextCard);
        	}
        }
        else {
        	return false;
        }
        return true;
    }
    
    public void prepareCardsFromXml() {
		XmlResourceParser todolistXml = getResources().getXml(R.xml.stack);

		int eventType = -1;
		while (eventType != XmlResourceParser.END_DOCUMENT) {
			if (eventType == XmlResourceParser.START_TAG) {

				String strNode = todolistXml.getName();
				if (strNode.equals("ReconCard")) {
					int id = todolistXml.getAttributeIntValue(null, "id", 0);
					int up = todolistXml.getAttributeIntValue(null, "up", -1);
					int down = todolistXml.getAttributeIntValue(null, "down", -1);
					int left = todolistXml.getAttributeIntValue(null, "left", -1);
					int right = todolistXml.getAttributeIntValue(null, "right", -1);
					int img = todolistXml.getAttributeResourceValue(null, "img", 0);
					int select = todolistXml.getAttributeIntValue(null, "select", -1);
					int back = todolistXml.getAttributeIntValue(null, "back", -1);
					
					//Bitmap bit = BitmapFactory.decodeResource(getResources(), img);
					
					ReconCard temp = new ReconCard(this, img, left, right, up, down, select, back);
					if (id > 0) {
						cards.put(id, temp);
					}
				}
			}

			try {
				eventType = todolistXml.next();
			} catch (XmlPullParserException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}
    
}