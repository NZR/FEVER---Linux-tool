package fever;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropReader extends Properties
{

    private static final long serialVersionUID = 1L;

    
    public static String prj_file = "resources/settings.properties";
    
    public static void setPropReaderFile(String p)
    {
    	prj_file = p;
    }
    
    
	public PropReader() throws IOException
	{
		super();
		InputStream i = getClass().getClassLoader().getResourceAsStream(prj_file);
		if( i != null )
		{
			this.load(i);
		}
	}
}
