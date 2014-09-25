package eu.fusepool.deduplication.transformer;



import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.hamcrest.core.StringContains;
import org.junit.Before;
import org.junit.Test;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;

import eu.fusepool.deduplication.transformer.FileUtil;
import eu.fusepool.deduplication.transformer.DuplicatesTransformer;
import eu.fusepool.p3.transformer.server.TransformerServer;

public class DuplicatesTransformerTest {
	
	//final String SILK_CONFIG_FILE = "src/test/resources/silk-config-file.xml";
	//final String INPUT_RDF_FILE = "src/main/resources/inputdata.ttl";
	private byte[] rdfData;
	private String baseUri;
	
	@Before
    public void setUp() throws Exception {
		File rdfFile = FileUtil.inputStreamToFile(getClass().getResourceAsStream("spatial-data-wgs84.ttl"));
		InputStream in = new FileInputStream(rdfFile);
        rdfData = IOUtils.toByteArray(in);
        in.close();
        final int port = findFreePort();
        baseUri = "http://localhost:"+port+"/";
        TransformerServer server = new TransformerServer(port);
        server.start(new DuplicatesTransformer());
    }

	
	@Test
	public void testSilk() throws IOException {
		Response response
        = RestAssured.given().header("Accept", "text/turtle")
        .contentType("text/turtle;charset=UTF-8")
        .content(rdfData)
        .expect().statusCode(HttpStatus.SC_OK).content(new StringContains("http://www.w3.org/2002/07/owl#sameAs")).header("Content-Type", "text/turtle").when()
        .post(baseUri);

		
		
		/*
        Graph graph = Parser.getInstance().parse(response.getBody().asInputStream(), "text/turtle");
        Iterator<Triple> typeTriples = graph.filter(null, RDF.type, 
                SimpleTransformer.TEXUAL_CONTENT);
        Assert.assertTrue("No type triple found", typeTriples.hasNext());
        Resource textDescription = typeTriples.next().getSubject();
        Assert.assertTrue("TextDescription resource is not a BNode", textDescription instanceof BNode);
        */
		
	}
	
	public static int findFreePort() {
        int port = 0;
        try (ServerSocket server = new ServerSocket(0);) {
            port = server.getLocalPort();
        } catch (Exception e) {
            throw new RuntimeException("unable to find a free port");
        }
        return port;
    }

}
