package eu.fusepool.deduplication.transformer;



import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.util.Iterator;

import org.apache.clerezza.rdf.core.BNode;
import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.ontologies.OWL;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.hamcrest.core.StringContains;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;

import eu.fusepool.deduplication.transformer.FileUtil;
import eu.fusepool.deduplication.transformer.DuplicatesTransformer;
import eu.fusepool.p3.transformer.sample.SimpleTransformer;
import eu.fusepool.p3.transformer.server.TransformerServer;

public class DuplicatesTransformerTest {
	
	private byte[] rdfData;
	private String baseUri;
	
	@Before
    public void setUp() throws Exception {
		// prepare the data to be posted to the service
		File rdfFile = FileUtil.inputStreamToFile(getClass().getResourceAsStream("spatial-data-wgs84.ttl"), "test-", ".ttl");
		InputStream in = new FileInputStream(rdfFile);
        rdfData = IOUtils.toByteArray(in);
        in.close();
        rdfFile.delete();
        
        final int port = findFreePort();
        baseUri = "http://localhost:"+port+"/";
        TransformerServer server = new TransformerServer(port);
        server.start(new DuplicatesTransformer("silk-config-test-spatial.xml"));
    }

	
	@Test
	public void testSilk() throws IOException {
		Response response
        = RestAssured.given().header("Accept", "text/turtle")
        .contentType("text/turtle;charset=UTF-8")
        .content(rdfData)
        .expect().statusCode(HttpStatus.SC_OK).content(new StringContains("http://www.w3.org/2002/07/owl#sameAs")).header("Content-Type", "text/turtle").when()
        .post(baseUri);

		Graph graph = Parser.getInstance().parse(response.getBody().asInputStream(), "text/turtle");
        Iterator<Triple> typeTriples = graph.filter(null, OWL.sameAs, null);
        Assert.assertTrue("No equivalent entities found", typeTriples.hasNext());
		
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
