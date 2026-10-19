/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */
package org.glassfish.wasp.benchmark;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** A product of the catalog the benchmark page renders. */
public class Product {

    private final String sku;
    private final String name;
    private final BigDecimal price;
    private final int quantity;
    private final List<String> tags;
    private final Map<String, String> attributes;

    public Product(String sku, String name, BigDecimal price, int quantity, List<String> tags, Map<String, String> attributes) {
        this.sku = sku;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.tags = tags;
        this.attributes = attributes;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity;
    }

    public List<String> getTags() {
        return tags;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }
}
