/*
 * Licensed to the Ted Dunning under one or more contributor license
 * agreements.  See the NOTICE file that may be
 * distributed with this work for additional information
 * regarding copyright ownership.  Ted Dunning licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.mapr.synth.samplers;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.util.List;
import java.util.Random;

/**
 * Create comma separated samples from another field definition or array of field definitions.
 */
public class SequenceSampler extends FieldSampler {
    private JsonNodeFactory nodeFactory = JsonNodeFactory.withExactBigDecimals(false);
    private FieldSampler base = null;
    private Random gen = new Random();
    private List<FieldSampler> array = null;
    private FieldSampler length = exponential(5);

    @JsonCreator
    public SequenceSampler() {
    }

    public void setLength(double length) {
        this.length = exponential(length);
    }

    private FieldSampler exponential(final double length) {
        return new FieldSampler() {
            @Override
            public JsonNode sample() {
                int n = (int) Math.floor(-length * Math.log(gen.nextDouble()));
                return new IntNode(n);
            }
        };
    }

    @SuppressWarnings("unused")
    public void setLengthDistribution(final JsonNode value) throws IOException {
        if (value.isObject()) {
            length = FieldSampler.newSampler(value.toString());
        } else {
            length = new FieldSampler() {
                @Override
                public JsonNode sample() {
                    return value;
                }
            };
        }
    }

    public void setBase(FieldSampler base) {
        this.base = base;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setArray(List<FieldSampler> base) {
        this.array = Lists.newArrayList(base);
    }

    @Override
    public JsonNode sample() {
        Preconditions.checkState(array != null || base != null, "Need to specify either base or array");
        ArrayNode r = nodeFactory.arrayNode();
        if (base != null) {
            int n = (int) length.sample().asDouble();
            for (int i = 0; i < n; i++) {
                r.add(base.sample());
            }
        } else {
            for (FieldSampler sampler : array) {
                r.add(sampler.sample());
            }
        }
        return r;
    }
}
