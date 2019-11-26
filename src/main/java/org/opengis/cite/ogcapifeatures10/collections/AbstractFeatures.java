package org.opengis.cite.ogcapifeatures10.collections;

import static org.opengis.cite.ogcapifeatures10.EtsAssert.assertTrue;
import static org.opengis.cite.ogcapifeatures10.OgcApiFeatures10.GEOJSON_MIME_TYPE;
import static org.opengis.cite.ogcapifeatures10.SuiteAttribute.IUT;
import static org.opengis.cite.ogcapifeatures10.collections.FeaturesAssertions.assertNumberMatched;
import static org.opengis.cite.ogcapifeatures10.collections.FeaturesAssertions.assertNumberReturned;
import static org.opengis.cite.ogcapifeatures10.collections.FeaturesAssertions.assertTimeStamp;
import static org.opengis.cite.ogcapifeatures10.openapi3.OpenApiUtils.retrieveTestPointsForCollections;
import static org.opengis.cite.ogcapifeatures10.util.JsonUtils.findLinkByRel;
import static org.opengis.cite.ogcapifeatures10.util.JsonUtils.findLinksWithSupportedMediaTypeByRel;
import static org.opengis.cite.ogcapifeatures10.util.JsonUtils.findLinksWithoutRelOrType;
import static org.opengis.cite.ogcapifeatures10.util.JsonUtils.findUnsupportedTypes;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.opengis.cite.ogcapifeatures10.CommonDataFixture;
import org.opengis.cite.ogcapifeatures10.SuiteAttribute;
import org.opengis.cite.ogcapifeatures10.openapi3.TestPoint;
import org.testng.ITestContext;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;

import com.reprezen.kaizen.oasparser.model3.Parameter;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

/**
 * @author <a href="mailto:goltz@lat-lon.de">Lyn Goltz </a>
 */
public class AbstractFeatures extends CommonDataFixture {

    protected final Map<String, ResponseData> collectionIdAndResponse = new HashMap<>();

    protected List<Map<String, Object>> collections;

    protected URI iut;

    @DataProvider(name = "collectionPaths")
    public Iterator<Object[]> collectionPaths( ITestContext testContext ) {
        this.iut = (URI) testContext.getSuite().getAttribute( IUT.getName() );
        List<TestPoint> testPointsForCollections = retrieveTestPointsForCollections( getApiModel(), iut,
                                                                                     noOfCollections );
        List<Object[]> collectionsData = new ArrayList<>();
        for ( TestPoint testPointForCollections : testPointsForCollections ) {
            collectionsData.add( new Object[] { testPointForCollections } );
        }
        return collectionsData.iterator();
    }

    @BeforeClass
    public void retrieveRequiredInformationFromTestContext( ITestContext testContext ) {
        this.collections = (List<Map<String, Object>>) testContext.getSuite().getAttribute( SuiteAttribute.COLLECTIONS.getName() );
    }

    /**
     * Abstract Test 22, Test Method 1
     *
     * <pre>
     * Abstract Test 22: /ats/core/fc-response
     * Test Purpose: Validate that the Feature Collections complies with the require structure and contents.
     * Requirement: /req/core/fc-response
     *
     * Test Method
     *   1. Validate that the type property is present and has a value of FeatureCollection
     * </pre>
     *
     * @param collection
     *            the collection under test, never <code>null</code>
     */
    public void validateTypeProperty( Map<String, Object> collection ) {
        String collectionId = (String) collection.get( "id" );
        ResponseData response = collectionIdAndResponse.get( collectionId );
        if ( response == null )
            throw new SkipException( "Could not find a response for collection with id " + collectionId );

        JsonPath jsonPath = response.jsonPath();
        String type = jsonPath.get( "type" );
        assertNotNull( type, "type property is missing" );
        assertEquals( type, "FeatureCollection", "Expected type property value of FeatureCollection but was " + type );
    }

    /**
     * Abstract Test 22, Test Method 2
     *
     * <pre>
     * Abstract Test 22: /ats/core/fc-response
     * Test Purpose: Validate that the Feature Collections complies with the require structure and contents.
     * Requirement: /req/core/fc-response
     *
     * Test Method
     *   2. Validate the features property is present and that it is populated with an array of feature items.
     * </pre>
     *
     * @param collection
     *            the collection under test, never <code>null</code>
     */
    void validateFeaturesProperty( Map<String, Object> collection ) {
        String collectionId = (String) collection.get( "id" );
        ResponseData response = collectionIdAndResponse.get( collectionId );
        if ( response == null )
            throw new SkipException( "Could not find a response for collection with id " + collectionId );

        JsonPath jsonPath = response.jsonPath();
        String type = jsonPath.get( "features" );
        assertNotNull( type, "features property is missing" );
    }

    /**
     * Abstract Test 22, Test Method 4 (Abstract Test 23)
     *
     * <pre>
     * Abstract Test 22: /ats/core/fc-response
     * Test Purpose: Validate that the Feature Collections complies with the require structure and contents.
     * Requirement: /req/core/fc-response
     *
     * Test Method
     *   4. If the links property is present, validate that all entries comply with /ats/core/fc-links
     * </pre>
     *
     * <pre>
     * Abstract Test 23: /ats/core/fc-links
     * Test Purpose: Validate that the required links are included in the Collections document.
     * Requirement: /req/core/fc-links, /req/core/fc-rel-type
     *
     * Test Method:
     * Verify that the response document includes:
     *   1. a link to this response document (relation: self),
     *   2. a link to the response document in every other media type supported by the server (relation: alternate).
     *
     * Verify that all links include the rel and type link parameters.
     * </pre>
     *
     * @param collection
     *            the collection under test, never <code>null</code>
     */
    void validateLinks( Map<String, Object> collection ) {
        String collectionId = (String) collection.get( "id" );
        ResponseData response = collectionIdAndResponse.get( collectionId );
        if ( response == null )
            throw new SkipException( "Could not find a response for collection with id " + collectionId );

        JsonPath jsonPath = response.jsonPath();
        List<Map<String, Object>> links = jsonPath.getList( "links" );

        // 1. a link to this response document (relation: self)
        Map<String, Object> linkToSelf = findLinkByRel( links, "self" );
        assertNotNull( linkToSelf, "Feature Collection Metadata document must include a link for itself" );

        // 2. a link to the response document in every other media type supported by the server (relation: alternate)
        // Dev: Supported media type are identified by the compliance classes for this server
        List<String> mediaTypesToSupport = createListOfMediaTypesToSupportForFeatureCollectionsAndFeatures( linkToSelf );
        List<Map<String, Object>> alternateLinks = findLinksWithSupportedMediaTypeByRel( links, mediaTypesToSupport,
                                                                                         "alternate" );
        List<String> typesWithoutLink = findUnsupportedTypes( alternateLinks, mediaTypesToSupport );
        assertTrue( typesWithoutLink.isEmpty(),
                    "Feature Collection Metadata document must include links for alternate encodings. Missing links for types "
                                                + typesWithoutLink );

        // Validate that each link includes a rel and type parameter.
        List<String> linksWithoutRelOrType = findLinksWithoutRelOrType( links );
        assertTrue( linksWithoutRelOrType.isEmpty(),
                    "Links for alternate encodings must include a rel and type parameter. Missing for links "
                                                     + linksWithoutRelOrType );
    }

    /**
     * Abstract Test 22, Test Method 5 (Abstract Test 24)
     *
     * <pre>
     * Abstract Test 22: /ats/core/fc-response
     * Test Purpose: Validate that the Feature Collections complies with the require structure and contents.
     * Requirement: /req/core/fc-response
     *
     * Test Method
     *   5. If the timeStamp property is present, validate that it complies with /ats/core/fc-timeStamp
     * </pre>
     *
     * <pre>
     * Abstract Test 24: /ats/core/fc-timeStamp
     * Test Purpose: Validate the timeStamp parameter returned with a Features response
     * Requirement: /req/core/fc-timeStamp
     *
     * Test Method: Validate that the timeStamp value is set to the time when the response was generated.
     * </pre>
     *
     * @param collection
     *            the collection under test, never <code>null</code>
     */
    public void validateTimeStamp( Map<String, Object> collection ) {
        String collectionId = (String) collection.get( "id" );
        ResponseData response = collectionIdAndResponse.get( collectionId );
        if ( response == null )
            throw new SkipException( "Could not find a response for collection with id " + collectionId );

        JsonPath jsonPath = response.jsonPath();

        assertTimeStamp( collectionId, jsonPath, response.timeStampBeforeResponse, response.timeStampAfterResponse,
                         true );
    }

    /**
     * Abstract Test 22, Test Method 6 (Abstract Test 25)
     *
     * <pre>
     * Abstract Test 22: /ats/core/fc-response
     * Test Purpose: Validate that the Feature Collections complies with the require structure and contents.
     * Requirement: /req/core/fc-response
     *
     * Test Method
     *   6. If the numberMatched property is present, validate that it complies with /ats/core/fc-numberMatched
     * </pre>
     *
     * <pre>
     * Abstract Test 25: /ats/core/fc-numberMatched
     * Test Purpose: Validate the numberMatched parameter returned with a Features response
     * Requirement: /req/core/fc-numberMatched
     *
     * Test Method: Validate that the value of the numberMatched parameter is identical to the number of features in the feature collections that match the selection parameters like bbox, datetime or additional filter parameters.
     * </pre>
     *
     * @param collection
     *            the collection under test, never <code>null</code>
     *
     * @throws URISyntaxException
     *             if the creation of a uri fails
     */
    void validateNumberMatched( Map<String, Object> collection )
                            throws URISyntaxException {
        String collectionId = (String) collection.get( "id" );
        ResponseData response = collectionIdAndResponse.get( collectionId );
        if ( response == null )
            throw new SkipException( "Could not find a response for collection with id " + collectionId );

        JsonPath jsonPath = response.jsonPath();

        assertNumberMatched( getApiModel(), iut, collectionId, jsonPath, true );
    }

    /**
     * Abstract Test 22, Test Method 7 (Abstract Test 26)
     *
     * <pre>
     * Abstract Test 22: /ats/core/fc-response
     * Test Purpose: Validate that the Feature Collections complies with the require structure and contents.
     * Requirement: /req/core/fc-response
     *
     * Test Method
     *   7. If the numberReturned property is present, validate that it complies with /ats/core/fc-numberReturned
     * </pre>
     *
     * <pre>
     * Abstract Test 26: /ats/core/fc-numberReturned
     * Test Purpose: Validate the numberReturned parameter returned with a Features response
     * Requirement: /req/core/fc-numberReturned
     *
     * Test Method: Validate that the numberReturned value is identical to the number of features in the response.
     * </pre>
     *
     * @param collection
     *            the collection under test, never <code>null</code>
     */
    void validateNumberReturned( Map<String, Object> collection ) {
        String collectionId = (String) collection.get( "id" );
        ResponseData response = collectionIdAndResponse.get( collectionId );
        if ( response == null )
            throw new SkipException( "Could not find a response for collection with id " + collectionId );

        JsonPath jsonPath = response.jsonPath();

        assertNumberReturned( collectionId, jsonPath, true );
    }

    protected String findFeaturesUrlForGeoJson( Map<String, Object> collection ) {
        List<Object> links = (List<Object>) collection.get( "links" );
        for ( Object linkObject : links ) {
            Map<String, Object> link = (Map<String, Object>) linkObject;
            Object rel = link.get( "rel" );
            Object type = link.get( "type" );
            if ( "items".equals( rel ) && GEOJSON_MIME_TYPE.equals( type ) )
                return (String) link.get( "href" );
        }
        return null;
    }

    protected boolean isRequired( Parameter param ) {
        return param.getRequired() != null && param.getRequired();
    }

    protected Boolean isExplode( Parameter param ) {
        return param.getExplode() != null && param.getExplode();
    }

    protected class ResponseData {

        private final Response response;

        protected final ZonedDateTime timeStampBeforeResponse;

        protected final ZonedDateTime timeStampAfterResponse;

        public ResponseData( Response response, ZonedDateTime timeStampBeforeResponse,
                             ZonedDateTime timeStampAfterResponse ) {
            this.response = response;
            this.timeStampBeforeResponse = timeStampBeforeResponse;
            this.timeStampAfterResponse = timeStampAfterResponse;
        }

        public JsonPath jsonPath() {
            return response.jsonPath();
        }
    }
}
