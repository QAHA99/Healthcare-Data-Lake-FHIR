package com.github.cm2027.lab3;

import com.github.cm2027.lab3.util.ConfigurationUtil;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;

/**
 * ClientSingleton is a singleton for the FHIR client.
 * It is connected with the address specified in
 * resources/application.properties on hapi.fhir.base-url
 * And will fallback to the deployed hapi fhir instance on kthcloud.
 * 
 * This singleton is safe to use concurrently.
 */
public class ClientSingleton {

    private static final String DEFAULT_HAPI_FHIR_BASE_URL = "https://hapi-fhir.app.cloud.cbh.kth.se/fhir";
    private static final String HAPI_FHIR_BASE_KEY = "hapi.fhir.base-url";

    private static IGenericClient instance;

    private ClientSingleton() {
    }

    public static synchronized IGenericClient getInstance() {
        if (instance == null) {
            String baseUrl = ConfigurationUtil.getString(
                    HAPI_FHIR_BASE_KEY, DEFAULT_HAPI_FHIR_BASE_URL);
            FhirContext context = FhirContext.forR4();
            instance = context.newRestfulGenericClient(baseUrl);
        }
        return instance;
    }
}
