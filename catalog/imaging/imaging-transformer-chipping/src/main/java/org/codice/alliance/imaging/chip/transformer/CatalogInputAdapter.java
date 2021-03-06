/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.alliance.imaging.chip.transformer;

import static org.apache.commons.lang3.Validate.notNull;

import java.io.Serializable;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.impl.ResourceRequestById;
import ddf.catalog.operation.impl.ResourceRequestByProductUri;

/**
 * A class to convert a Metacard to a ResourceRequest object.
 */
public class CatalogInputAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogInputAdapter.class);

    /**
     * @param metacard the Metacard containing the relevant information for the ResourceRequest.
     *                 May not be null.
     * @return the ResourceRequest object to query the CatalogFramework service.
     */
    public ResourceRequest buildReadRequest(Metacard metacard, String qualifier) {

        if (metacard == null) {
            throw new IllegalArgumentException("method argument 'metacard' may not be null.");
        }

        if (qualifier == null) {
            throw new IllegalArgumentException("method argument 'qualifier' may not be null.");
        }

        Attribute attribute = metacard.getAttribute(Metacard.DERIVED_RESOURCE_URI);

        if (attribute != null) {
            List<Serializable> values = attribute.getValues();

            if (values == null) {
                throw new IllegalStateException(String.format(
                        "metacard attribute %s has no values assigned.",
                        Metacard.DERIVED_RESOURCE_URI));
            }

            URI qualifiedUri = findDerivedResourceUri(values, qualifier);

            return new ResourceRequestByProductUri(qualifiedUri);
        }

        throw new IllegalStateException(String.format(
                "The supplied metacard does not contain the " + "'%s' attribute.",
                Metacard.DERIVED_RESOURCE_URI));
    }

    /**
     * Construct a resource request for the object represented by the metacard.
     *
     * @param metacard must be non-null
     * @return a resource request than can be passed to the catalog framework
     */
    @SuppressWarnings("unused")
    public ResourceRequest buildReadRequest(Metacard metacard) {
        notNull(metacard, "method argument 'metacard' may not be null.");
        LOGGER.trace("building a framework resource request for id '{}'", metacard.getId());
        return new ResourceRequestById(metacard.getId());
    }

    private URI findDerivedResourceUri(List<Serializable> values, String qualifier) {
        List<URI> qualifiedUri = values.stream()
                .map(String::valueOf)
                .map(URI::create)
                .filter(uri -> uri.getFragment()
                        .equals(qualifier))
                .collect(Collectors.toList());

        if (qualifiedUri.size() < 1) {
            throw new IllegalArgumentException(String.format(
                    "The derived resource URI for qualifier '%s' was not found on the metacard.",
                    qualifier));
        }

        if (qualifiedUri.size() > 1) {
            throw new IllegalStateException(String.format(
                    "Metacard contains multiple derived resource URIs with the same qualifier: '%s'.",
                    qualifier));
        }

        return qualifiedUri.get(0);
    }
}
