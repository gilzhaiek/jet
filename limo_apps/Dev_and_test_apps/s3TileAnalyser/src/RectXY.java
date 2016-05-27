



public class RectXY  	// designed for world coordinates where top > bottom (implemented to avoid confusion related to RectF usage, where Y is +ve down)
{
	public float	left;		 
	public float   	top;
	public float 	right;
	public float	bottom;

	public RectXY(float _left, float _top, float _right, float _bottom) {
		left = _left;
		top = _top;
		right = _right;
		bottom = _bottom;
	}
	
	
//============ parcelable protocol handlers

    public int describeContents() {
        return 0;
    }

    
// ===========
// methods
	
	public boolean Contains(float _x, float _y) {
		return (_x>=left && _x<=right && _y>=bottom && _y<=top);
	}
	
	public boolean Contains(RectXY _rect) {
		return (_rect.left >= left && _rect.right <= right && _rect.top <= top && _rect.bottom >= bottom);
	}
	
	public boolean Intersects(RectXY _rect) {
		return !(_rect.right<left || _rect.left>right || _rect.bottom >top || _rect.top< bottom);
	}
	
}