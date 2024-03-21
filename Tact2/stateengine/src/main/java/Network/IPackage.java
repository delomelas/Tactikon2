package Network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class IPackage
{
	public PackageResponse mReturnCode;
	
	abstract public void QueryToBinary(DataOutputStream stream) throws IOException;
	
	abstract public void BinaryToQuery(DataInputStream stream) throws IOException;
	
	abstract public void ResponseToBinary(DataOutputStream stream) throws IOException;
	
	abstract public void BinaryToResponse(DataInputStream stream) throws IOException;
			
}
