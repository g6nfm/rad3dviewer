
public class Vector3D 
{
	public float X;
	public float Y;
	public float Z;
	
	public Vector3D() {}
	public Vector3D(float xyz) {X = xyz; Y = xyz; Z = xyz;}
	public Vector3D(float x, float y, float z) {X = x; Y = y; Z = z;}
	
	public static Vector3D VectorYZ = new Vector3D(0f, 1f, 1f);
	
	public boolean Equals(Vector3D other)
	{
		if(other == null) return false;
		return X == other.X && Y == other.Y && Z == other.Z;
	}
	public static boolean Equals(Vector3D left, Vector3D right)
	{
		if(left == null || right == null) return left == right;
		return left.equals(right);
	}
	public static Vector3D Add(Vector3D A, Vector3D B)
	{
		return new Vector3D(A.X + B.X, A.Y + B.Y, A.Z + B.Z);
	}
	public static Vector3D Add(Vector3D A, float B)
	{
		return new Vector3D(A.X + B, A.Y + B, A.Z + B);
	}
	public void Add(Vector3D other)
	{
		X += other.X;
		Y += other.Y;
		Z += other.Z;
	}
	public void Add(float other)
	{
		X += other;
		Y += other;
		Z += other;
	}
	public static Vector3D Subtract(Vector3D A, Vector3D B)
	{
		return new Vector3D(A.X - B.X, A.Y - B.Y, A.Z - B.Z);
	}
	public static Vector3D Subtract(Vector3D A, float B)
	{
		return new Vector3D(A.X - B, A.Y - B, A.Z - B);
	}
	public void Subtract(Vector3D other)
	{
		X -= other.X;
		Y -= other.Y;
		Z -= other.Z;
	}
	public void Subtract(float other)
	{
		X -= other;
		Y -= other;
		Z -= other;
	}
	public static Vector3D Multiply(Vector3D A, Vector3D B)
	{
		return new Vector3D(A.X * B.X, A.Y * B.Y, A.Z * B.Z);
	}
	public static Vector3D Multiply(Vector3D A, float B)
	{
		return new Vector3D(A.X * B, A.Y * B, A.Z * B);
	}
	public void Multiply(Vector3D other)
	{
		X *= other.X;
		Y *= other.Y;
		Z *= other.Z;
	}
	public void Multiply(float other)
	{
		X *= other;
		Y *= other;
		Z *= other;
	}
	public static Vector3D Divide(Vector3D A, Vector3D B)
	{
		return new Vector3D(A.X / B.X, A.Y / B.Y, A.Z / B.Z);
	}
	public static Vector3D Divide(Vector3D A, float B)
	{
		return new Vector3D(A.X / B, A.Y / B, A.Z / B);
	}
	public void Divide(Vector3D other)
	{
		X /= other.X;
		Y /= other.Y;
		Z /= other.Z;
	}
	public void Divide(float other)
	{
		X /= other;
		Y /= other;
		Z /= other;
	}
	public static Vector3D Abs(Vector3D Value)
	{
		return new Vector3D(Math.abs(Value.X), Math.abs(Value.Y), Math.abs(Value.Z));
	}
	public void Abs()
	{
		X = Math.abs(X);
		Y = Math.abs(Y);
		Z = Math.abs(Z);
	}
	public static Vector3D Distance(Vector3D A, Vector3D B)
	{
		Vector3D result = Subtract(A, B);
		result.Abs();
		return result;
	}
	public static Vector3D MinComponents(Vector3D A, Vector3D B)
	{
		return new Vector3D(Math.min(A.X, B.X), Math.min(A.Y, B.Y), Math.min(A.Z, B.Z));
	}
	public static Vector3D MaxComponents(Vector3D A, Vector3D B)
	{
		return new Vector3D(Math.max(A.X, B.X), Math.max(A.Y, B.Y), Math.max(A.Z, B.Z));
	}
	public static float Sum(Vector3D Value)
	{
		return Value.X + Value.Y + Value.Z;
	}
	public static float Max(Vector3D Value)
	{
		return  Math.max(Value.X, Math.max(Value.Y, Value.Z));
	}
	public static float Min(Vector3D Value)
	{
		return  Math.min(Value.X, Math.min(Value.Y, Value.Z));
	}
	public static float Length(Vector3D Value)
	{
		 return (float)Math.sqrt(Sum(Multiply(Value, Value)));
	}
	public static Vector3D Mid(Vector3D A, Vector3D B)
	{
		Vector3D minComps = MinComponents(A,B);
		Vector3D maxComps = MaxComponents(A,B);
		Vector3D distance = Distance(maxComps, minComps);
		distance.Divide(2.0f);
		minComps.Add(distance);
		return minComps;
	}
}
