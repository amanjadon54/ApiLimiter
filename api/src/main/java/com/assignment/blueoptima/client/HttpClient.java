package com.assignment.blueoptima.client;

import com.assignment.blueoptima.exception.ApiLimitException;
import com.assignment.blueoptima.exception.ExceptionConstants;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

/***
 * Rest Get client executor , to test the functionality.
 */
public class HttpClient {

    private static DefaultHttpClient httpClient;

    public static void executeHttpClient(String host, String path, String user) throws IOException {
        try {
            httpClient = new DefaultHttpClient();
            HttpGet getRequest = new HttpGet(host + path);
            getRequest.addHeader("userName", user);
            HttpResponse response = httpClient.execute(getRequest);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                HttpEntity httpEntity = response.getEntity();
                String apiOutput = EntityUtils.toString(httpEntity);
                throw new ApiLimitException("service execution failed", ExceptionConstants.prepareExceptionDetails(apiOutput));
            }
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }
}
