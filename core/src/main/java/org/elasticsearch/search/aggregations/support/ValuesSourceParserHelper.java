/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.search.aggregations.support;

import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.ParsingException;
import org.elasticsearch.common.xcontent.ObjectParser;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.query.QueryParseContext;
import org.elasticsearch.script.Script;
import org.joda.time.DateTimeZone;

public final class ValuesSourceParserHelper {
    static final ParseField TIME_ZONE = new ParseField("time_zone");

    private ValuesSourceParserHelper() {} // utility class, no instantiation

    public static void declareAnyFields(
            ObjectParser<? extends ValuesSourceAggregationBuilder<ValuesSource, ?>, QueryParseContext> objectParser,
            boolean scriptable, boolean formattable) {
        declareFields(objectParser, scriptable, formattable, false, ValuesSourceType.ANY, null);
    }

    public static void declareNumericFields(
            ObjectParser<? extends ValuesSourceAggregationBuilder<ValuesSource.Numeric, ?>, QueryParseContext> objectParser,
            boolean scriptable, boolean formattable, boolean timezoneAware) {
        declareFields(objectParser, scriptable, formattable, timezoneAware, ValuesSourceType.NUMERIC, ValueType.NUMERIC);
    }

    public static void declareBytesFields(
            ObjectParser<? extends ValuesSourceAggregationBuilder<ValuesSource.Bytes, ?>, QueryParseContext> objectParser,
            boolean scriptable, boolean formattable) {
        declareFields(objectParser, scriptable, formattable, false, ValuesSourceType.BYTES, ValueType.STRING);
    }

    public static void declareGeoFields(
            ObjectParser<? extends ValuesSourceAggregationBuilder<ValuesSource.GeoPoint, ?>, QueryParseContext> objectParser,
            boolean scriptable, boolean formattable) {
        declareFields(objectParser, scriptable, formattable, false, ValuesSourceType.GEOPOINT, ValueType.GEOPOINT);
    }

    private static <VS extends ValuesSource> void declareFields(
            ObjectParser<? extends ValuesSourceAggregationBuilder<VS, ?>, QueryParseContext> objectParser,
            boolean scriptable, boolean formattable, boolean timezoneAware, ValuesSourceType valuesSourceType, ValueType targetValueType) {


        objectParser.declareField(ValuesSourceAggregationBuilder::field, XContentParser::text,
                new ParseField("field"), ObjectParser.ValueType.STRING);

        objectParser.declareField(ValuesSourceAggregationBuilder::missing, XContentParser::objectText,
                new ParseField("missing"), ObjectParser.ValueType.VALUE);

        objectParser.declareField(ValuesSourceAggregationBuilder::valueType, p -> {
            ValueType valueType = ValueType.resolveForScript(p.text());
            if (targetValueType != null && valueType.isNotA(targetValueType)) {
                throw new ParsingException(p.getTokenLocation(),
                        "Aggregation [" + objectParser.getName() + "] was configured with an incompatible value type ["
                                + valueType + "]. It can only work on value of type ["
                                + targetValueType + "]");
            }
            return valueType;
        }, new ParseField("value_type", "valueType"), ObjectParser.ValueType.STRING);

        if (formattable) {
            objectParser.declareField(ValuesSourceAggregationBuilder::format, XContentParser::text,
                    new ParseField("format"), ObjectParser.ValueType.STRING);
        }

        if (scriptable) {
            objectParser.declareField(ValuesSourceAggregationBuilder::script, org.elasticsearch.script.Script::parse,
                    Script.SCRIPT_PARSE_FIELD, ObjectParser.ValueType.OBJECT_OR_STRING);
        }

        if (timezoneAware) {
            objectParser.declareField(ValuesSourceAggregationBuilder::timeZone, p -> {
                if (p.currentToken() == XContentParser.Token.VALUE_STRING) {
                    return DateTimeZone.forID(p.text());
                } else {
                    return DateTimeZone.forOffsetHours(p.intValue());
                }
            }, TIME_ZONE, ObjectParser.ValueType.LONG);
        }
    }

}
