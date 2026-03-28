import java.util.*;
import java.util.List;


public class Model 
{
	public List<Polygon> Polygons;
	//public boolean IsWheelModel; //in case if u wanna reuse for body too.
	private Cylinder BoundingCylinder;
	private int userDefinedWidth, userDefinedRadius;
	
	public Model(int width, int radius)
	{
		Polygons = new ArrayList<>();
		userDefinedWidth = width;
		userDefinedRadius = radius;
	}
	
	public boolean IsEmpty()
	{
		if(Polygons == null) return true;
		return Polygons.isEmpty();
	}
	
	public void CalculateBoundingCylinder()
	{
		if(userDefinedWidth > 0 && userDefinedRadius > 0)
		{
			return;
		}
		BoundingCylinder = Cylinder.GetFromModel(this);
	}
	
	public float GetWidth()
	{
		if(userDefinedWidth > 0) return userDefinedWidth;
		if(BoundingCylinder == null)
			CalculateBoundingCylinder();
		return BoundingCylinder.Width;
	}
	
	public float GetRadius()
	{
		if(userDefinedRadius > 0) return userDefinedRadius;
		if(BoundingCylinder == null)
			CalculateBoundingCylinder();
		return BoundingCylinder.Radius;
	}
	
	public float GetNFMCorrectedRadius()
	{
		if(userDefinedRadius > 0) return ((float)userDefinedRadius) * Cylinder.NFMWheelRadiusGoldenRatio;
		if(BoundingCylinder == null)
			CalculateBoundingCylinder();
		return BoundingCylinder.NFMVanillaCorrectedWheelRadius;
	}
}


