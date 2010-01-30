package org.jvnet.hudson.l10n;

import com.sun.mail.util.BASE64DecoderStream;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.http.Cookie;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

/**
 * @author Kohsuke Kawaguchi
 */
public class App {
    public static final class SubmissionEntry {
        public final String text, baseName, key, original;

        @DataBoundConstructor
        public SubmissionEntry(String text, String baseName, String key, String original) {
            this.text = text;
            this.baseName = baseName;
            this.key = key;
            this.original = original;
        }

        public boolean isUpdated() {
            return !original.equals(text);
        }
    }

    public void doSubmit(StaplerRequest req, StaplerResponse rsp) throws IOException {
        System.out.println("Accepting a submission");

        Cookie c = findIDCookie(req);
        if (c==null) {
            c = new Cookie("ID",UUID.randomUUID().toString());
            c.setPath("/");
            c.setMaxAge(60*60*24*365*10); // 10 years
            rsp.addCookie(c);
        }

        String queryString = req.getQueryString();

        handleSubmission(req, c, queryString);

        // send back the response
        rsp.setStatus(200);
        rsp.setContentType("text/javascript");
        PrintWriter out = rsp.getWriter();
        out.println("");
        out.close();
    }

    /**
     * Processes submission.
     *
     * @param req
     *      Can be null.
     * @param c
     *      Can be null.
     */
    private void handleSubmission(StaplerRequest req, Cookie c, String queryString) throws IOException {
        String id = UUID.randomUUID().toString();

        String dir = System.getenv("DATADIR");
        if(dir==null)   dir=".";
        File f = new File(dir);

        String js = IOUtils.toString(new GZIPInputStream(new BASE64DecoderStream(new ByteArrayInputStream(queryString.getBytes()))),"UTF-8");
        JSONObject json = JSONObject.fromObject(js);

        filter(json);

        // place the data in a locale specific directory
        f = new File(f,json.getString("locale"));
        f.mkdirs();

        FileUtils.writeStringToFile(new File(f,id+".json"),json.toString(2),"UTF-8");

        Properties props = new Properties();
        if (req!=null) {
            props.setProperty("remoteHost",req.getRemoteHost());
            props.setProperty("referer",req.getReferer());
        }
        if (c!=null) {
            props.setProperty("cookie",c.getValue());
        }
        props.setProperty("timestamp",String.valueOf(System.currentTimeMillis()));
        props.setProperty("date",new Date().toString());
        props.setProperty("submitter",json.getString("submitter"));
        props.setProperty("id",json.getString("id"));
        props.setProperty("version",json.getString("version"));

        FileOutputStream os = new FileOutputStream(new File(f,id+".properties"));
        try {
            props.store(os,null);
        } finally {
            os.close();
        }
    }

    /**
     * Filter out entries that haven't changed.
     */
    private void filter(JSONObject json) {
        Object e = json.get("entry");
        if (e instanceof JSONObject) {
            JSONArray a = new JSONArray();
            a.add(e);
            json.put("entry",a);
            e=a;
        }
        if (e instanceof JSONArray) {
            JSONArray a = (JSONArray) e;
            for (JSONObject o : new ArrayList<JSONObject>(a)) {
                if(o.getString("original").equals(o.getString("text"))) {
                    a.remove(o);
                }
            }
        }
    }

    private Cookie findIDCookie(StaplerRequest req) {
        Cookie[] cookies = req.getCookies();
        if(cookies==null)   return null;
        
        for (Cookie c : cookies) {
            if(c.getName().equals("ID"))
                return c;
        }
        return null;
    }

    /**
     * Replay from Apache log files in stdin.
     */
    public static void main(String[] args) throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        App app = new App();
        String line;
        while ((line=in.readLine())!=null) {
            int idx = line.indexOf("GET /l10n/submit");
            if (idx<0)  continue;   // not a submission

            String submission = line.substring(idx); // GET /l10n/submit?... HTTP/1.0" ...
            String[] tokens = submission.split(" ");

            String query = tokens[1].substring(tokens[1].indexOf('?')+1);
            try {
                app.handleSubmission(null,null,query);
            } catch (IOException e) {
                System.err.println("Failed to handle submission: "+line);
            }
        }
    }
}
