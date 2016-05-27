package com.reconinstruments.mapImages.prim;

public class Vector3
{
	public float x, y, z;

	public Vector3()
	{
		set(0, 0, 0);
	}
	
	public Vector3(Vector3 v)
	{
		set(v.x, v.y, v.z);
	}
	
	public Vector3(float x, float y, float z)
	{
		set(x, y, z);
	}

	public Vector3 set(float x, float y, float z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
		return this;
	}
	
	public Vector3 set(Vector3 v)
	{
		return set(v.x, v.y, v.z);
	}

	public Vector3 copy(Vector3 v)
	{
		return set(v.x, v.y, v.z);
	}

	public float dot(Vector3 v)
	{
		return x * v.x + y * v.y + z * v.z;
	}

	public Vector3 cross(Vector3 v)
	{
		return set((y * v.z) - (z * v.y),
					 (z * v.x) - (x * v.z), 
					 (x * v.y) - (y * v.x));
	}

	public Vector3 add(Vector3 v)
	{
		return set(x + v.x, y + v.y, z + v.z);
	}
	
	public Vector3 add(float x, float y, float z)
	{
		return set(this.x + x, this.y + y, this.z + z);
	}

	public Vector3 sub(Vector3 v)
	{
		return set(x - v.x, y - v.y, z - v.z);
	}

	public Vector3 mult(float s)
	{
		return set(x * s, y * s, z * s);
	}

	public Vector3 div(float s)
	{
		return set(x / s, y / s, z / s);
	}

	public float magnitude()
	{
		return (float)Math.sqrt((x * x) + (y * y) + (z * z));
	}

	public float distance(Vector3 v)
	{
		float dX = x - v.x;
		float dY = y - v.y;
		float dZ = z - v.z;		
		return (float)Math.sqrt((dX * dX) + (dY * dY) + (dZ * dZ));
	}

	public Vector3 normalize()
	{
		return div(magnitude());
	}

	public String toString()
	{
		return "[" + x + ", " + y + ", " + z + "]";
	}
}
