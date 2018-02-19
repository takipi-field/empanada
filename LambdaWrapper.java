import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.*;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.net.ServerSocket;
import java.net.Socket;
 
public class LambdaWrapper {

    public static void main(String[] args) throws Exception {

        ServerSocket listener = new ServerSocket(9090);
        System.out.println("READY");
        try {
            while (true) {
                Socket socket = listener.accept();
                try {
                    process(socket.getInputStream(),socket.getOutputStream());
                } finally {
                    socket.close();
                }
            }
        }
        finally {
            listener.close();
        }

    }

    private static void process(InputStream input, OutputStream output) throws Exception{
        Object handler = getHandler(System.getenv("TAKIPI_HANDLER"));
        //context
        if (handler instanceof RequestHandler) {
/*		Object result = ((RequestHandler) handler).handleRequest(inputObject, ctx);
		// try turning the output into json
		try {
			result = new ObjectMapper().writeValueAsString(result);
		} catch (JsonProcessingException jsonException) {
			// continue with results as it is
		}
		// The contract with lambci is to print the result to stdout, whereas logs go to stderr
		System.out.println(result);
*/
	} else if (handler instanceof RequestStreamHandler) {
		((RequestStreamHandler) handler).handleRequest(input, output, null);
                output.close();
	}

    }

    private static Object getHandler(String handlerName) throws Exception {
	Class<?> clazz = Class.forName(handlerName);
	return clazz.getConstructor().newInstance();
    }
}

