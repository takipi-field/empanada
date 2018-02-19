import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.net.Socket;

public class TakipiWrapper implements RequestStreamHandler {

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) {
        Map<String, String> environment = new HashMap<String,String>(System.getenv());
        RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
        List<String> args = new ArrayList<String>();
        for(String s:bean.getInputArguments()){
//          if(!s.contains("Xshare")) //before the wrapper this generated an error.
            args.add(s);
        }
        
        installWrapper(false, context);

        if(! (System.getenv("TAKIPI_DISABLE")!=null && System.getenv("TAKIPI_DISABLE").equals("TRUE"))){
          String takipi_home="/tmp/takipi";
          String takipi_lib=takipi_home+"/lib";
          installTakipi(context, takipi_home);
          environment.put("TAKIPI_HOME",takipi_home);
          environment.put("LD_LIBRARY_PATH",takipi_lib+":"+environment.get("LD_LIBRARY_PATH"));
          args.add("-agentlib:TakipiAgent");
          args.add("-Dtakipi.daemon.host="+System.getenv("TAKIPI_HOST"));
          args.add("-Dtakipi.daemon.port="+System.getenv("TAKIPI_PORT"));
        }
        
        environment.put("_HANDLER", environment.get("TAKIPI_HANDLER")+"::handleRequest");

        String path =System.getProperty("java.class.path"); //" -classpath "+
        args.add(" -classpath /var/task/:"+path); //TODO not sure if /var/task is enough..or should it be "."??
        
        //String cmd =System.getProperty("sun.java.command");
        //args.add(cmd);

        args.add("LambdaWrapper");
        //args.add("SayHello");

        run(true, context, "ps ax");
        int r = run(true, context, "/tmp/isrunning.sh LambdaWrapper");
        if(r!=0){
            context.getLogger().log("PROCESS WAS NOT RUNNING");
            //run(false, context, environment, args, "/tmp/runner.sh java", true);
            run(false, context, environment, args, "java", true);
        }
        call(context, 9090, inputStream, outputStream);
    }

    private void call(Context context, int port, InputStream inputStream, OutputStream outputStream) {
        try{
            Socket s = new Socket("localhost", port);
            Thread it = new Thread(new StreamPumper(inputStream, s.getOutputStream(),false));
            Thread ot = new Thread(new StreamPumper(s.getInputStream(), outputStream ,false));

            it.start();
            ot.start();
            it.join();
            s.shutdownOutput();
            ot.join();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void installWrapper(boolean print, Context context){
        int e = run(true, context, "ls /tmp/runner.sh");//Skip if already installed
        if(e!=0){
          run(true, context, "cp /var/task/runner.sh /tmp/");
          run(true, context, "cp /var/task/isrunning.sh /tmp/");
          run(true, context, "chmod +x /tmp/runner.sh");
          run(true, context, "chmod +x /tmp/isrunning.sh");
        }
    }
    public void installTakipi(Context context, String takipi_home){
        boolean print = System.getenv("TAKIPI_PRINT")!=null && System.getenv("TAKIPI_PRINT").equals("TRUE");
        int e = run(print, context, "ls "+takipi_home);//Skip if already installed
        if(e!=0){
          run(print, context, "mkdir "+takipi_home);//TODO -R??
          run(print, context, "curl https://s3.amazonaws.com/app-takipi-com/deploy/linux/takipi-latest.tar.gz -o /tmp/takipi.tgz");
          run(print, context, "tar xvf /tmp/takipi.tgz --strip-components=1 -C "+takipi_home);
        }
    }

  
    public int run(boolean printoutput, Context context, String cmd){
        return run(printoutput, context, null, null, cmd, false);
    }

    private int run(boolean printoutput, Context context, Map<String, String> environment, List<String> arguments, String cmd, boolean waitForReady){
        //TODO create function to prepare "la"
          ArrayList<String> l = new ArrayList<String>();
          if(environment != null)
            for(String k: environment.keySet()){
              l.add(k+"="+environment.get(k));
//              context.getLogger().log("---"+k+":::"+environment.get(k));
            }
          String[] la = new String[l.size()];
          la = l.toArray(la);

        try{
          if(arguments != null)
            cmd=cmd+" "+String.join(" ",arguments); 
          Process p = Runtime.getRuntime().exec(cmd, la);
          context.getLogger().log("$" + cmd);

          if(waitForReady){
              BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

              String line = "";
              while ((line = reader.readLine())!= null) {
                 context.getLogger().log(">>>"+line);
                 break;
              }
          }

          if(printoutput){
              p.waitFor();
              BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

              String line = "";
              while ((line = reader.readLine())!= null) {
                  context.getLogger().log(">" + line);
              }
          
              reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));

              while ((line = reader.readLine())!= null) {
                  context.getLogger().log(">>" + line);
              }
          }
          return p.exitValue();
        }catch(Exception e){
          //TODO 
        }
        return 1;
    }

}

