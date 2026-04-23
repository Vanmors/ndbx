package com.vanmors.ndbx.converter;

import com.vanmors.ndbx.entity.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class SafeCategoryReadConverter implements Converter<String, Category> {

    private static final Logger log = LoggerFactory.getLogger(SafeCategoryReadConverter.class);

    @Override
    public Category convert(final String source) {
        if (source == null || source.isBlank()) {
            return null;
        }

        try {
            return Category.valueOf(source.toLowerCase());
        } catch (final IllegalArgumentException ex) {
            log.info("Unknown category value from DB: {}", source);
            return null;
        }
    }
}