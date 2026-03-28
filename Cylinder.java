

public class Cylinder 
{
	public static float NFMWheelRadiusGoldenRatio = 34f / 42f;
	public Vector3D Location;
	public float Radius;
	public float Width;
	public float NFMVanillaCorrectedWheelRadius;
	
	public Cylinder(Vector3D location, float radius, float width)
	{
		Location = location;
		Radius = radius;
		Width = width;
		NFMVanillaCorrectedWheelRadius = Radius * NFMWheelRadiusGoldenRatio;
	}
	
	public static Cylinder GetFromModel(Model model)
	{
		Vector3D location = new Vector3D(0f);
		float radius = 0f;
		float width = 0f;
		if(model != null)
		{
			if(!model.IsEmpty())
			{
				int minX = 2147483647;
		    	int maxX = -2147483648;
		    	int minY = minX;
		    	int maxY = maxX;
		    	int minZ = minX;
		    	int maxZ = maxX;
		    	for(Polygon poly : model.Polygons)
		    	{
		    		for(int i = 0; i < poly.NumPoints; i++)
		    		{
		    			int pX = poly.PointsX[i];
		    			int pY = poly.PointsY[i];
		    			int pZ = poly.PointsZ[i];
		    			minX = Math.min(minX, pX);
		    			maxX = Math.max(maxX, pX);
		    			minY = Math.min(minY, pY);
		    			maxY = Math.max(maxY, pY);
		    			minZ = Math.min(minZ, pZ);
		    			maxZ = Math.max(maxZ, pZ);
		    		}
		    	}
		    	
		    	Vector3D minV = new Vector3D(minX, minY, minZ);
		    	Vector3D maxV = new Vector3D(maxX, maxY, maxZ);
		    	location = Vector3D.Mid(minV, maxV);
		    	Vector3D fminV = Vector3D.Multiply(minV, Vector3D.VectorYZ);
		    	Vector3D fmaxV = Vector3D.Multiply(maxV, Vector3D.VectorYZ);
		    	Vector3D fLoc = Vector3D.Multiply(location, Vector3D.VectorYZ);
		    	radius = Math.max(Vector3D.Max(Vector3D.Distance(fLoc, fminV)), Vector3D.Max(Vector3D.Distance(fLoc, fmaxV)));
		    	width = Math.abs(maxV.X - minV.X);
			}
		}
		return new Cylinder(location, radius, width);
	}
}
