/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 *  org.apache.commons.csv.CSVFormat
 *  org.apache.commons.csv.CSVPrinter
 *  org.apache.commons.csv.CSVRecord
 *  org.apache.commons.io.input.CharSequenceReader
 */
package org.thingsboard.server.report.util;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Generated;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.CharSequenceReader;

public class CsvUtils {
    public static List<List<String>> parseCsv(String content, Character delimiter) throws Exception {
        List<CSVRecord> records;
        CSVFormat csvFormat = delimiter.equals(Character.valueOf(',')) ? CSVFormat.DEFAULT : CSVFormat.DEFAULT.withDelimiter(delimiter.charValue());
        try (CharSequenceReader reader = new CharSequenceReader((CharSequence)content);){
            records = csvFormat.parse((Reader)reader).getRecords();
        }
        return records.stream().map(record -> Stream.iterate(0, i -> i < record.size(), i -> i + 1).map(record::get).collect(Collectors.toList())).collect(Collectors.toList());
    }

    public static byte[] generateCsv(List<List<String>> rows) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (OutputStreamWriter writer = new OutputStreamWriter((OutputStream)out, StandardCharsets.UTF_8);
             CSVPrinter csvPrinter = new CSVPrinter((Appendable)writer, CSVFormat.DEFAULT);){
            for (List<String> row : rows) {
                csvPrinter.printRecord(row);
            }
            csvPrinter.flush();
        }
        catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
        return out.toByteArray();
    }

    @Generated
    private CsvUtils() {
    }
}

