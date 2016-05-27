



public class PointXY 
{
	public float	x;
	public float   	y;

	public PointXY(float _x, float _y) {
		x= _x;
		y= _y;
	}
	
	
//============ parcelable protocol handlers

    public int describeContents() {
        return 0;
    }


}