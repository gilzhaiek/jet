package com.reconinstruments.jumpdetection;


import android.os.Bundle;
public class ReconJump   
{
	/* 
	 * Constants 
	 */
	
	// standard ident tag
   // private static final String TAG = ReconJump.class.getSimpleName(); 
    
    public  static final String BROADCAST_ACTION_STRING = "RECON_MOD_BROADCAST_JUMP";
    
	// invalid (not read) values
    public final static int    INVALID_VALUE      = -10000;
    public final static int    INVALID_SEQUENCE   = -10000;
    public final static long   INVALID_DATE       = -10000;
    public final static int    INVALID_AIR        = -10000;
    public final static float  INVALID_DISTANCE   = -10000;
    public final static float  INVALID_DROP       = -10000;
    public final static float  INVALID_HEIGHT     = -10000;

    // Bundle IPC Key Names
    public static final String KEY_SEQUENCE = "Sequence";
    public static final String KEY_DATE     = "Date";
    public static final String KEY_AIR      = "Air";
    public static final String KEY_DISTANCE = "Distance";
    public static final String KEY_DROP     = "Drop";
    public static final String KEY_HEIGHT   = "Height";
 
    /* 
     * Static Class level Helpers
     */
    public static Bundle generateBundle(ReconJump jump)
    {
	   Bundle b = new Bundle();

	   b.putInt    (ReconJump.KEY_SEQUENCE, jump.getSequence() ) ;
	   b.putLong   (ReconJump.KEY_DATE,     jump.getDate() ) ;
	   b.putLong   (ReconJump.KEY_AIR,      jump.getAir() ) ;
	   b.putFloat  (ReconJump.KEY_DISTANCE, jump.getDistance() ) ;
	   b.putFloat  (ReconJump.KEY_DROP,     jump.getDrop() ) ;
	   b.putFloat  (ReconJump.KEY_HEIGHT,   jump.getHeight() ) ;

	   return b;
    }
    
    /* 
     * Instance Data Members 
     */
    
    // jump properties (making protected, don't want to expose outside package
    protected int     mSequence;
    protected long    mDate;
    protected long    mAir;
    protected float   mDistance;
    protected float   mDrop;
    protected float   mHeight;

    /* Public Accessors */
    public int   getSequence() {return mSequence;}
    public long  getDate()     {return mDate;}
    public long  getAir()      {return mAir;}
    public float getDistance() {return mDistance;}
    public float getDrop()     {return mDrop;}
    public float getHeight()   {return mHeight;}
    
    
    /* 
     * Construction 
     */
    
    // default c-tor -- initializes with invalid data 
    public ReconJump(){this.InitDefaults(); }
    
    // Initializing Constructor from Bundle -- useful on client end
    // when IPC transmits Jump Data inside Intent extras
    public ReconJump (Bundle b)
    {
	   mSequence = b.getInt   (ReconJump.KEY_SEQUENCE); 
	   mDate     = b.getLong  (ReconJump.KEY_DATE);  
	   mAir      = b.getLong  (ReconJump.KEY_AIR);
	   mDistance = b.getFloat (ReconJump.KEY_DISTANCE);
	   mDrop     = b.getFloat (ReconJump.KEY_DROP);
	   mHeight   = b.getFloat (ReconJump.KEY_HEIGHT);
    }

    /*
     * Internal Helpers
     */
    
    // initializes default (invalid) values
    private void InitDefaults()
    {
    	mSequence   = ReconJump.INVALID_SEQUENCE;
    	mDate       = ReconJump.INVALID_DATE;
    	mAir        = ReconJump.INVALID_AIR;
    	mDistance   = ReconJump.INVALID_DISTANCE ;
    	mDrop       = ReconJump.INVALID_DROP;
    	mHeight     = ReconJump.INVALID_HEIGHT;
    }
}
