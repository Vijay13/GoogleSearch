
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

/**
 * This program encodes command-line arguments as a Google search query,
 * downloads the results, and displays the corresponding links as output.
 */
public class GoogleSearch {

    /**
     * The main entry point of the program.
     * 
     * @param args
     *            The command-line arguments. These arguments are encoded as a
     *            Google search query.
     */
    public static void main(String[] args) {
        // Check for usage errors.
        if (args.length == 0) {
            System.out.println("usage: GoogleSearch query ...");
            return;
        }

        // Catch IO errors that may occur while encoding the query, downloading
        // the results, or parsing the downloaded content.
        try {
            // Encode the command-line arguments as a Google search query.
            URL url = encodeGoogleQuery(args);

            // Download the content from Google.
            System.out.println("Downloading [" + url + "]...\n");
            String html = downloadString(url);

            // Parse and display the links.
            List<URL> links = parseGoogleLinks(html);
            for (URL link : links){
            	//System.out.println( getPageTitle(link) );
            	System.out.println("  " + link);
            }
            
            // Download search result pages.
            downloadPages(links);

        } catch (IOException e) {
            // Display an error if anything fails.
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Reads all contents from an input stream and returns a string from the
     * data.
     * 
     * @param stream
     *            The input stream to read.
     * 
     * @return A string built from the contents of the input stream.
     * 
     * @throws IOException
     *             Thrown if there is an error reading the stream.
     */
    private static String downloadString(InputStream stream)
            throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int ch;
        while (-1 != (ch = stream.read()))
            out.write(ch);
        return out.toString();
    }

    /**
     * Downloads the contents of a URL as a String. This method alters the
     * User-Agent of the HTTP request header so that Google does not return
     * Error 403 Forbidden.
     * 
     * @param url
     *            The URL to download.
     * 
     * @return The content downloaded from the URL as a string.
     * 
     * @throws IOException
     *             Thrown if there is an error downloading the content.
     */
    private static String downloadString(URL url) throws IOException {
        String agent = "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US)";
        URLConnection connection = url.openConnection();
        connection.setRequestProperty("User-Agent", agent);
        String redirect = connection.getHeaderField("Location");
        if (redirect != null){
            connection = new URL(redirect).openConnection();
        }
        InputStream stream = connection.getInputStream();
        return downloadString(stream);
    }

    /**
     * Encodes a string of arguments as a URL for a Google search query.
     * 
     * @param args
     *            The array of arguments to pass to Google's search engine.
     * 
     * @return A URL for a Google search query based on the arguments.
     */
    private static URL encodeGoogleQuery(String[] args) {
        try {
            StringBuilder localAddress = new StringBuilder();
            localAddress.append("/search?q=");

            for (int i = 0; i < args.length; i++) {
                String encoding = URLEncoder.encode(args[i], "UTF-8");
                localAddress.append(encoding);
                if (i + 1 < args.length)
                    localAddress.append("+");
            }

            return new URL("http", "www.google.com", localAddress.toString());

        } catch (IOException e) {
            // Errors should not occur under normal circumstances.
            throw new RuntimeException(
                    "An error occurred while encoding the query arguments.");
        }
    }

    /**
     * Parses HTML output from a Google search and returns a list of
     * corresponding links for the query. The parsing algorithm is crude and may
     * not work if Google changes the output of their results. This method works
     * adequately as of February 6, 2015.
     * 
     * @param html
     *            The HTML output from Google search results.
     * 
     * @return A list of links for the query.
     * 
     * @throws IOException
     *             Thrown if there is an error parsing the results from Google
     *             or if one of the links returned by Google is not a valid URL.
     */
    private static List<URL> parseGoogleLinks(String html)
            throws IOException {
        // These tokens are adequate for parsing the HTML from Google. First,
        // find a heading-3 element with an "r" class. Then find the next anchor
        // with the desired link. The last token indicates the end of the URL
        // for the link.
        String token1 = "<h3 class=\"r\">";
        String token2 = "http";
        String token3 = "&amp;sa=U&amp";

        List<URL> links = new ArrayList<URL>();

        try {
            // Loop until all links are found and parsed. Find each link by
            // finding the beginning and ending index of the tokens defined
            // above.
            int index = 0;
            while (-1 != (index = html.indexOf(token1, index))) {
                int result = html.indexOf(token2, index);
                int urlStart = result; //+ token2.length();
                int urlEnd = html.indexOf(token3, result);
                if(urlStart < urlEnd){
                    String urlText = html.substring(urlStart, urlEnd);
                    URL url = new URL( URLDecoder.decode( urlText, "UTF-8" ) );
                    links.add(url);
                }

                index = urlEnd + token3.length();
            }

            return links;

        } catch (MalformedURLException e) {
        	System.out.println( e.getLocalizedMessage());
            throw new IOException("Failed to parse Google links 1.");
        } catch (IndexOutOfBoundsException e) {
            throw new IOException("Failed to parse Google links 2.");
        }
    }
    
    /**
     * Downloads the contents of URLs and write it into /working/directory/html/result_index.html
     * 
     * @param url
     *            The list of URLs to download.
     * 
     * 
     * @throws IOException
     *             Thrown if there is an error downloading the content.
     */
    private static void downloadPages(List<URL> links){
			String currentDir = System.getProperty("user.dir");
			File theDir = new File(currentDir + "/html");
			theDir.mkdir();
			int i = 0;
	    	for (URL link : links){
	    		String page;
	    		try{
	    			page = downloadString(link);
		    		PrintWriter out = new PrintWriter(theDir.getPath() + "/result_" + i + ".html" , "UTF-8");
		    		out.println(page);
		    		out.close();
		    		System.out.println("Downloaded: " + link.toString());
	    		}catch(Exception e){
	    			System.out.println(e.getMessage());
	    		}
	    		i++;
	        }
    }
    
    /**
     * Downloads the contents of URLs and extract page title. 
     * The result will not contain any special characters.
     * 
     * @param url
     *            The URL of page whose title is to be read.
     * 
     * @return Title of page located at url
     * 
     * @throws IOException
     *             Thrown if there is an error downloading the content.
     */
    
    private static String getPageTitle( URL url ){
    	String html;
    	String title = "";
		try {
			html = downloadString(url);
			int titleStart = html.indexOf("<title>") + 7;
	    	int titleEnd = html.indexOf("</title>");
	    	if(titleStart < titleEnd){
	    		title = html.substring(titleStart, titleEnd);
	    	}
	    	while(title.contains("&#")){
	    		int start = title.indexOf("&#");
	    		int end = title.indexOf(";", start) + 1;
	    		if(start < end){
		    		String sub = title.substring(start, end);
		    		title = title.replace(sub, "");	    			
	    		}else{
	    			break;
	    		}
	    	}
	    	while(title.contains("&") && title.contains(";")){
	    		int start = title.indexOf("&");
	    		int end = title.indexOf(";", start) + 1;
	    		if(start < end){
		    		String sub = title.substring(start, end);
		    		title = title.replace(sub, "");	    			
	    		}else{
	    			break;
	    		}
	    	}
	    	return title.replace("\n", "").replace("\r", "").replace("\t", "");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return title;
    }
}

