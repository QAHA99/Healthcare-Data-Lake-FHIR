package com.github.cm2027.lab3.util;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.util.BundleUtil;

public class StreamUtil<T extends IBaseResource> {

    protected final IGenericClient client;
    protected final Class<T> resourceType;

    public StreamUtil(IGenericClient client, Class<T> resourceType) {
        this.client = client;
        this.resourceType = resourceType;
    }

    public Stream<T> streamAll(Function<IGenericClient, Bundle> fetch) {
        Iterator<T> iterator = new Iterator<>() {
            private Bundle currentBundle = null;
            private Iterator<T> currentIterator = Collections.emptyIterator();

            private void loadNextBundleIfNeeded() {
                try {
                    while (!currentIterator.hasNext()) {
                        if (currentBundle == null) {
                            // Initial load
                            currentBundle = fetch.apply(client);
                        } else if (currentBundle.getLink(Bundle.LINK_NEXT) != null) {
                            // Load next page
                            currentBundle = client.loadPage().next(currentBundle).execute();
                        } else {
                            // No more pages
                            currentIterator = Collections.emptyIterator();
                            return;
                        }

                        currentIterator = BundleUtil
                                .toListOfResourcesOfType(client.getFhirContext(), currentBundle, resourceType)
                                .iterator();
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Error fetching resources", e);
                }
            }

            @Override
            public boolean hasNext() {
                loadNextBundleIfNeeded();
                return currentIterator.hasNext();
            }

            @Override
            public T next() {
                loadNextBundleIfNeeded();
                if (!currentIterator.hasNext()) {
                    throw new NoSuchElementException();
                }
                return currentIterator.next();
            }
        };

        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
    }

}
