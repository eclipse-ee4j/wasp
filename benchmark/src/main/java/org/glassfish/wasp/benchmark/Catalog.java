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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * The catalog the benchmark page renders: {@value #CATEGORIES} categories of {@value #PRODUCTS_PER_CATEGORY} products,
 * always the same, so that the benchmark can check the page.
 */
public class Catalog {

    public static final int CATEGORIES = 5;
    public static final int PRODUCTS_PER_CATEGORY = 20;

    private static final String[] TAGS = { "new", "sale", "eco", "premium", "bestseller", "limited", "imported" };
    private static final String[] COLORS = { "red", "green", "blue", "black", "white" };
    private static final String[] SIZES = { "S", "M", "L", "XL" };

    private final List<Category> categories = new ArrayList<>();
    private final List<Product> products = new ArrayList<>();
    private final TreeSet<String> tags = new TreeSet<>();
    private BigDecimal totalValue = BigDecimal.ZERO;

    public Catalog() {
        for (int c = 0; c < CATEGORIES; c++) {
            List<Product> categoryProducts = new ArrayList<>();
            for (int p = 0; p < PRODUCTS_PER_CATEGORY; p++) {
                int n = c * PRODUCTS_PER_CATEGORY + p;

                List<String> productTags = new ArrayList<>();
                for (int t = 0; t < n % 4; t++) {
                    productTags.add(TAGS[(n + t) % TAGS.length]);
                }
                Map<String, String> attributes = new LinkedHashMap<>();
                attributes.put("color", COLORS[n % COLORS.length]);
                attributes.put("size", SIZES[n % SIZES.length]);

                Product product = new Product(
                        String.format("SKU-%05d", n),
                        "product " + n + " of category " + c,
                        BigDecimal.valueOf(1000 + n * 37L, 2),
                        n % 11,
                        productTags,
                        attributes);

                categoryProducts.add(product);
                products.add(product);
                tags.addAll(productTags);
                totalValue = totalValue.add(product.getPrice().multiply(BigDecimal.valueOf(product.getQuantity())));
            }
            categories.add(new Category("Category " + c, categoryProducts));
        }
    }

    public List<Category> getCategories() {
        return categories;
    }

    public List<Product> getProducts() {
        return products;
    }

    /** @return all tags of the products, sorted */
    public TreeSet<String> getTags() {
        return tags;
    }

    /** @return the sum of price times quantity of all products */
    public BigDecimal getTotalValue() {
        return totalValue;
    }
}
