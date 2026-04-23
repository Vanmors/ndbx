package com.vanmors.ndbx.converter;

import com.vanmors.ndbx.entity.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;


@WritingConverter
public class SafeCategoryWriteConverter implements Converter<Category, String> {

    private static final Logger log = LoggerFactory.getLogger(SafeCategoryWriteConverter.class);

    @Override
    public String convert(final Category source) {

        if (source == null) {
            return null;
        }
        log.info("try write={}", source);
        return source.name().toLowerCase();
    }
}